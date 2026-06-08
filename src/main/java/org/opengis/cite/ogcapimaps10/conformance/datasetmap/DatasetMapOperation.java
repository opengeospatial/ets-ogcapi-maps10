package org.opengis.cite.ogcapimaps10.conformance.datasetmap;

import static io.restassured.http.Method.GET;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.opengis.cite.ogcapimaps10.conformance.CommonFixture;
import org.testng.Assert;
import org.testng.annotations.Test;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

/**
 * A.14.4. Abstract Test for Requirement dataset map operation.
 *
 * <pre>
 * Abstract test A.52
 *
 * Identifier: /conf/dataset-map/operation
 * Requirement: Requirement 52: /req/dataset-map/operation
 * Test purpose: Verify that the implementation supports retrieving a dataset maps
 * resource exposed by the OGC API - Maps implementation.
 * </pre>
 */
public class DatasetMapOperation extends CommonFixture {

	private static final String MAP_REL_HTTPS = "https://www.opengis.net/def/rel/ogc/1.0/map";

	private static final String MAP_REL_HTTP = "http://www.opengis.net/def/rel/ogc/1.0/map";

	private static final String MAP_REL_COMPACT = "ogc-rel:map";

	private static final String DEFAULT_ACCEPT = "image/*";

	/**
	 * <pre>
	 * Abstract test A.52
	 *
	 * Identifier: /conf/dataset-map/operation
	 * Requirement: Requirement 52: /req/dataset-map/operation
	 * Test purpose: Verify that the dataset map resource can be retrieved using HTTP
	 *               GET in the default style.
	 * </pre>
	 */
	@Test(description = "Implements A.14.4. Abstract Test for Requirement dataset map operation "
			+ "(Requirement /req/dataset-map/operation)")
	public void DatasetMapOperation() {
		Response landingPageResponse = init().baseUri(rootUri.toString())
			.accept("application/json")
			.when()
			.request(GET);
		Assert.assertTrue(landingPageResponse.statusCode() >= 200 && landingPageResponse.statusCode() < 300,
				"Landing page JSON response must have a successful HTTP status code.");

		JsonPath json;
		try {
			json = landingPageResponse.jsonPath();
		}
		catch (RuntimeException e) {
			Assert.fail("Landing page response must be valid JSON.", e);
			return;
		}

		List<Map<String, Object>> links = json.getList("links");
		Assert.assertNotNull(links, "Landing page JSON response must include a 'links' array.");
		Assert.assertFalse(links.isEmpty(), "Landing page JSON response 'links' array must not be empty.");

		Map<String, Object> mapLink = findDatasetMapLink(links);
		Assert.assertNotNull(mapLink, "Landing page must include a dataset map link with rel '" + MAP_REL_HTTPS
				+ "', legacy rel '" + MAP_REL_HTTP + "', or compact rel '" + MAP_REL_COMPACT + "'.");

		String href = valueAsString(mapLink.get("href"));
		Assert.assertNotNull(href, "Dataset map link must include an 'href' value.");
		Assert.assertFalse(href.isEmpty(), "Dataset map link 'href' value must not be empty.");

		URI mapUri = resolveLink(href);
		String accept = mediaTypeOrDefault(mapLink.get("type"));
		Response mapResponse = init().baseUri(mapUri.toString()).accept(accept).when().request(GET);

		Assert.assertEquals(mapResponse.statusCode(), 200,
				"HTTP GET on the dataset map resource must return status code 200.");
		String contentType = mapResponse.getContentType();
		Assert.assertNotNull(contentType, "Dataset map response must include a Content-Type header.");
		Assert.assertTrue(contentType.startsWith("image/"),
				"Dataset map response Content-Type must be an image media type, but was: " + contentType);
		Assert.assertTrue(mapResponse.getBody().asByteArray().length > 0,
				"Dataset map response body must not be empty.");
	}

	private static Map<String, Object> findDatasetMapLink(List<Map<String, Object>> links) {
		for (Map<String, Object> link : links) {
			String rel = valueAsString(link.get("rel"));
			if (MAP_REL_HTTPS.equals(rel) || MAP_REL_HTTP.equals(rel) || MAP_REL_COMPACT.equals(rel)) {
				return link;
			}
		}
		return null;
	}

	private URI resolveLink(String href) {
		String base = rootUri.toString();
		if (!base.endsWith("/")) {
			base = base + "/";
		}
		return URI.create(base).resolve(href);
	}

	private static String mediaTypeOrDefault(Object value) {
		String mediaType = valueAsString(value);
		if (mediaType == null || mediaType.isEmpty() || !mediaType.startsWith("image/")) {
			return DEFAULT_ACCEPT;
		}
		return mediaType;
	}

	private static String valueAsString(Object value) {
		return value instanceof String ? (String) value : null;
	}

}
