package org.opengis.cite.ogcapimaps10.conformance.mapTilesets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.opengis.cite.ogcapimaps10.conformance.CommonFixture;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * A.2.1. Abstract Test for Requirement desc-links
 */
public class DescLinks extends CommonFixture {

	private static final String REL_TILESETS_MAP = "https://www.opengis.net/def/rel/ogc/1.0/tilesets-map";

	/**
	 * <pre>
	 * Abstract test A.2
	 *
	 * Identifier: /conf/tilesets/desc-links
	 * Requirement: Requirement 4: /req/tilesets/desc-links
	 * Test purpose: Verify that the implementation supports map tilesets
	 *
	 * Test method:
	 * Given: a geospatial data resource conforming to this Standard, to “Map Tilesets”, to OGC API-Tiles and providing a description resource including links
	 * When: retrieving the geospatial data resource description
	 * Then:
	 * - assert that the geospatial data resource (e.g., collection or landing page description’s links property) includes a link with the href pointing to a tileset list supported that presents a tile aspect of this geospatial data resource and with rel: [ogc-rel:tilesets-map]
	 * </pre>
	 */
	@Test(description = "Implements A.2.1. Abstract Test for Requirement desc-links (Requirement /req/tilesets/desc-links)")
	public void verifyTilesetsLink() throws Exception {
		boolean foundTilesetLink = false;
		StringBuilder errorMessages = new StringBuilder();

		String baseUrl = rootUri.toString();
		if (baseUrl.endsWith("/")) {
			baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
		}

		boolean isCollectionsEndpoint = baseUrl.endsWith("/collections");

		if (isCollectionsEndpoint) {
			foundTilesetLink = checkCollections(baseUrl, errorMessages);
		}
		else {
			foundTilesetLink = checkLandingPage(baseUrl, errorMessages);

			if (!foundTilesetLink) {
				String collectionsUrl = baseUrl + "/collections";
				foundTilesetLink = checkCollections(collectionsUrl, errorMessages);
			}
		}

		if (errorMessages.length() > 0) {
			Assert.fail(errorMessages.toString());
		}

		Assert.assertTrue(foundTilesetLink, "No link with relation '" + REL_TILESETS_MAP
				+ "' was found in the Landing Page or any Collection. "
				+ "The implementation must provide a link to the Map Tilesets if it claims to support the Map Tilesets conformance class.");
	}

	private boolean checkLandingPage(String landingPageUrl, StringBuilder errorMessages) {
		try {
			Map<String, Object> lpData = fetchResource(landingPageUrl);

			if (lpData != null) {
				List<Map<String, Object>> lpLinks = getLinks(lpData);
				if (lpLinks != null) {
					Map<String, Object> link = findLinkByRel(lpLinks, REL_TILESETS_MAP);
					if (link != null) {
						validateHref(link, "Landing Page", errorMessages);
						return true;
					}
				}
			}
		}
		catch (Exception e) {
			System.out.println("Info: Landing Page check skipped or failed: " + e.getMessage());
		}
		return false;
	}

	private boolean checkCollections(String collectionsUrl, StringBuilder errorMessages) {
		try {
			Map<String, Object> data = fetchResource(collectionsUrl);

			if (data != null) {
				Object collectionsObj = data.get("collections");
				if (collectionsObj instanceof List) {
					List<Map<String, Object>> collectionsList = ((List<?>) collectionsObj).stream()
						.filter(item -> item instanceof Map)
						.map(item -> (Map<String, Object>) item)
						.toList();

					boolean found = false;
					for (Map<String, Object> collection : collectionsList) {
						List<Map<String, Object>> collectionLinks = getLinks(collection);
						if (collectionLinks == null) {
							continue;
						}

						Map<String, Object> tilesetLink = findLinkByRel(collectionLinks, REL_TILESETS_MAP);

						if (tilesetLink != null) {
							found = true;
							validateHref(tilesetLink, "Collection '" + collection.get("id") + "'", errorMessages);
						}
					}
					return found;
				}
			}
		}
		catch (Exception e) {
			System.out.println("Info: Collections check failed: " + e.getMessage());
		}
		return false;
	}

	protected Map<String, Object> fetchResource(String requestUrl) throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		HttpURLConnection connection = (HttpURLConnection) new URL(requestUrl).openConnection();
		connection.setRequestMethod("GET");
		connection.setRequestProperty("Accept", "application/json");

		if (connection.getResponseCode() == 200) {
			return objectMapper.readValue(connection.getInputStream(), new TypeReference<Map<String, Object>>() {
			});
		}
		return null;
	}

	private List<Map<String, Object>> getLinks(Map<String, Object> source) {
		Object linksObj = source.get("links");
		if (linksObj instanceof List) {
			return ((List<?>) linksObj).stream()
				.filter(item -> item instanceof Map)
				.map(item -> (Map<String, Object>) item)
				.toList();
		}
		return null;
	}

	private void validateHref(Map<String, Object> link, String contextName, StringBuilder errorMessages) {
		String href = (String) link.get("href");
		if (href == null || href.isEmpty()) {
			errorMessages.append(contextName).append(" has a tilesets-map link but the 'href' is missing or empty.\n");
		}
	}

	public static Map<String, Object> findLinkByRel(List<Map<String, Object>> links, String expectedRel) {
		if (links == null) {
			return null;
		}
		for (Map<String, Object> link : links) {
			Object rel = link.get("rel");
			if (expectedRel.equals(rel)) {
				return link;
			}
		}
		return null;
	}

}