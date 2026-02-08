package org.opengis.cite.ogcapimaps10.conformance.mapTilesets;

import java.util.List;
import java.util.Map;

import org.opengis.cite.ogcapimaps10.conformance.CommonFixture;
import org.opengis.cite.ogcapimaps10.conformance.SuiteAttribute;
import org.opengis.cite.ogcapimaps10.domain.InteractiveTestResult;
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;

import io.restassured.response.Response;

/**
 * Base fixture for tiles-parameters tests. Provides common functionality for both
 * automated and interactive tests related to tile parameters.
 */
public class TilesParametersFixture extends CommonFixture {

	private static final String REL_TILESETS_MAP = "https://www.opengis.net/def/rel/ogc/1.0/tilesets-map";

	private static final String REL_SELF = "self";

	private static final String REL_ITEM = "item";

	protected String tilesetsMapUrl;

	protected ITestContext testContext;

	/**
	 * The selected TileMatrixSet identifier (e.g., "WebMercatorQuad" or
	 * "WorldCRS84Quad").
	 */
	protected String tileMatrixSet;

	/**
	 * Initializes the tiles parameters fixture by finding the tilesets-map endpoint.
	 * @param testContext The test context containing suite attributes.
	 */
	@BeforeClass
	public void initTilesParametersFixture(ITestContext testContext) {
		this.testContext = testContext;
		this.tilesetsMapUrl = findTilesetsMapUrl();
		this.tileMatrixSet = getTileMatrixSetFromContext(testContext);
	}

	/**
	 * Retrieves the TileMatrixSet from the test context.
	 * @param context The test context.
	 * @return The TileMatrixSet identifier, defaults to "WebMercatorQuad" if not set.
	 */
	private String getTileMatrixSetFromContext(ITestContext context) {
		if (context != null) {
			Object attribute = context.getSuite().getAttribute(SuiteAttribute.TILE_MATRIX_SET.getName());
			if (attribute != null) {
				return (String) attribute;
			}
		}
		return "WebMercatorQuad";
	}

	/**
	 * Retrieves the InteractiveTestResult from the test context.
	 * @param context The test context.
	 * @return The InteractiveTestResult containing interactive test results.
	 * @throws SkipException if the context or result is null.
	 */
	protected InteractiveTestResult getInteractiveTestResult(ITestContext context) {
		if (context == null) {
			throw new SkipException("Test context is null!");
		}
		Object attribute = context.getSuite().getAttribute(SuiteAttribute.INTERACTIVE_TEST_RESULT.getName());
		if (attribute == null) {
			throw new SkipException("Interactive test result is missing!");
		}
		return (InteractiveTestResult) attribute;
	}

	/**
	 * Finds the tilesets-map URL from the landing page or collections.
	 * @return The tilesets-map URL, or null if not found.
	 */
	private String findTilesetsMapUrl() {
		String baseUrl = rootUri.toString();
		if (baseUrl.endsWith("/")) {
			baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
		}

		String tilesetsUrl = findTilesetsMapFromLandingPage(baseUrl);
		if (tilesetsUrl != null) {
			return tilesetsUrl;
		}

		return findTilesetsMapFromCollections(baseUrl + "/collections");
	}

	/**
	 * Finds tilesets-map URL from the landing page.
	 * @param landingPageUrl The landing page URL.
	 * @return The tilesets-map URL, or null if not found.
	 */
	private String findTilesetsMapFromLandingPage(String landingPageUrl) {
		try {
			Response response = init().accept("application/json").when().get(landingPageUrl);
			if (response.getStatusCode() == 200) {
				List<Map<String, Object>> links = response.jsonPath().getList("links");
				String tilesetsUrl = findTilesetsMapLink(links);
				if (tilesetsUrl != null) {
					return resolveUrl(landingPageUrl, tilesetsUrl);
				}
			}
		}
		catch (Exception e) {
			// Landing page check failed, continue to collections
		}
		return null;
	}

	/**
	 * Finds tilesets-map link from a list of links.
	 * @param links The list of links.
	 * @return The href of the tilesets-map link, or null if not found.
	 */
	private String findTilesetsMapLink(List<Map<String, Object>> links) {
		if (links == null) {
			return null;
		}
		for (Map<String, Object> link : links) {
			String rel = (String) link.get("rel");
			if (rel != null && matchesRelIgnoringScheme(rel, REL_TILESETS_MAP)) {
				return (String) link.get("href");
			}
		}
		return null;
	}

	/**
	 * Resolves a potentially relative URL against a base URL.
	 * @param baseUrl The base URL.
	 * @param url The URL to resolve (may be relative or absolute).
	 * @return The resolved absolute URL.
	 */
	private String resolveUrl(String baseUrl, String url) {
		if (url == null) {
			return null;
		}
		if (url.startsWith("http://") || url.startsWith("https://")) {
			return url;
		}
		try {
			java.net.URI base = java.net.URI.create(baseUrl);
			return base.resolve(url).toString();
		}
		catch (Exception e) {
			return url;
		}
	}

	/**
	 * Finds tilesets-map URL from collections.
	 * @param collectionsUrl The collections URL.
	 * @return The tilesets-map URL, or null if not found.
	 */
	private String findTilesetsMapFromCollections(String collectionsUrl) {
		try {
			Response response = init().accept("application/json").when().get(collectionsUrl);
			if (response.getStatusCode() == 200) {
				List<Map<String, Object>> collections = response.jsonPath().getList("collections");
				if (collections != null) {
					for (Map<String, Object> collection : collections) {
						Object linksObj = collection.get("links");
						List<Map<String, Object>> links = toMapList(linksObj);
						String tilesetsUrl = findTilesetsMapLink(links);
						if (tilesetsUrl != null) {
							return resolveUrl(collectionsUrl, tilesetsUrl);
						}
					}
				}
			}
		}
		catch (Exception e) {
			// Collections check failed
		}
		return null;
	}

	/**
	 * Finds a link by its rel attribute.
	 * @param links The list of links.
	 * @param rel The rel value to search for.
	 * @return The href of the matching link, or null if not found.
	 */
	protected String findLinkByRel(List<Map<String, Object>> links, String rel) {
		if (links == null) {
			return null;
		}
		for (Map<String, Object> link : links) {
			String linkRel = (String) link.get("rel");
			if (linkRel != null && matchesRelIgnoringScheme(linkRel, rel)) {
				return (String) link.get("href");
			}
		}
		return null;
	}

	/**
	 * Gets the tileset URL for the selected TileMatrixSet from the tilesets-map endpoint.
	 * @return The tileset URL, or null if not available.
	 */
	protected String getFirstTilesetUrl() {
		if (tilesetsMapUrl == null) {
			return null;
		}
		try {
			Response response = init().accept("application/json").when().get(tilesetsMapUrl);
			if (response.getStatusCode() == 200) {
				List<Map<String, Object>> tilesets = response.jsonPath().getList("tilesets");
				if (tilesets != null && !tilesets.isEmpty()) {
					// Try to find tileset matching our TileMatrixSet
					for (Map<String, Object> tileset : tilesets) {
						String tileMatrixSetURI = (String) tileset.get("tileMatrixSetURI");
						if (tileMatrixSetURI != null && tileMatrixSetURI.contains(tileMatrixSet)) {
							Object linksObj = tileset.get("links");
							List<Map<String, Object>> links = toMapList(linksObj);
							String selfUrl = findLinkByRel(links, REL_SELF);
							if (selfUrl != null) {
								return resolveUrl(tilesetsMapUrl, selfUrl);
							}
						}
					}
					Map<String, Object> firstTileset = tilesets.get(0);
					Object linksObj = firstTileset.get("links");
					List<Map<String, Object>> links = toMapList(linksObj);
					String selfUrl = findLinkByRel(links, REL_SELF);
					if (selfUrl != null) {
						return resolveUrl(tilesetsMapUrl, selfUrl);
					}
				}
			}
		}
		catch (Exception e) {
			// Failed to get tileset
		}
		return null;
	}

	/**
	 * Gets a tile URL template from a tileset, finding the "item" link with image type.
	 * @param tilesetUrl The tileset URL.
	 * @return The tile URL template, or null if not found.
	 */
	protected String getTileUrlTemplate(String tilesetUrl) {
		if (tilesetUrl == null) {
			return null;
		}
		try {
			Response response = init().accept("application/json").when().get(tilesetUrl);
			if (response.getStatusCode() == 200) {
				List<Map<String, Object>> links = response.jsonPath().getList("links");
				if (links != null) {
					for (Map<String, Object> link : links) {
						String rel = (String) link.get("rel");
						String type = (String) link.get("type");
						if (rel != null && matchesRelIgnoringScheme(rel, REL_ITEM) && type != null
								&& type.startsWith("image/")) {
							String href = (String) link.get("href");
							return resolveUrl(tilesetUrl, href);
						}
					}
				}
			}
		}
		catch (Exception e) {
			// Failed to get tile URL template
		}
		return null;
	}

	/**
	 * Builds a tile request URL with the specified parameters.
	 * @param tilesetUrl The tileset URL.
	 * @param tileMatrix The tile matrix identifier.
	 * @param tileRow The tile row.
	 * @param tileCol The tile column.
	 * @return The tile request URL.
	 */
	protected String buildTileRequestUrl(String tilesetUrl, String tileMatrix, int tileRow, int tileCol) {
		// Assuming the tileset URL pattern follows OGC API - Tiles
		// {tilesetUrl}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}
		return String.format("%s/tiles/%s/%s/%d/%d", tilesetUrl, tileMatrixSet, tileMatrix, tileRow, tileCol);
	}

	/**
	 * Gets the selected TileMatrixSet identifier.
	 * @return The TileMatrixSet identifier.
	 */
	protected String getTileMatrixSet() {
		return tileMatrixSet;
	}

	private static boolean matchesRelIgnoringScheme(String actual, String expected) {
		return normalizeScheme(actual).equals(normalizeScheme(expected));
	}

	private static String normalizeScheme(String rel) {
		if (rel.startsWith("https://")) {
			return "http://" + rel.substring("https://".length());
		}
		return rel;
	}

	/**
	 * Safely converts an Object to List of Map with type checking.
	 * @param obj The object to convert.
	 * @return The list of maps, or null if conversion fails.
	 */
	private List<Map<String, Object>> toMapList(Object obj) {
		if (!(obj instanceof List)) {
			return null;
		}
		List<?> list = (List<?>) obj;
		List<Map<String, Object>> result = new java.util.ArrayList<>();
		for (Object item : list) {
			if (item instanceof Map) {
				Map<?, ?> map = (Map<?, ?>) item;
				Map<String, Object> typedMap = new java.util.HashMap<>();
				for (Map.Entry<?, ?> entry : map.entrySet()) {
					if (entry.getKey() instanceof String) {
						typedMap.put((String) entry.getKey(), entry.getValue());
					}
				}
				result.add(typedMap);
			}
		}
		return result;
	}

}
