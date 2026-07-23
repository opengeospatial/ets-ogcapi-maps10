package org.opengis.cite.ogcapimaps10.conformance.collectionsselection;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opengis.cite.ogcapimaps10.conformance.CommonFixture;
import org.opengis.cite.ogcapimaps10.conformance.SuiteAttribute;
import org.opengis.cite.ogcapimaps10.domain.CollectionsResponseInteractiveTestResult;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * A.36 Abstract Test for Requirement collections-selection response.
 *
 * <pre>
 * Abstract Test A.36
 *
 * Identifier: /conf/collections-selection/collections-response
 * Requirement: Requirement 36: /req/collections-selection/collections-response
 * Test purpose: Verify that the implementation uses only the specified collections
 *               when generating a map response, and renders them in the declared
 *               draw order (leftmost collection at the bottom, rightmost at the top).
 *
 * Test method: Interactive — the human tester visually compares side-by-side map
 *              images presented in the TEAMENGINE form and confirms:
 *              Req 36/A: the filtered map (collections=id1) differs from the default map.
 *              Req 36/B: the forward-order map (collections=id1,id2) differs from the
 *                        reverse-order map (collections=id2,id1) in draw order.
 * </pre>
 */
public class CollectionsResponse extends CommonFixture {

	private static final String COLLECTIONS_SELECTION_CONF_URI = "https://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/collections-selection";

	private static final String MAP_REL_HTTPS = "https://www.opengis.net/def/rel/ogc/1.0/map";

	private static final String MAP_REL_HTTP = "http://www.opengis.net/def/rel/ogc/1.0/map";

	private static final String MAP_REL_COMPACT = "ogc-rel:map";

	private static final String COVERAGE_REL = "http://www.opengis.net/def/rel/ogc/1.0/coverage";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private boolean collectionsSelectionSupported = false;

	private String datasetMapUrl = null;

	private String collectionId1 = null;

	private String collectionId2 = null;

	/**
	 * Checks conformance declaration, discovers the dataset map URL from the landing
	 * page, and collects up to two collection IDs for use as skip guards in test methods.
	 * @param testContext suite context carrying IUT and basicAuth parameters
	 */
	@BeforeClass
	@Override
	public void initCommonFixture(ITestContext testContext) {
		super.initCommonFixture(testContext);

		Map<String, Object> conformance = fetchJson(baseUrl() + "/conformance?f=json");
		if (conformance == null) {
			System.out.println("[A.36] Could not fetch /conformance — skipping.");
			return;
		}
		List<?> conformsTo = (List<?>) conformance.get("conformsTo");
		if (conformsTo == null) {
			System.out.println("[A.36] conformsTo array missing — skipping.");
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
			System.out.println("[A.36] conf/collections-selection not declared — skipping all tests.");
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
			System.out.println("[A.36] No dataset map link found in landing page.");
		}
		else {
			System.out.println("[A.36] Dataset map URL: " + datasetMapUrl);
		}

		// Two-pass collection selection: prefer map-capable, non-raster collections.
		// Two fully opaque rasters produce identical visuals in both draw orders,
		// making Req 36/B untestable. Non-rasters (no coverage link) are picked first;
		// rasters (coverage link present) are only used as a last resort.
		Map<String, Object> collectionsResponse = fetchJson(baseUrl() + "/collections?f=json");
		List<String> preferredIds = new ArrayList<>();
		List<String> fallbackIds = new ArrayList<>();
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
					List<?> collLinks = (List<?>) coll.get("links");
					boolean hasMapLink = false;
					boolean hasCoverageLink = false;
					if (collLinks != null) {
						for (Object linkObj : collLinks) {
							Map<String, Object> link = castMap(linkObj);
							if (link == null) {
								continue;
							}
							String rel = (String) link.get("rel");
							if (MAP_REL_HTTPS.equals(rel) || MAP_REL_HTTP.equals(rel) || MAP_REL_COMPACT.equals(rel)) {
								hasMapLink = true;
							}
							if (COVERAGE_REL.equals(rel)) {
								hasCoverageLink = true;
							}
						}
					}
					if (!hasMapLink) {
						continue;
					}
					if (!hasCoverageLink) {
						preferredIds.add(id);
					}
					else {
						fallbackIds.add(id);
					}
					if (preferredIds.size() >= 2) {
						break;
					}
				}
			}
		}
		List<String> candidates = new ArrayList<>(preferredIds);
		candidates.addAll(fallbackIds);
		collectionId1 = candidates.size() > 0 ? candidates.get(0) : null;
		collectionId2 = candidates.size() > 1 ? candidates.get(1) : null;
		System.out.println("[A.36] Collection IDs discovered: " + collectionId1 + ", " + collectionId2);
	}

	/**
	 * <pre>
	 * Abstract Test A.36 — Case 1 (Req 36/A)
	 *
	 * Interactive verification: tester confirms that GET {datasetMapUrl}?collections=id1
	 * produces a map that differs visually from GET {datasetMapUrl} (no parameter),
	 * proving the server restricts rendering to only the specified collection.
	 * </pre>
	 * @param testContext the TestNG test context, used to access the suite attribute
	 * holding the interactive test result
	 */
	@Test(description = "A.36 Req 36/A (interactive): tester confirms filtered map "
			+ "(collections=id1) visually differs from default map — only specified collection rendered.")
	public void verifyCollectionsFilteredVsDefault(ITestContext testContext) {
		if (!collectionsSelectionSupported) {
			throw new SkipException("Server does not declare conf/collections-selection.");
		}
		if (datasetMapUrl == null) {
			throw new SkipException("No dataset map link found in landing page.");
		}
		if (collectionId1 == null) {
			throw new SkipException("No collections discovered.");
		}

		CollectionsResponseInteractiveTestResult result = (CollectionsResponseInteractiveTestResult) testContext
			.getSuite()
			.getAttribute(SuiteAttribute.COLLECTIONS_RESPONSE_INTERACTIVE_TEST_RESULT.getName());

		if (result == null || !result.isEnabled()) {
			throw new SkipException("Interactive verification for A.36 Req 36/A not enabled. "
					+ "Enable via TEAMENGINE form or set collections_response_interactive_enabled=true.");
		}

		Assert.assertTrue(result.isFilterAppliedCorrect(),
				"A.36 Req 36/A: Tester reported that the filtered map (collections=" + collectionId1
						+ ") does NOT visually differ from the default map — "
						+ "the server does not appear to be restricting rendering to the specified collection.");
	}

	/**
	 * <pre>
	 * Abstract Test A.36 — Case 2 (Req 36/B)
	 *
	 * Interactive verification: tester confirms that GET {datasetMapUrl}?collections=id1,id2
	 * produces a map that differs visually in draw order from
	 * GET {datasetMapUrl}?collections=id2,id1, proving the server renders collections
	 * in the declared left-to-right order (leftmost at the bottom, rightmost at the top).
	 * </pre>
	 * @param testContext the TestNG test context, used to access the suite attribute
	 * holding the interactive test result
	 */
	@Test(description = "A.36 Req 36/B (interactive): tester confirms forward-order map "
			+ "(collections=id1,id2) differs from reverse-order map (collections=id2,id1) — draw order respected.")
	public void verifyCollectionsDrawOrder(ITestContext testContext) {
		if (!collectionsSelectionSupported) {
			throw new SkipException("Server does not declare conf/collections-selection.");
		}
		if (datasetMapUrl == null) {
			throw new SkipException("No dataset map link found in landing page.");
		}
		if (collectionId1 == null || collectionId2 == null) {
			throw new SkipException("Fewer than 2 collections discovered — cannot test draw order.");
		}

		CollectionsResponseInteractiveTestResult result = (CollectionsResponseInteractiveTestResult) testContext
			.getSuite()
			.getAttribute(SuiteAttribute.COLLECTIONS_RESPONSE_INTERACTIVE_TEST_RESULT.getName());

		if (result == null || !result.isEnabled()) {
			throw new SkipException("Interactive verification for A.36 Req 36/B not enabled. "
					+ "Enable via TEAMENGINE form or set collections_response_interactive_enabled=true.");
		}

		Assert.assertTrue(result.isDrawOrderCorrect(),
				"A.36 Req 36/B: Tester reported that the forward-order map (collections=" + collectionId1 + ","
						+ collectionId2 + ") does NOT differ in draw order from the reverse-order map (collections="
						+ collectionId2 + "," + collectionId1
						+ ") — the server does not appear to be applying the declared collection rendering order.");
	}

	// -------------------------------------------------------------------------
	// Private helpers
	// -------------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	private Map<String, Object> fetchJson(String url) {
		try {
			HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");
			conn.setConnectTimeout(30000);
			conn.setReadTimeout(30000);
			conn.setInstanceFollowRedirects(true);
			applyAuth(conn);
			int status = conn.getResponseCode();
			if (status < 200 || status >= 300) {
				return null;
			}
			try (InputStream in = conn.getInputStream()) {
				return OBJECT_MAPPER.readValue(in, new TypeReference<Map<String, Object>>() {
				});
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
