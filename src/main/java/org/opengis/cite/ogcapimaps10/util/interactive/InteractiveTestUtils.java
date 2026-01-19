package org.opengis.cite.ogcapimaps10.util.interactive;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility methods for interactive CTL tests. These methods are called from CTL scripts to
 * build tile request URLs with various parameters for manual verification.
 */
public final class InteractiveTestUtils {

	private static final String REL_TILESETS_MAP = "https://www.opengis.net/def/rel/ogc/1.0/tilesets-map";

	private static final String REL_SELF = "self";

	private static final String REL_ITEM = "item";

	/**
	 * Prefix added to URLs when no valid tile URL could be found. This allows CTL scripts
	 * to detect and display an appropriate warning message.
	 */
	public static final String NOT_FOUND_PREFIX = "NOT_FOUND:";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private InteractiveTestUtils() {
		// Utility class, no instantiation
	}

	/**
	 * Builds a tile request URL with the bgcolor parameter.
	 * @param tilesUrl The tiles endpoint URL (landing page or tilesets URL).
	 * @param bgcolor The background color value (e.g., "0x00FF00", "red").
	 * @param tileMatrixSet The TileMatrixSet identifier (e.g., "WebMercatorQuad").
	 * @return The tile request URL with bgcolor parameter.
	 */
	public static String buildTileRequestWithBgcolor(String tilesUrl, String bgcolor, String tileMatrixSet) {
		String tileUrl = retrieveFirstTileUrl(tilesUrl, tileMatrixSet);
		if (tileUrl == null) {
			return NOT_FOUND_PREFIX + tilesUrl + "?bgcolor=" + encodeParam(bgcolor);
		}
		return appendParam(tileUrl, "bgcolor", bgcolor);
	}

	/**
	 * Builds a tile request URL with the transparent parameter.
	 * @param tilesUrl The tiles endpoint URL.
	 * @param transparent The transparent value ("true" or "false").
	 * @param tileMatrixSet The TileMatrixSet identifier (e.g., "WebMercatorQuad").
	 * @return The tile request URL with transparent parameter.
	 */
	public static String buildTileRequestWithTransparent(String tilesUrl, String transparent, String tileMatrixSet) {
		String tileUrl = retrieveFirstTileUrl(tilesUrl, tileMatrixSet);
		if (tileUrl == null) {
			return NOT_FOUND_PREFIX + tilesUrl + "?transparent=" + encodeParam(transparent);
		}
		return appendParam(tileUrl, "transparent", transparent);
	}

	/**
	 * Builds a tile request URL with the mm-per-pixel (display resolution) parameter.
	 * @param tilesUrl The tiles endpoint URL.
	 * @param mmPerPixel The display resolution value.
	 * @param tileMatrixSet The TileMatrixSet identifier (e.g., "WebMercatorQuad").
	 * @return The tile request URL with mm-per-pixel parameter.
	 */
	public static String buildTileRequestWithResolution(String tilesUrl, String mmPerPixel, String tileMatrixSet) {
		String tileUrl = retrieveFirstTileUrl(tilesUrl, tileMatrixSet);
		if (tileUrl == null) {
			return NOT_FOUND_PREFIX + tilesUrl + "?mm-per-pixel=" + encodeParam(mmPerPixel);
		}
		return appendParam(tileUrl, "mm-per-pixel", mmPerPixel);
	}

	/**
	 * Builds a tile request URL with the subset parameter.
	 * @param tilesUrl The tiles endpoint URL.
	 * @param subset The subset parameter value (e.g., "Lat(40:50),Lon(-75:-70)").
	 * @param tileMatrixSet The TileMatrixSet identifier (e.g., "WebMercatorQuad").
	 * @return The tile request URL with subset parameter.
	 */
	public static String buildTileRequestWithSubset(String tilesUrl, String subset, String tileMatrixSet) {
		String tileUrl = retrieveFirstTileUrl(tilesUrl, tileMatrixSet);
		if (tileUrl == null) {
			return NOT_FOUND_PREFIX + tilesUrl + "?subset=" + encodeParam(subset);
		}
		return appendParam(tileUrl, "subset", subset);
	}

	/**
	 * Retrieves the first available tile URL from a landing page or tilesets endpoint.
	 * @param landingPageUrl The landing page or tilesets URL.
	 * @param tileMatrixSet The TileMatrixSet identifier (e.g., "WebMercatorQuad").
	 * @return The first tile URL, or null if not found.
	 */
	public static String retrieveFirstTileUrl(String landingPageUrl, String tileMatrixSet) {
		try {
			String tilesetsUrl = findTilesetsMapUrl(landingPageUrl);
			if (tilesetsUrl == null) {
				tilesetsUrl = landingPageUrl;
			}

			String tilesetUrl = findFirstTilesetUrl(tilesetsUrl, tileMatrixSet);
			if (tilesetUrl == null) {
				return null;
			}

			return buildDefaultTileUrl(tilesetUrl, tileMatrixSet);
		}
		catch (Exception e) {
			return null;
		}
	}

	/**
	 * Finds the tilesets-map URL from a landing page.
	 * @param landingPageUrl The landing page URL.
	 * @return The tilesets-map URL, or null if not found.
	 */
	@SuppressWarnings("unchecked")
	private static String findTilesetsMapUrl(String landingPageUrl) {
		try {
			Map<String, Object> data = fetchJson(landingPageUrl);
			if (data == null) {
				return null;
			}

			// Check landing page links
			List<Map<String, Object>> links = (List<Map<String, Object>>) data.get("links");
			String tilesetsUrl = findTilesetsMapLink(links);
			if (tilesetsUrl != null) {
				return resolveUrl(landingPageUrl, tilesetsUrl);
			}

			// Check collections
			List<Map<String, Object>> collections = (List<Map<String, Object>>) data.get("collections");
			if (collections != null) {
				for (Map<String, Object> collection : collections) {
					List<Map<String, Object>> collectionLinks = (List<Map<String, Object>>) collection.get("links");
					tilesetsUrl = findTilesetsMapLink(collectionLinks);
					if (tilesetsUrl != null) {
						return resolveUrl(landingPageUrl, tilesetsUrl);
					}
				}
			}
		}
		catch (Exception e) {
			// Failed to find tilesets-map URL
		}
		return null;
	}

	/**
	 * Finds tilesets-map link from a list of links.
	 * @param links The list of links.
	 * @return The href of the tilesets-map link, or null if not found.
	 */
	private static String findTilesetsMapLink(List<Map<String, Object>> links) {
		if (links == null) {
			return null;
		}
		for (Map<String, Object> link : links) {
			String rel = (String) link.get("rel");
			if (REL_TILESETS_MAP.equals(rel)) {
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
	private static String resolveUrl(String baseUrl, String url) {
		if (url == null) {
			return null;
		}
		if (url.startsWith("http://") || url.startsWith("https://")) {
			return url;
		}
		try {
			URI base = URI.create(baseUrl);
			return base.resolve(url).toString();
		}
		catch (Exception e) {
			return url;
		}
	}

	/**
	 * Finds the tileset URL for the specified TileMatrixSet from a tilesets list.
	 * @param tilesetsUrl The tilesets list URL.
	 * @param tileMatrixSet The TileMatrixSet identifier to match.
	 * @return The tileset URL, or null if not found.
	 */
	@SuppressWarnings("unchecked")
	private static String findFirstTilesetUrl(String tilesetsUrl, String tileMatrixSet) {
		try {
			Map<String, Object> data = fetchJson(tilesetsUrl);
			if (data == null) {
				return null;
			}

			List<Map<String, Object>> tilesets = (List<Map<String, Object>>) data.get("tilesets");
			if (tilesets != null && !tilesets.isEmpty()) {
				// Try to find tileset matching the requested TileMatrixSet
				for (Map<String, Object> tileset : tilesets) {
					String tileMatrixSetURI = (String) tileset.get("tileMatrixSetURI");
					if (tileMatrixSetURI != null && tileMatrixSetURI.contains(tileMatrixSet)) {
						List<Map<String, Object>> links = (List<Map<String, Object>>) tileset.get("links");
						String selfUrl = findLinkByRel(links, REL_SELF);
						if (selfUrl != null) {
							return resolveUrl(tilesetsUrl, selfUrl);
						}
					}
				}
				// Fallback to first tileset
				Map<String, Object> firstTileset = tilesets.get(0);
				List<Map<String, Object>> links = (List<Map<String, Object>>) firstTileset.get("links");
				String selfUrl = findLinkByRel(links, REL_SELF);
				if (selfUrl != null) {
					return resolveUrl(tilesetsUrl, selfUrl);
				}
			}
		}
		catch (Exception e) {
			// Failed to find tileset URL
		}
		return null;
	}

	/**
	 * Builds a default tile URL from a tileset URL by finding the "item" link template.
	 * @param tilesetUrl The tileset URL.
	 * @param tileMatrixSet The TileMatrixSet identifier (e.g., "WebMercatorQuad").
	 * @return A default tile URL (zoom 0, row 0, col 0).
	 */
	@SuppressWarnings("unchecked")
	private static String buildDefaultTileUrl(String tilesetUrl, String tileMatrixSet) {
		String matrixSet = (tileMatrixSet != null && !tileMatrixSet.isEmpty()) ? tileMatrixSet : "WebMercatorQuad";

		// Try to get tile URL template from the tileset
		try {
			Map<String, Object> data = fetchJson(tilesetUrl);
			if (data != null) {
				List<Map<String, Object>> links = (List<Map<String, Object>>) data.get("links");
				if (links != null) {
					// Find "item" link with image type
					for (Map<String, Object> link : links) {
						String rel = (String) link.get("rel");
						String type = (String) link.get("type");
						if (REL_ITEM.equals(rel) && type != null && type.startsWith("image/")) {
							String href = (String) link.get("href");
							String template = resolveUrl(tilesetUrl, href);
							// Replace template variables with default values
							return template.replace("{tileMatrixSetId}", matrixSet)
								.replace("{tileMatrix}", "0")
								.replace("{tileRow}", "0")
								.replace("{tileCol}", "0");
						}
					}
				}
			}
		}
		catch (Exception e) {
			// Failed to get tile template, use fallback
		}

		// Fallback: construct URL with .png extension
		return tilesetUrl + "/" + matrixSet + "/0/0/0.png";
	}

	/**
	 * Finds a link by its rel attribute.
	 * @param links The list of links.
	 * @param rel The rel value to search for.
	 * @return The href of the matching link, or null if not found.
	 */
	private static String findLinkByRel(List<Map<String, Object>> links, String rel) {
		if (links == null) {
			return null;
		}
		for (Map<String, Object> link : links) {
			if (rel.equals(link.get("rel"))) {
				return (String) link.get("href");
			}
		}
		return null;
	}

	/**
	 * Fetches JSON from a URL.
	 * @param urlString The URL to fetch.
	 * @return The parsed JSON as a Map, or null if failed.
	 */
	private static Map<String, Object> fetchJson(String urlString) {
		try {
			URL url = URI.create(urlString).toURL();
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Accept", "application/json");
			connection.setConnectTimeout(10000);
			connection.setReadTimeout(10000);

			if (connection.getResponseCode() == 200) {
				try (InputStream is = connection.getInputStream()) {
					return OBJECT_MAPPER.readValue(is, new TypeReference<Map<String, Object>>() {
					});
				}
			}
		}
		catch (Exception e) {
			// Failed to fetch JSON
		}
		return null;
	}

	/**
	 * Appends a parameter to a URL.
	 * @param url The base URL.
	 * @param param The parameter name.
	 * @param value The parameter value.
	 * @return The URL with the appended parameter.
	 */
	private static String appendParam(String url, String param, String value) {
		String separator = url.contains("?") ? "&" : "?";
		return url + separator + param + "=" + encodeParam(value);
	}

	/**
	 * URL-encodes a parameter value.
	 * @param value The value to encode.
	 * @return The encoded value.
	 */
	private static String encodeParam(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

}
