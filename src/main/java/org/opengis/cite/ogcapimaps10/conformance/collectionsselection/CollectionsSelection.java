package org.opengis.cite.ogcapimaps10.conformance.collectionsselection;

import static io.restassured.http.Method.GET;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.opengis.cite.ogcapimaps10.conformance.CommonFixture;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import io.restassured.response.Response;

/**
 * A.10. Abstract Test for Requirement collections-selection parameter.
 *
 * <pre>
 * Abstract test A.35
 *
 * Identifier: /conf/collections-selection/collections-parameter
 * Requirement: Requirement 35: /req/collections-selection/collections-parameter
 * Test purpose: Verify that the implementation supports the collections query parameter
 *               to select which data collections to include in a dataset map response.
 * </pre>
 */
public class CollectionsSelection extends CommonFixture {

	private static final String COLLECTIONS_SELECTION_CONF_URI = "https://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/collections-selection";

	private static final String MAP_REL_HTTPS = "https://www.opengis.net/def/rel/ogc/1.0/map";

	private static final String MAP_REL_HTTP = "http://www.opengis.net/def/rel/ogc/1.0/map";

	private static final String MAP_REL_COMPACT = "ogc-rel:map";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private boolean collectionsSelectionSupported = false;

	private String datasetMapUrl = null;

	private String collectionId1 = null;

	private String collectionId2 = null;

	/**
	 * Checks conformance declaration, discovers the dataset map URL from the landing
	 * page, and collects up to two collection IDs for use in test methods.
	 * @param testContext suite context carrying IUT and basicAuth parameters
	 */
	@BeforeClass
	@Override
	public void initCommonFixture(ITestContext testContext) {
		super.initCommonFixture(testContext);

		Map<String, Object> conformance = fetchJson(baseUrl() + "/conformance?f=json");
		if (conformance == null) {
			System.out.println("[A.35] Could not fetch /conformance — skipping.");
			return;
		}
		List<?> conformsTo = (List<?>) conformance.get("conformsTo");
		if (conformsTo == null) {
			System.out.println("[A.35] conformsTo array missing — skipping.");
			return;
		}
		boolean declared = false;
		for (Object entry : conformsTo) {
			if (entry instanceof String
					&& normalizeScheme((String) entry).equals(normalizeScheme(COLLECTIONS_SELECTION_CONF_URI))) {
				declared = true;
				break;
			}
		}
		if (!declared) {
			System.out.println("[A.35] conf/collections-selection not declared — skipping all tests.");
			return;
		}
		collectionsSelectionSupported = true;

		Map<String, Object> landingPage = fetchJson(baseUrl());
		if (landingPage != null) {
			List<?> links = (List<?>) landingPage.get("links");
			if (links != null) {
				for (Object linkObj : links) {
					Map<String, Object> link = castMap(linkObj);
					if (link == null) {
						continue;
					}
					String rel = (String) link.get("rel");
					if (MAP_REL_HTTPS.equals(rel) || MAP_REL_HTTP.equals(rel) || MAP_REL_COMPACT.equals(rel)) {
						String href = (String) link.get("href");
						if (href != null && !href.isEmpty()) {
							datasetMapUrl = rootUri.resolve(href).toString();
							break;
						}
					}
				}
			}
		}
		if (datasetMapUrl == null) {
			System.out.println("[A.35] No dataset map link found in landing page.");
		}
		else {
			System.out.println("[A.35] Dataset map URL: " + datasetMapUrl);
		}

		Map<String, Object> collectionsResponse = fetchJson(baseUrl() + "/collections?f=json");
		if (collectionsResponse != null) {
			List<?> collections = (List<?>) collectionsResponse.get("collections");
			if (collections != null) {
				for (Object collObj : collections) {
					Map<String, Object> coll = castMap(collObj);
					if (coll == null) {
						continue;
					}
					String id = (String) coll.get("id");
					if (id == null || id.isEmpty()) {
						continue;
					}
					if (collectionId1 == null) {
						collectionId1 = id;
					}
					else if (collectionId2 == null) {
						collectionId2 = id;
						break;
					}
				}
			}
		}
		System.out.println("[A.35] Collection IDs discovered: " + collectionId1 + ", " + collectionId2);
	}

	/**
	 * <pre>
	 * Abstract test A.35 — Case 1 (Req 35/A)
	 *
	 * GET {datasetMapUrl}?collections=id1,id2
	 * Expected: HTTP 200, Content-Type: image/*
	 * </pre>
	 */
	@Test(description = "A.35 Req 35/A: collections=id1,id2 returns HTTP 200 and Content-Type image/*")
	public void verifyValidMultiCollectionSelection() {
		if (!collectionsSelectionSupported) {
			throw new SkipException("Server does not declare conf/collections-selection.");
		}
		if (datasetMapUrl == null) {
			throw new SkipException("No dataset map link found in landing page.");
		}
		if (collectionId1 == null || collectionId2 == null) {
			throw new SkipException("Fewer than 2 collections discovered — cannot test multi-collection selection.");
		}

		String collectionsValue = collectionId1 + "," + collectionId2;
		Response response = init().accept("image/png,image/jpeg,image/*")
			.queryParam("collections", collectionsValue)
			.when()
			.request(GET, datasetMapUrl);

		Assert.assertEquals(response.statusCode(), 200, "GET " + datasetMapUrl + "?collections=" + collectionsValue
				+ " must return HTTP 200 but returned " + response.statusCode() + ".");
		String contentType = response.contentType();
		Assert.assertNotNull(contentType,
				"Response for collections=" + collectionsValue + " must include a Content-Type header.");
		Assert.assertTrue(contentType.toLowerCase().startsWith("image/"), "Response for collections=" + collectionsValue
				+ " must have Content-Type image/*, but got '" + contentType + "'.");
	}

	/**
	 * <pre>
	 * Abstract test A.35 — Case 2 (Req 35/C)
	 *
	 * GET {datasetMapUrl}?collections=id1
	 * Expected: HTTP 200, Content-Type: image/*
	 * </pre>
	 */
	@Test(description = "A.35 Req 35/C: collections=id1 (single ID) returns HTTP 200 and Content-Type image/*")
	public void verifyValidSingleCollectionSelection() {
		if (!collectionsSelectionSupported) {
			throw new SkipException("Server does not declare conf/collections-selection.");
		}
		if (datasetMapUrl == null) {
			throw new SkipException("No dataset map link found in landing page.");
		}
		if (collectionId1 == null) {
			throw new SkipException("No collections discovered.");
		}

		Response response = init().accept("image/png,image/jpeg,image/*")
			.queryParam("collections", collectionId1)
			.when()
			.request(GET, datasetMapUrl);

		Assert.assertEquals(response.statusCode(), 200, "GET " + datasetMapUrl + "?collections=" + collectionId1
				+ " must return HTTP 200 but returned " + response.statusCode() + ".");
		String contentType = response.contentType();
		Assert.assertNotNull(contentType,
				"Response for collections=" + collectionId1 + " must include a Content-Type header.");
		Assert.assertTrue(contentType.toLowerCase().startsWith("image/"), "Response for collections=" + collectionId1
				+ " must have Content-Type image/*, but got '" + contentType + "'.");
	}

	/**
	 * <pre>
	 * Abstract test A.35 — Case 3 (Req 35/D)
	 *
	 * GET {datasetMapUrl}  (no collections parameter)
	 * Expected: HTTP 200, Content-Type: image/*
	 * </pre>
	 */
	@Test(description = "A.35 Req 35/D: no collections parameter returns HTTP 200 and Content-Type image/*")
	public void verifyDefaultNoParameter() {
		if (!collectionsSelectionSupported) {
			throw new SkipException("Server does not declare conf/collections-selection.");
		}
		if (datasetMapUrl == null) {
			throw new SkipException("No dataset map link found in landing page.");
		}

		Response response = init().accept("image/png,image/jpeg,image/*").when().request(GET, datasetMapUrl);

		Assert.assertEquals(response.statusCode(), 200, "GET " + datasetMapUrl
				+ " (no collections parameter) must return HTTP 200 but returned " + response.statusCode() + ".");
		String contentType = response.contentType();
		Assert.assertNotNull(contentType, "Response without collections parameter must include a Content-Type header.");
		Assert.assertTrue(contentType.toLowerCase().startsWith("image/"),
				"Response without collections parameter must have Content-Type image/*, but got '" + contentType
						+ "'.");
	}

	/**
	 * <pre>
	 * Abstract test A.35 — Case 4 (Req 35/E)
	 *
	 * GET {datasetMapUrl}?collections=nonexistent_xyz_9999
	 * Expected: HTTP 400
	 * </pre>
	 */
	@Test(description = "A.35 Req 35/E: collections=nonexistent_xyz_9999 returns HTTP 400")
	public void verifyInvalidCollectionId() {
		if (!collectionsSelectionSupported) {
			throw new SkipException("Server does not declare conf/collections-selection.");
		}
		if (datasetMapUrl == null) {
			throw new SkipException("No dataset map link found in landing page.");
		}

		Response response = init().accept("image/png,image/jpeg,image/*")
			.queryParam("collections", "nonexistent_xyz_9999")
			.when()
			.request(GET, datasetMapUrl);

		Assert.assertEquals(response.statusCode(), 400, "GET " + datasetMapUrl
				+ "?collections=nonexistent_xyz_9999 must return HTTP 400 but returned " + response.statusCode() + ".");
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> fetchJson(String url) {
		try {
			HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");
			conn.setConnectTimeout(30000);
			conn.setReadTimeout(30000);
			applyAuth(conn);
			int status = conn.getResponseCode();
			if (status < 200 || status >= 300) {
				return null;
			}
			try (InputStream in = conn.getInputStream()) {
				return OBJECT_MAPPER.readValue(in, Map.class);
			}
		}
		catch (Exception e) {
			return null;
		}
	}

	private String baseUrl() {
		String url = rootUri.toString();
		return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
	}

	private static String normalizeScheme(String uri) {
		if (uri != null && uri.startsWith("https://")) {
			return "http://" + uri.substring("https://".length());
		}
		return uri;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> castMap(Object obj) {
		return obj instanceof Map ? (Map<String, Object>) obj : null;
	}

}
