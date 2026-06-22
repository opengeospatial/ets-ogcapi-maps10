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
 * A.13.3. Abstract Test for Requirement collection map operation.
 *
 */
public class CollectionMapOperation extends CommonDataFixture {

	private static final String MAP_REL_HTTPS = "https://www.opengis.net/def/rel/ogc/1.0/map";

	private static final String MAP_REL_HTTP = "http://www.opengis.net/def/rel/ogc/1.0/map";

	private static final String MAP_REL_COMPACT = "ogc-rel:map";

	/**
	 * <pre>
	 * Abstract test A.48
	 *
	 * Identifier: /conf/collection-map/map-operation
	 * Requirement: Requirement 48: /req/collection-map/map-operation
	 * Test purpose: Verify that the implementation supports retrieving maps from an OGC API collection resource as defined in the OGC API - Common Standard.
	 *
	 * Test Method:
	 * Given: a collection correctly linking to a map resource as per /conf/collection-map/desc-links
	 * When: retrieving a map for that collection resource as per /conf/core
	 * Then:
	 * - assert that every OGC API collection available as a map supports an HTTP GET operation to a URL /collections/{collectionId}/map to retrieve a map from that collection resource.
	 * </pre>
	 */
	@Test(description = "Implements A.13.3. Abstract Test for Requirement collection map operation "
			+ "(Requirement /req/collection-map/map-operation)")
	public void verifyCollectionMapOperation() {
		Response collectionsResponse = init().baseUri(rootUri.toString())
			.accept("application/json")
			.when()
			.request(GET, collectionResource("collections"));
		assertSuccessfulJsonResponse(collectionsResponse, "Collections resource");

		List<Map<String, Object>> collections = collectionsResponse.jsonPath().getList("collections");
		Assert.assertNotNull(collections, "Collections JSON response must include a 'collections' array.");
		if (collections.isEmpty()) {
			throw new SkipException(
					"No collections are available to test Requirement /req/collection-map/map-operation.");
		}

		int mapCollections = 0;
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

			Map<String, Object> mapLink = findMapLink(descriptionResponse.jsonPath(), collectionId);
			if (mapLink == null) {
				continue;
			}

			mapCollections++;
			verifyMapResource(mapLink, collectionId);
		}

		if (mapCollections == 0) {
			throw new SkipException("No collection descriptions include a map link to test Requirement "
					+ "/req/collection-map/map-operation.");
		}
	}

	private Map<String, Object> findMapLink(JsonPath collectionDescription, String collectionId) {
		List<Map<String, Object>> links = collectionDescription.getList("links");
		Assert.assertNotNull(links, "Collection description for '" + collectionId + "' must include a 'links' array.");
		return findMapLink(links);
	}

	private void verifyMapResource(Map<String, Object> mapLink, String collectionId) {
		String href = valueAsString(mapLink.get("href"));
		Assert.assertNotNull(href,
				"Map link in collection description for '" + collectionId + "' must include an 'href' value.");
		Assert.assertFalse(href.isEmpty(),
				"Map link in collection description for '" + collectionId + "' must include a non-empty 'href' value.");

		URI mapUri = resolveMapUri(href, collectionId);
		Response mapResponse = init().accept("image/png,image/jpeg,image/*").when().request(GET, mapUri.toString());

		Assert.assertTrue(mapResponse.statusCode() >= 200 && mapResponse.statusCode() < 300,
				"Map resource '" + mapUri + "' for collection '" + collectionId
						+ "' must support HTTP GET with a successful status code, but returned "
						+ mapResponse.statusCode() + ".");

		String contentType = mapResponse.contentType();
		Assert.assertNotNull(contentType,
				"Map resource '" + mapUri + "' for collection '" + collectionId + "' must include a Content-Type.");
		Assert.assertTrue(contentType.toLowerCase().startsWith("image/"),
				"Map resource '" + mapUri + "' for collection '" + collectionId
						+ "' must return an image media type, but returned '" + contentType + "'.");
	}

	private URI resolveMapUri(String href, String collectionId) {
		try {
			URI hrefUri = URI.create(href);
			if (hrefUri.isAbsolute()) {
				return hrefUri;
			}
			return rootUriWithTrailingSlash().resolve(hrefUri);
		}
		catch (IllegalArgumentException e) {
			Assert.fail("Map link href '" + href + "' in collection description for '" + collectionId
					+ "' is not a valid URI.", e);
			return null;
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

	private URI rootUriWithTrailingSlash() {
		String base = rootUri.toString();
		if (!base.endsWith("/")) {
			base = base + "/";
		}
		return URI.create(base);
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
