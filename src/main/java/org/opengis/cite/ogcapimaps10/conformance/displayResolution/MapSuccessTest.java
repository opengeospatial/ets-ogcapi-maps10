package org.opengis.cite.ogcapimaps10.conformance.displayResolution;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.opengis.cite.ogcapimaps10.conformance.CommonFixture;
import org.opengis.cite.ogcapimaps10.conformance.SuiteAttribute;
import org.opengis.cite.ogcapimaps10.domain.MapSuccessInteractiveTestResult;
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.Test;

import io.restassured.http.Method;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

/**
 * Implements Abstract Test A.17: /conf/display-resolution/map-success
 *
 * <p>
 * Test Purpose: Verify that the server returns a successful map response when valid
 * mm-per-pixel values are provided (Requirement 17: /req/display-resolution/map-success).
 *
 * <p>
 * Assertions verified:
 * <ul>
 * <li>[Req17/accepts-valid] The server returns HTTP 200 for valid positive mm-per-pixel
 * values (0.28 and 0.14).</li>
 * <li>[Req17/map-success] Interactive verification: maps rendered at mm-per-pixel=0.28
 * and mm-per-pixel=0.14 look visually different, confirming that the server correctly
 * interprets the parameter.</li>
 * </ul>
 */
public class MapSuccessTest extends CommonFixture {

	private static final String PARAM_MM_PER_PIXEL = "mm-per-pixel";

	private static final String REL_MAP = "http://www.opengis.net/def/rel/ogc/1.0/map";

	/**
	 * A.17 Abstract Test for Requirement /req/display-resolution/map-success.
	 *
	 * <p>
	 * Verifies that the server accepts valid mm-per-pixel values and returns HTTP 200,
	 * and that it correctly interprets the parameter by producing visually different maps
	 * at different mm-per-pixel values combined with a fixed scale denominator and
	 * dimensions.
	 * @param context the TestNG test context, used to access suite attributes
	 */
	@Test(description = "A.17 Abstract Test for Requirement /req/display-resolution/map-success: "
			+ "Verify that the server returns a successful map response for valid mm-per-pixel values.")
	public void verifyMapSuccess(ITestContext context) {
		List<String> errors = new ArrayList<>();

		String landingPageUrl = rootUri.toString();

		// Locate the dataset map endpoint
		String mapUrl = findMapUrl(landingPageUrl);
		if (mapUrl == null) {
			throw new SkipException("Could not find a dataset map endpoint (rel=ogc/1.0/map) from the landing page at "
					+ landingPageUrl + ". Skipping A.17 test.");
		}

		// --- Assertion 1: HTTP 200 for valid mm-per-pixel values [Req17/accepts-valid]
		// ---
		int status028 = init().param(PARAM_MM_PER_PIXEL, "0.28").when().request(Method.GET, mapUrl).getStatusCode();
		if (status028 != 200) {
			errors.add("[Req17/accepts-valid] Expected HTTP 200 for " + PARAM_MM_PER_PIXEL + "=0.28 but got HTTP "
					+ status028 + ".");
		}

		int status014 = init().param(PARAM_MM_PER_PIXEL, "0.14").when().request(Method.GET, mapUrl).getStatusCode();
		if (status014 != 200) {
			errors.add("[Req17/accepts-valid] Expected HTTP 200 for " + PARAM_MM_PER_PIXEL + "=0.14 but got HTTP "
					+ status014 + ".");
		}

		// --- Assertion 2: Interactive visual verification [Req17/map-success] ---
		// Two maps are shown with scale-denominator=40000000, width=1024, height=512 and
		// different mm-per-pixel values (0.28 vs 0.14). Halving mm-per-pixel halves the
		// computed bbox extent, so Map B should appear zoomed in to a smaller area.
		MapSuccessInteractiveTestResult interactiveResult = (MapSuccessInteractiveTestResult) context.getSuite()
			.getAttribute(SuiteAttribute.MAP_SUCCESS_INTERACTIVE_TEST_RESULT.getName());
		if (interactiveResult != null && interactiveResult.isEnabled()) {
			if (!interactiveResult.isMapSuccessCorrect()) {
				errors.add("[Req17/map-success] Interactive verification failed: the maps rendered at "
						+ "mm-per-pixel=0.28 and mm-per-pixel=0.14 (with scale-denominator=40000000, "
						+ "width=1024, height=512) were reported as NOT visually different. "
						+ "The server does not appear to correctly interpret the mm-per-pixel parameter.");
			}
		}

		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.17 verifyMapSuccess failed:\n" + String.join("\n", errors));
		}
	}

	// -------------------------------------------------------------------------
	// Private helpers
	// -------------------------------------------------------------------------

	/**
	 * Finds the dataset map URL from the landing page by following the rel=ogc/1.0/map
	 * link, or falls back to {landingPageUrl}/map.
	 * @param landingPageUrl the IUT landing page URL
	 * @return the map URL, or null if the landing page cannot be reached
	 */
	private String findMapUrl(String landingPageUrl) {
		try {
			Response response = init().when().request(Method.GET, landingPageUrl + "?f=json");
			if (response.getStatusCode() != 200) {
				return null;
			}
			JsonPath json = response.jsonPath();
			List<Map<String, Object>> links = json.getList("links");
			if (links != null) {
				for (Map<String, Object> link : links) {
					String rel = (String) link.get("rel");
					if (rel != null && matchesRelIgnoringScheme(rel, REL_MAP)) {
						String href = (String) link.get("href");
						if (href != null) {
							return resolveUrl(landingPageUrl, href);
						}
					}
				}
			}
		}
		catch (Exception e) {
			// fall through to default
		}
		String base = landingPageUrl.endsWith("/") ? landingPageUrl.substring(0, landingPageUrl.length() - 1)
				: landingPageUrl;
		return base + "/map";
	}

	private String resolveUrl(String baseUrl, String url) {
		if (url.startsWith("http://") || url.startsWith("https://")) {
			return url;
		}
		try {
			return java.net.URI.create(baseUrl).resolve(url).toString();
		}
		catch (Exception e) {
			return url;
		}
	}

	private boolean matchesRelIgnoringScheme(String actual, String expected) {
		return normalizeScheme(actual).equals(normalizeScheme(expected));
	}

	private String normalizeScheme(String rel) {
		if (rel.startsWith("https://")) {
			return "http://" + rel.substring("https://".length());
		}
		return rel;
	}

}
