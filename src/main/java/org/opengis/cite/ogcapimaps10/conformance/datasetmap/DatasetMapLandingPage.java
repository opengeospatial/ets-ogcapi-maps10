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
 * A.14.1. Abstract Test for Requirement dataset landing page.
 *
 * <pre>
 * Abstract test A.49
 *
 * Identifier: /conf/dataset-map/landingpage
 * Requirement: Requirement 49: /req/dataset-map/landingpage
 * Test purpose: Verify that the implementation supports linking properly from an
 * OGC API landing page to a map resource.
 * </pre>
 */
public class DatasetMapLandingPage extends CommonFixture {

	private static final String MAP_REL_HTTPS = "https://www.opengis.net/def/rel/ogc/1.0/map";

	private static final String MAP_REL_HTTP = "http://www.opengis.net/def/rel/ogc/1.0/map";

	private static final String MAP_REL_COMPACT = "ogc-rel:map";

	/**
	 * <pre>
	 * Abstract test A.49
	 *
	 * Identifier: /conf/dataset-map/landingpage
	 * Requirement: Requirement 49: /req/dataset-map/landingpage
	 * Test purpose: Verify that the deployed API endpoint landing page includes a map
	 *               link to the dataset map resource at /map.
	 * </pre>
	 */
	@Test(description = "Implements A.14.1. Abstract Test for Requirement dataset landing page "
			+ "(Requirement /req/dataset-map/landingpage)")
	public void verifyDatasetMapLinkFromLandingPage() {
		Response response = init().baseUri(rootUri.toString()).accept("application/json").when().request(GET);
		Assert.assertTrue(response.statusCode() >= 200 && response.statusCode() < 300,
				"Landing page JSON response must have a successful HTTP status code.");

		JsonPath json;
		try {
			json = response.jsonPath();
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
		Assert.assertEquals(mapUri.getScheme(), rootUri.getScheme(),
				"Dataset map link 'href' must resolve to the same URI scheme as the API landing page.");
		Assert.assertEquals(mapUri.getHost(), rootUri.getHost(),
				"Dataset map link 'href' must resolve to the same host as the API landing page.");
		Assert.assertEquals(effectivePort(mapUri), effectivePort(rootUri),
				"Dataset map link 'href' must resolve to the same port as the API landing page.");
		Assert.assertEquals(normalizePath(mapUri.getPath()), expectedDatasetMapPath(),
				"Dataset map link 'href' must point to the dataset map resource at /map.");
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

	private String expectedDatasetMapPath() {
		String rootPath = normalizePath(rootUri.getPath());
		return rootPath.isEmpty() ? "/map" : rootPath + "/map";
	}

	private URI resolveLink(String href) {
		String base = rootUri.toString();
		if (!base.endsWith("/")) {
			base = base + "/";
		}
		return URI.create(base).resolve(href);
	}

	private static String normalizePath(String path) {
		if (path == null || path.isEmpty() || "/".equals(path)) {
			return "";
		}
		return path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
	}

	private static int effectivePort(URI uri) {
		if (uri.getPort() != -1) {
			return uri.getPort();
		}
		if ("http".equalsIgnoreCase(uri.getScheme())) {
			return 80;
		}
		if ("https".equalsIgnoreCase(uri.getScheme())) {
			return 443;
		}
		return -1;
	}

	private static String valueAsString(Object value) {
		return value instanceof String ? (String) value : null;
	}

}
