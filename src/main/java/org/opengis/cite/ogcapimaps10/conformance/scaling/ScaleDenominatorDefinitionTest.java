package org.opengis.cite.ogcapimaps10.conformance.scaling;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.opengis.cite.ogcapimaps10.conformance.CommonFixture;
import org.opengis.cite.ogcapimaps10.conformance.SuiteAttribute;
import org.opengis.cite.ogcapimaps10.domain.ScalingScaleDenominatorInteractiveTestResult;
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * Implements Abstract Test A.15: /conf/scaling/scale-denominator-definition
 *
 * <p>
 * Test Purpose: Verify that the implementation supports the scale-denominator parameter
 * correctly for map requests (Requirement 15: /req/scaling/scale-denominator-definition).
 *
 * <p>
 * Assertions verified:
 * <ul>
 * <li>[Req15/scale-denom-accepted] The server returns HTTP 200 for a map request
 * combining scale-denominator with explicit width and height.</li>
 * <li>[Req15/scale-denom-width-height-no-subsetting] If Subsetting is not supported, the
 * server returns HTTP 4xx when scale-denominator is combined with width and/or
 * height.</li>
 * <li>[Req15/scale-denom-bbox-conflict] If Subsetting is supported, the server returns
 * HTTP 4xx when scale-denominator, width/height, and bbox are all combined.</li>
 * <li>[Req15/omit-scale-denom] When scale-denominator is omitted, the server returns HTTP
 * 200.</li>
 * <li>[Req15/scale-interpretation] Interactive verification that the server correctly
 * interprets the scale-denominator value (Req 15/B and C) and, when Subsetting is
 * supported, derives an appropriate bounding box from it (Req 15/G).</li>
 * </ul>
 */
public class ScaleDenominatorDefinitionTest extends CommonFixture {

	private static final String PARAM_WIDTH = "width";

	private static final String PARAM_HEIGHT = "height";

	private static final String PARAM_SCALE_DENOMINATOR = "scale-denominator";

	private static final String REL_MAP = "http://www.opengis.net/def/rel/ogc/1.0/map";

	private static final String CONF_SPATIAL_SUBSETTING = "https://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/spatial-subsetting";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	/**
	 * A.15 Abstract Test for Requirement /req/scaling/scale-denominator-definition.
	 *
	 * <p>
	 * Verifies that the server accepts the scale-denominator parameter, rejects forbidden
	 * parameter combinations, returns HTTP 200 when scale-denominator is omitted, and
	 * (interactively) correctly interprets the scale and derives spatial extents from it.
	 * @param context the TestNG test context, used to access suite attributes
	 */
	@Test(description = "A.15 Abstract Test for Requirement /req/scaling/scale-denominator-definition: "
			+ "Verify that the implementation supports the scale-denominator parameter correctly for map requests.")
	public void verifyScaleDenominatorDefinition(ITestContext context) {
		List<String> errors = new ArrayList<>();

		String landingPageUrl = rootUri.toString();

		// Locate the dataset map endpoint
		String mapUrl = findMapUrl(landingPageUrl);
		if (mapUrl == null) {
			throw new SkipException("Could not find a dataset map endpoint (rel=ogc/1.0/map) from the landing page at "
					+ landingPageUrl + ". Skipping A.15 test.");
		}
		String sep = mapUrl.contains("?") ? "&" : "?";

		// --- Assertion B (automated): scale-denominator accepted
		// [Req15/scale-denom-accepted] ---
		// Request a map combining scale-denominator with explicit dimensions and verify
		// HTTP 200.
		int statusB = getStatusRaw(mapUrl + sep + "scale-denominator=40000000&width=1024&height=512&f=png");
		if (statusB != 200) {
			errors.add("[Req15/scale-denom-accepted] Expected HTTP 200 for " + PARAM_SCALE_DENOMINATOR
					+ "=40000000&width=1024&height=512 but got HTTP " + statusB + ".");
		}

		// --- Assertions D & E: scale-denominator conflict checks ---
		boolean hasSubsetting = hasConformanceClass(landingPageUrl, CONF_SPATIAL_SUBSETTING);

		// Assertion D: scale-denom + width -> 4xx (only if Subsetting NOT supported)
		// [Req15/scale-denom-width-height-no-subsetting]
		if (!hasSubsetting) {
			int statusD1 = getStatusRaw(mapUrl + sep + "scale-denominator=40000000&" + PARAM_WIDTH + "=800&f=png");
			if (!is4xx(statusD1)) {
				errors.add("[Req15/scale-denom-width-height-no-subsetting] Expected HTTP 4xx when combining "
						+ PARAM_SCALE_DENOMINATOR + " and " + PARAM_WIDTH
						+ " (Spatial Subsetting not declared) but got HTTP " + statusD1 + ".");
			}
			int statusD2 = getStatusRaw(mapUrl + sep + "scale-denominator=40000000&" + PARAM_HEIGHT + "=400&f=png");
			if (!is4xx(statusD2)) {
				errors.add("[Req15/scale-denom-width-height-no-subsetting] Expected HTTP 4xx when combining "
						+ PARAM_SCALE_DENOMINATOR + " and " + PARAM_HEIGHT
						+ " (Spatial Subsetting not declared) but got HTTP " + statusD2 + ".");
			}
			int statusD3 = getStatusRaw(
					mapUrl + sep + "scale-denominator=40000000&" + PARAM_WIDTH + "=800&" + PARAM_HEIGHT + "=400&f=png");
			if (!is4xx(statusD3)) {
				errors.add("[Req15/scale-denom-width-height-no-subsetting] Expected HTTP 4xx when combining "
						+ PARAM_SCALE_DENOMINATOR + ", " + PARAM_WIDTH + ", and " + PARAM_HEIGHT
						+ " (Spatial Subsetting not declared) but got HTTP " + statusD3 + ".");
			}
		}

		// Assertion E: scale-denom + width/height + bbox -> 4xx (only if Subsetting
		// supported)
		// [Req15/scale-denom-bbox-conflict]
		if (hasSubsetting) {
			String rawUrlE = mapUrl + sep
					+ "scale-denominator=40000000&width=800&height=400&bbox=-180,-90,180,90&f=png";
			int statusE = getStatusRaw(rawUrlE);
			if (!is4xx(statusE)) {
				errors
					.add("[Req15/scale-denom-bbox-conflict] Expected HTTP 4xx when combining " + PARAM_SCALE_DENOMINATOR
							+ ", " + PARAM_WIDTH + "/" + PARAM_HEIGHT + ", and bbox but got HTTP " + statusE + ".");
			}
		}

		// --- Assertion F: omit scale-denominator -> HTTP 200 [Req15/omit-scale-denom]
		// ---
		int statusF = getStatusRaw(mapUrl + sep + "f=png");
		if (statusF != 200) {
			errors.add("[Req15/omit-scale-denom] Expected HTTP 200 when " + PARAM_SCALE_DENOMINATOR
					+ " is omitted but got HTTP " + statusF + ".");
		}

		// --- Assertions B/C/G (interactive): scale interpretation
		// [Req15/scale-interpretation] ---
		ScalingScaleDenominatorInteractiveTestResult interactiveResult = (ScalingScaleDenominatorInteractiveTestResult) context
			.getSuite()
			.getAttribute(SuiteAttribute.SCALING_SCALE_DENOMINATOR_INTERACTIVE_TEST_RESULT.getName());
		if (interactiveResult != null && interactiveResult.isEnabled()) {
			if (!interactiveResult.isScaleDenominatorAppropriate()) {
				errors.add("[Req15/scale-interpretation] Interactive verification failed: the map rendered with "
						+ PARAM_SCALE_DENOMINATOR
						+ "=40000000 was reported as NOT correctly reflecting the requested scale. "
						+ "The server does not appear to correctly interpret the scale-denominator "
						+ "parameter per Req 15/B and C (physicalMetersPerPixel = "
						+ "(mm-per-pixel / 1000) * scale-denominator).");
			}
		}

		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.15 verifyScaleDenominatorDefinition failed:\n" + String.join("\n", errors));
		}
	}

	// -------------------------------------------------------------------------
	// Private helpers
	// -------------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	private String findMapUrl(String landingPageUrl) {
		try {
			Map<String, Object> landingPage = fetchJson(landingPageUrl + "?f=json");
			if (landingPage != null) {
				List<Map<String, Object>> links = (List<Map<String, Object>>) landingPage.get("links");
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
		}
		catch (Exception e) {
			// fall through to default
		}
		String base = landingPageUrl.endsWith("/") ? landingPageUrl.substring(0, landingPageUrl.length() - 1)
				: landingPageUrl;
		return base + "/map";
	}

	@SuppressWarnings("unchecked")
	private boolean hasConformanceClass(String landingPageUrl, String classUri) {
		try {
			Map<String, Object> conf = fetchJson(landingPageUrl + "/conformance?f=json");
			if (conf == null) {
				return false;
			}
			List<String> classes = (List<String>) conf.get("conformsTo");
			if (classes == null) {
				return false;
			}
			String normalizedTarget = normalizeScheme(classUri);
			return classes.stream().anyMatch(c -> normalizeScheme(c).equals(normalizedTarget));
		}
		catch (Exception e) {
			return false;
		}
	}

	private Map<String, Object> fetchJson(String urlString) {
		try {
			URL url = URI.create(urlString).toURL();
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");
			conn.setConnectTimeout(10000);
			conn.setReadTimeout(10000);
			if (conn.getResponseCode() == 200) {
				try (InputStream is = conn.getInputStream()) {
					return OBJECT_MAPPER.readValue(is, new TypeReference<Map<String, Object>>() {
					});
				}
			}
		}
		catch (Exception e) {
			// return null
		}
		return null;
	}

	private int getStatusRaw(String urlString) {
		try {
			URL url = URI.create(urlString).toURL();
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(10000);
			conn.setReadTimeout(10000);
			return conn.getResponseCode();
		}
		catch (Exception e) {
			return -1;
		}
	}

	private String resolveUrl(String baseUrl, String url) {
		if (url.startsWith("http://") || url.startsWith("https://")) {
			return url;
		}
		try {
			return URI.create(baseUrl).resolve(url).toString();
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

	private boolean is4xx(int status) {
		return status >= 400 && status <= 499;
	}

}
