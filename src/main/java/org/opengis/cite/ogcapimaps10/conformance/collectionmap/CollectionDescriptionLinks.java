package org.opengis.cite.ogcapimaps10.conformance.collectionmap;

import static io.restassured.http.Method.GET;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.opengis.cite.ogcapimaps10.conformance.CommonDataFixture;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

/**
 * A.13.1. Abstract Test for Requirement collection description links.
 *
 */
public class CollectionDescriptionLinks extends CommonDataFixture {

	private static final String MAP_REL_HTTPS = "https://www.opengis.net/def/rel/ogc/1.0/map";

	private static final String MAP_REL_HTTP = "http://www.opengis.net/def/rel/ogc/1.0/map";

	private static final String MAP_REL_COMPACT = "ogc-rel:map";

	/**
	 * <pre>
	 * Abstract test A.46
	 *
	 * Identifier: /conf/collection-map/desc-links
	 * Requirement: Requirement 46: /req/collection-map/desc-links
	 * Test purpose: Verify that the implementation links correctly from the collection description resource to the map resource
	 *
	 * Test Method:
	 * Given: a collection from an API implementation conforming to OGC API-Common-Part 2: Geospatial Data “Collections” conformance class
	 * When: retrieving the JSON representation of the description for that collection
	 * Then:
	 * - assert that the OGC API collection description includes a link with relation type https://www.opengis.net/def/rel/ogc/1.0/map (or [ogc-rel:map]) and the href pointing to a the map resource for this collection.
	 * </pre>
	 */
	@Test(description = "Implements A.13.1. Abstract Test for Requirement collection description links "
			+ "(Requirement /req/collection-map/desc-links)")
	public void verifyCollectionDescriptionLinks() {
		Response collectionsResponse = init().baseUri(rootUri.toString())
			.accept("application/json")
			.when()
			.request(GET, collectionResource("collections"));
		assertSuccessfulJsonResponse(collectionsResponse, "Collections resource");

		List<Map<String, Object>> collections = collectionsResponse.jsonPath().getList("collections");
		Assert.assertNotNull(collections, "Collections JSON response must include a 'collections' array.");
		if (collections.isEmpty()) {
			throw new SkipException("No collections are available to test Requirement /req/collection-map/desc-links.");
		}

		int limit = Math.min(noOfCollections, collections.size());
		for (int index = 0; index < limit; index++) {
			String collectionId = valueAsString(collections.get(index).get("id"));
			Assert.assertNotNull(collectionId, "Collection at index " + index + " must include an 'id' value.");
			Assert.assertFalse(collectionId.isEmpty(),
					"Collection at index " + index + " must include a non-empty 'id' value.");

			Response descriptionResponse = init().accept("application/json")
				.when()
				.get(collectionResource("collections/{collectionId}"), collectionId);
			String context = "Collection description for '" + collectionId + "'";
			assertSuccessfulJsonResponse(descriptionResponse, context);
			verifyMapLink(descriptionResponse.jsonPath(), collectionId);
		}
	}

	private void verifyMapLink(JsonPath collectionDescription, String collectionId) {
		List<Map<String, Object>> links = collectionDescription.getList("links");
		Assert.assertNotNull(links, "Collection description for '" + collectionId + "' must include a 'links' array.");

		Map<String, Object> mapLink = findMapLink(links);
		Assert.assertNotNull(mapLink, "Collection description for '" + collectionId
				+ "' must include a map link with rel '" + MAP_REL_HTTPS + "' or '" + MAP_REL_COMPACT + "'.");

		String href = valueAsString(mapLink.get("href"));
		Assert.assertNotNull(href,
				"Map link in collection description for '" + collectionId + "' must include an 'href' value.");
		Assert.assertFalse(href.isEmpty(),
				"Map link in collection description for '" + collectionId + "' must include a non-empty 'href' value.");

		try {
			URI resolvedMapUri = rootUri.resolve(URI.create(href));
			Assert.assertTrue(resolvedMapUri.isAbsolute(), "Map link href '" + href
					+ "' in collection description for '" + collectionId + "' must resolve to an absolute URI.");
		}
		catch (IllegalArgumentException e) {
			Assert.fail("Map link href '" + href + "' in collection description for '" + collectionId
					+ "' is not a valid URI.", e);
		}
	}

	private void assertSuccessfulJsonResponse(Response response, String resourceName) {
		Assert.assertTrue(response.statusCode() >= 200 && response.statusCode() < 300, resourceName
				+ " must return a successful HTTP status code, but returned " + response.statusCode() + ".");
		try {
			response.jsonPath();
		}
		catch (RuntimeException e) {
			Assert.fail(resourceName + " response must be valid JSON.", e);
		}
	}

	private String collectionResource(String relativePath) {
		String base = rootUri.toString();
		if (!base.endsWith("/")) {
			base = base + "/";
		}
		return base + relativePath;
	}

	private static Map<String, Object> findMapLink(List<Map<String, Object>> links) {
		for (Map<String, Object> link : links) {
			String rel = valueAsString(link.get("rel"));
			if (MAP_REL_HTTPS.equals(rel) || MAP_REL_HTTP.equals(rel) || MAP_REL_COMPACT.equals(rel)) {
				return link;
			}
		}
		return null;
	}

	private static String valueAsString(Object value) {
		return value instanceof String ? (String) value : null;
	}

}
