package org.opengis.cite.ogcapimaps10.conformance.collectionSelection;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opengis.cite.ogcapimaps10.conformance.CommonFixture;
import org.opengis.cite.ogcapimaps10.conformance.SuiteAttribute;
import org.opengis.cite.ogcapimaps10.domain.InteractiveTestResult;
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A.4.2. Abstract Test for Requirement Collection Selection Response
 *
 * <pre>
 * Abstract Test A.12
 *
 * Identifier: /conf/collections-selection/collections-response
 * Requirement: Requirement 12: /req/collections-selection/collections-response
 *
 * Test purpose:
 * Verify that the implementation responds correctly to map requests using the
 * collections parameter.
 *
 * Test method:
 * Given: a map resource that conformed successfully to /conf/core and that is understood
 *        to consist of multiple collections (e.g., a dataset advertising support for
 *        Dataset Map and featuring multiple collections)
 * When:  retrieving a map using the collections parameter with one and multiple collectionIds
 * Then:
 *   Req 12A: assert that only collections of geospatial data enumerated in the values of
 *            the collections parameter are used to generate the responses for the resource
 *            (map) to which they apply.
 *   Req 12B: assert that if there is more than one collection name and the style applied
 *            does not specify otherwise, the collections are rendered in the result in an
 *            order starting with the first (leftmost) collection and ending with the last
 *            (rightmost).
 * </pre>
 */
@SuppressWarnings("unchecked")
public class CollectionsResponseTest extends CommonFixture {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	/**
	 * <pre>
	 * Abstract Test A.12
	 *
	 * Automated assertions (Req 12A / error conditions §10.2.3):
	 *   - Single valid collection  → HTTP 200
	 *   - Two valid collections    → HTTP 200
	 *   - Non-existent collection  → HTTP 400
	 *
	 * Interactive assertion (Req 12B):
	 *   - Two maps with the same collections in reversed order must look visually
	 *     different, confirming that the server respects the left-to-right rendering
	 *     order (first collection at the bottom of the display stack, last on top).
	 * </pre>
	 */
	@Test(description = "A.12 Abstract Test for Requirement Collection Selection Response "
			+ "(/req/collections-selection/collections-response)")
	public void verifyCollectionsResponse(ITestContext context) {
		List<String> errors = new ArrayList<>();

		// ----------------------------------------------------------------
		// Setup: discover dataset map URL and two renderable leaf collection IDs
		// ----------------------------------------------------------------

		String mapUrl = findDatasetMapUrl();
		if (mapUrl == null) {
			throw new SkipException("No dataset map endpoint found; skipping A.12");
		}

		List<String> collectionIds = findTwoRenderableCollectionIds();
		if (collectionIds == null || collectionIds.size() < 2) {
			throw new SkipException(
					"Could not find two renderable leaf collections (parent containers filtered out); skipping A.12");
		}

		String id1 = collectionIds.get(0);
		String id2 = collectionIds.get(1);

		// ----------------------------------------------------------------
		// Automated — Req 12A: server accepts valid collections and rejects
		// invalid ones (error conditions §10.2.3)
		// ----------------------------------------------------------------

		// 1. Single collection → HTTP 200
		// Raw URL: colons in namespaced IDs (e.g. NaturalEarth:cultural:...) and commas
		// are valid query-string characters (RFC 3986 sub-delimiters) and must NOT be
		// encoded.
		int statusSingle = getStatus(mapUrl + "?collections=" + id1 + "&f=png&width=800&height=400");
		if (statusSingle != HttpURLConnection.HTTP_OK) {
			errors.add(String.format("[Req12A/single] Expected HTTP 200 for ?collections=%s but got %d", id1,
					statusSingle));
		}

		// 2. Two collections → HTTP 200
		int statusTwo = getStatus(mapUrl + "?collections=" + id1 + "," + id2 + "&f=png&width=800&height=400");
		if (statusTwo != HttpURLConnection.HTTP_OK) {
			errors.add(String.format("[Req12A/two] Expected HTTP 200 for ?collections=%s,%s but got %d", id1, id2,
					statusTwo));
		}

		// 3. Non-existent collection → HTTP 400 (error conditions §10.2.3)
		int statusBad = getStatus(mapUrl + "?collections=__nonexistent_cite_collection__");
		if (statusBad != HttpURLConnection.HTTP_BAD_REQUEST) {
			errors.add(String.format("[Req12A/invalid] Expected HTTP 400 for non-existent collection but got %d",
					statusBad));
		}

		// ----------------------------------------------------------------
		// Interactive — Req 12B: rendering order (visual, human-verified)
		// ----------------------------------------------------------------

		InteractiveTestResult interactiveResult = null;
		try {
			Object attr = context.getSuite().getAttribute(SuiteAttribute.INTERACTIVE_TEST_RESULT.getName());
			if (attr instanceof InteractiveTestResult) {
				interactiveResult = (InteractiveTestResult) attr;
			}
		}
		catch (Exception e) {
			// interactive tests not available
		}

		if (interactiveResult != null && interactiveResult.isEnabled()) {
			if (!interactiveResult.isCollectionsResponseCorrect()) {
				errors.add("[Req12B/order] Interactive verification failed: "
						+ "Maps with reversed collection order did not appear visually different. "
						+ "The server must render collections left-to-right "
						+ "(first collection at the bottom of the display stack, last on top).");
			}
		}

		// ----------------------------------------------------------------
		// Final assertion
		// ----------------------------------------------------------------

		clearMessages();

		if (!errors.isEmpty()) {
			StringBuilder message = new StringBuilder();
			message.append("A.12 collections-response verification failed with ")
				.append(errors.size())
				.append(" error(s):\n");
			for (int i = 0; i < errors.size(); i++) {
				message.append("  ").append(i + 1).append(". ").append(errors.get(i)).append("\n");
			}
			throw new AssertionError(message.toString());
		}
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	/**
	 * Finds the dataset map URL from the landing page links (rel=ogc/1.0/map), falling
	 * back to {rootUri}/map.
	 */
	private String findDatasetMapUrl() {
		try {
			Map<String, Object> landingPage = fetchJson(rootUri.toString() + "?f=json");
			if (landingPage != null) {
				List<Map<String, Object>> links = (List<Map<String, Object>>) landingPage.get("links");
				if (links != null) {
					for (Map<String, Object> link : links) {
						String rel = (String) link.get("rel");
						if (rel != null
								&& matchesRelIgnoringScheme(rel, "http://www.opengis.net/def/rel/ogc/1.0/map")) {
							String href = (String) link.get("href");
							URI mapUri = new URI(href);
							return mapUri.isAbsolute() ? href : rootUri.resolve(mapUri).toString();
						}
					}
				}
			}
		}
		catch (Exception e) {
			// fall through
		}
		String base = rootUri.toString();
		if (base.endsWith("/")) {
			base = base.substring(0, base.length() - 1);
		}
		return base + "/map";
	}

	/**
	 * Discovers two renderable leaf collection IDs from {rootUri}/collections. Parent
	 * containers are excluded: a collection is considered a parent if any other
	 * collection's ID starts with {@code {id}:}. Among the remaining leaf collections, a
	 * raster + non-raster pair is preferred to maximise visual distinctiveness when the
	 * order is reversed for Req 12B verification.
	 * @return a list of exactly two leaf collection IDs, or {@code null} if fewer than
	 * two leaf collections are available
	 */
	private List<String> findTwoRenderableCollectionIds() {
		try {
			String base = rootUri.toString();
			if (base.endsWith("/")) {
				base = base.substring(0, base.length() - 1);
			}
			Map<String, Object> response = fetchJson(base + "/collections?f=json");
			if (response == null) {
				return null;
			}
			List<Map<String, Object>> collections = (List<Map<String, Object>>) response.get("collections");
			if (collections == null || collections.size() < 2) {
				return null;
			}

			// Collect all IDs preserving encounter order
			Set<String> allIds = new LinkedHashSet<>();
			for (Map<String, Object> c : collections) {
				String id = (String) c.get("id");
				if (id != null) {
					allIds.add(id);
				}
			}

			// Filter out parent containers: a collection is a parent if any other
			// collection's ID starts with "{id}:"
			List<String> leafIds = new ArrayList<>();
			for (String id : allIds) {
				boolean isParent = false;
				for (String other : allIds) {
					if (other.startsWith(id + ":")) {
						isParent = true;
						break;
					}
				}
				if (!isParent) {
					leafIds.add(id);
				}
			}

			if (leafIds.size() < 2) {
				return null;
			}

			// Prefer a raster leaf + non-raster leaf pair for visual distinctiveness
			// when rendering order is reversed (one raster, one vector/cultural layer)
			String rasterLeaf = null;
			String otherLeaf = null;
			for (String id : leafIds) {
				if (rasterLeaf == null && id.toLowerCase().contains("raster")) {
					rasterLeaf = id;
				}
				else if (otherLeaf == null) {
					otherLeaf = id;
				}
				if (rasterLeaf != null && otherLeaf != null) {
					break;
				}
			}

			String id1;
			String id2;
			if (rasterLeaf != null && otherLeaf != null) {
				id1 = rasterLeaf;
				id2 = otherLeaf;
			}
			else {
				// No raster/non-raster split available; fall back to first two leaf IDs
				id1 = leafIds.get(0);
				id2 = leafIds.get(1);
			}

			return List.of(id1, id2);
		}
		catch (Exception e) {
			return null;
		}
	}

	private Map<String, Object> fetchJson(String urlString) {
		try {
			HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");
			conn.setConnectTimeout(10000);
			conn.setReadTimeout(10000);
			if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
				return OBJECT_MAPPER.readValue(conn.getInputStream(), Map.class);
			}
		}
		catch (Exception e) {
			// return null
		}
		return null;
	}

	private int getStatus(String urlString) {
		try {
			HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(10000);
			conn.setReadTimeout(10000);
			return conn.getResponseCode();
		}
		catch (Exception e) {
			return -1;
		}
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

}