package org.opengis.cite.ogcapimaps10.conformance.collectionSelection;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.opengis.cite.ogcapimaps10.conformance.CommonFixture;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A.11 Abstract Test for Requirement /req/collections-selection/collections-parameter
 */
@SuppressWarnings("unchecked")
public class CollectionsParameterTest extends CommonFixture {

	/**
	 * <pre>
	 * Abstract Test A.11
	 *
	 * Identifier: /conf/collections-selection/collections-parameter
	 * Requirement: Requirement 11: /req/collections-selection/collections-parameter
	 *
	 * Test purpose:
	 * Verify that a map resource consisting of multiple collections supports
	 * the optional 'collections' query parameter.
	 *
	 * Test Method:
	 * Given: a map resource consisting of multiple collections
	 * When: retrieving a map using the 'collections' parameter with one or more collection identifiers
	 * Then: assert that the implementation supports the 'collections' parameter
	 * </pre>
	 */
	@Test(description = "Implements A.11. Abstract Test for collections parameter support")
	public void verifyCollectionsParameterSupport() throws Exception {

		ObjectMapper objectMapper = new ObjectMapper();

		// Step 1: Retrieve collections
		String collectionsUrl = rootUri.toString() + "/collections?f=json";
		HttpURLConnection collectionsConnection = (HttpURLConnection) new URL(collectionsUrl).openConnection();
		collectionsConnection.setRequestMethod("GET");
		collectionsConnection.setRequestProperty("Accept", "application/json");

		Map<String, Object> collectionsResponse = objectMapper.readValue(collectionsConnection.getInputStream(),
				Map.class);

		List<Map<String, Object>> collections = (List<Map<String, Object>>) collectionsResponse.get("collections");

		if (collections == null || collections.size() < 2) {
			throw new SkipException("Less than two collections available; skipping A.11");
		}

		// Step 2: Find collections that support map links
		List<Map<String, Object>> mapCollections = new ArrayList<>();

		for (Map<String, Object> collection : collections) {
			List<Map<String, Object>> links = (List<Map<String, Object>>) collection.get("links");
			Map<String, Object> mapLink = findLinkByRel(links, "http://www.opengis.net/def/rel/ogc/1.0/map");

			if (mapLink != null) {
				mapCollections.add(collection);
			}
		}

		if (mapCollections.size() < 2) {
			throw new SkipException("Less than two collections support map resources; skipping A.11");
		}

		// Step 3: Build collections parameter (IDs)
		Map<String, Object> c1 = mapCollections.get(0);
		Map<String, Object> c2 = mapCollections.get(1);

		String id1 = (String) c1.get("id");
		String id2 = (String) c2.get("id");

		Assert.assertNotNull(id1, "Collection id must not be null");
		Assert.assertNotNull(id2, "Collection id must not be null");

		String collectionsParamIds = id1 + "," + id2;

		// Step 4: Resolve a map endpoint
		List<Map<String, Object>> links1 = (List<Map<String, Object>>) c1.get("links");
		Map<String, Object> mapLink1 = findLinkByRel(links1, "http://www.opengis.net/def/rel/ogc/1.0/map");

		String mapHref = (String) mapLink1.get("href");
		URI mapUri = new URI(mapHref);
		if (!mapUri.isAbsolute()) {
			mapUri = rootUri.resolve(mapUri);
		}

		// Step 5: Request map using collection IDs
		URI requestUriIds = new URI(mapUri.toString() + "?collections=" + collectionsParamIds);

		HttpURLConnection mapConnectionIds = (HttpURLConnection) requestUriIds.toURL().openConnection();
		mapConnectionIds.setRequestMethod("GET");
		mapConnectionIds.setConnectTimeout(5000);
		mapConnectionIds.setReadTimeout(5000);

		int statusIds = mapConnectionIds.getResponseCode();
		Assert.assertEquals(statusIds, HttpURLConnection.HTTP_OK, "Map request with collections IDs failed");

		// Step 6: Build collections parameter (full URLs)
		URI collectionUri1 = rootUri.resolve("/collections/" + id1);
		URI collectionUri2 = rootUri.resolve("/collections/" + id2);

		String collectionsParamUrls = collectionUri1.toString() + "," + collectionUri2.toString();

		URI requestUriUrls = new URI(mapUri.toString() + "?collections=" + collectionsParamUrls);

		HttpURLConnection mapConnectionUrls = (HttpURLConnection) requestUriUrls.toURL().openConnection();
		mapConnectionUrls.setRequestMethod("GET");
		mapConnectionUrls.setConnectTimeout(5000);
		mapConnectionUrls.setReadTimeout(5000);

		int statusUrls = mapConnectionUrls.getResponseCode();
		Assert.assertEquals(statusUrls, HttpURLConnection.HTTP_OK, "Map request with collections URLs failed");
	}

	private static Map<String, Object> findLinkByRel(List<Map<String, Object>> links, String expectedRel) {

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