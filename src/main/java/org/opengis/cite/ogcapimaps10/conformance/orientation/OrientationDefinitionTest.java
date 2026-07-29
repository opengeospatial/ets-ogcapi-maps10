package org.opengis.cite.ogcapimaps10.conformance.orientation;

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
import org.opengis.cite.ogcapimaps10.domain.OrientationInteractiveTestResult;
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Implements Abstract Test A.37: /conf/orientation/orientation
 *
 * <p>
 * Test Purpose: Verify that the implementation supports the {@code orientation} query
 * parameter correctly for map requests (Requirement 37: /req/orientation/orientation).
 *
 * <p>
 * Four test methods correspond to the four sub-requirements:
 * <ul>
 * <li>{@link #verifyReq37A} — valid orientation values return HTTP 200; non-numeric value
 * returns HTTP 4xx; rotation direction is counterclockwise (interactive).</li>
 * <li>{@link #verifyReq37B} — omitting orientation returns HTTP 200; default is north-up
 * / identical to orientation=0 (interactive).</li>
 * <li>{@link #verifyReq37C} — rotation pivot is the center of the map extent (no bbox)
 * and the center of the bbox when subsetting is used (interactive only).</li>
 * <li>{@link #verifyReq37D} — combining orientation with bbox returns HTTP 200; no empty
 * corners in output (conditional on spatial subsetting; interactive).</li>
 * </ul>
 */
public class OrientationDefinitionTest extends CommonFixture {

	private static final String PARAM_ORIENTATION = "orientation";

	private static final String REL_MAP = "http://www.opengis.net/def/rel/ogc/1.0/map";

	private static final String CONF_ORIENTATION = "https://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/orientation";

	private static final String CONF_SPATIAL_SUBSETTING = "https://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/spatial-subsetting";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private String landingPageUrl;

	private String mapUrl;

	private String sep;

	private boolean hasSubsetting;

	private String skipReason;

	/**
	 * Discovers the dataset map endpoint and spatial-subsetting support once before all
	 * A.37 tests run. Sets {@link #skipReason} instead of throwing so that each
	 * {@code @Test} method throws its own {@link SkipException} with a visible reason in
	 * the TEAMENGINE result page.
	 */
	@BeforeClass(alwaysRun = true)
	public void discoverEndpoints() {
		landingPageUrl = rootUri.toString();
		if (!hasConformanceClass(landingPageUrl, CONF_ORIENTATION)) {
			skipReason = "Server does not declare /conf/orientation support (" + CONF_ORIENTATION
					+ "). Skipping A.37 tests.";
			return;
		}
		mapUrl = findMapUrl(landingPageUrl);
		if (mapUrl == null) {
			skipReason = "Could not find a dataset map endpoint (rel=ogc/1.0/map) " + "from the landing page at "
					+ landingPageUrl + ". Skipping A.37 tests.";
			return;
		}
		sep = mapUrl.contains("?") ? "&" : "?";
		hasSubsetting = hasConformanceClass(landingPageUrl, CONF_SPATIAL_SUBSETTING);
	}

	// -------------------------------------------------------------------------
	// Test methods — one per requirement
	// -------------------------------------------------------------------------

	/**
	 * A.37 Req 37/A: The server accepts valid orientation values and rejects non-numeric
	 * ones.
	 *
	 * <p>
	 * Automated assertions (rows 1-4): orientation=0, 45, 90, 270 return HTTP 200;
	 * orientation=abc returns HTTP 4xx.
	 *
	 * <p>
	 * Interactive assertion (row 5): Map B (orientation=45) appears rotated 45 degrees
	 * counterclockwise relative to Map A (default north-up).
	 * @param context the TestNG test context
	 */
	@Test(description = "A.37 Req 37/A: Valid orientation values return HTTP 200; "
			+ "non-numeric orientation returns HTTP 4xx; "
			+ "map content is rotated counterclockwise by the specified degrees (interactive).")
	public void verifyReq37A(ITestContext context) {
		if (skipReason != null)
			throw new SkipException(skipReason);
		List<String> errors = new ArrayList<>();

		for (int degrees : new int[] { 0, 45, 90, 270 }) {
			int status = getStatusRaw(mapUrl + sep + PARAM_ORIENTATION + "=" + degrees + "&f=png");
			if (status != 200) {
				errors.add("[Req37/A] Expected HTTP 200 for " + PARAM_ORIENTATION + "=" + degrees + " but got HTTP "
						+ status + ".");
			}
		}

		int statusInvalid = getStatusRaw(mapUrl + sep + PARAM_ORIENTATION + "=abc&f=png");
		if (!is4xx(statusInvalid)) {
			errors.add("[Req37/A] Expected HTTP 4xx for " + PARAM_ORIENTATION + "=abc (non-numeric) but got HTTP "
					+ statusInvalid + ".");
		}

		OrientationInteractiveTestResult ir = getInteractiveResult(context);
		if (ir != null && ir.isEnabled() && !ir.isRow5RotationCcw()) {
			errors.add("[Req37/A] Interactive: Map B (" + PARAM_ORIENTATION
					+ "=45) was reported as NOT rotated 45 degrees counterclockwise relative to Map A (default north-up).");
		}

		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.37 verifyReq37A failed:\n" + String.join("\n", errors));
		}
	}

	/**
	 * A.37 Req 37/B: Omitting the orientation parameter is valid and defaults to
	 * north-up.
	 *
	 * <p>
	 * Automated assertion (row 6): a request without orientation returns HTTP 200.
	 *
	 * <p>
	 * Interactive assertion (row 7): the map with no orientation parameter appears
	 * visually identical to orientation=0.
	 * @param context the TestNG test context
	 */
	@Test(description = "A.37 Req 37/B: Omitting orientation returns HTTP 200; "
			+ "default output is north-up and visually identical to orientation=0 (interactive).")
	public void verifyReq37B(ITestContext context) {
		if (skipReason != null)
			throw new SkipException(skipReason);
		List<String> errors = new ArrayList<>();

		int statusNoParam = getStatusRaw(mapUrl + sep + "f=png");
		if (statusNoParam != 200) {
			errors.add("[Req37/B] Expected HTTP 200 when " + PARAM_ORIENTATION + " is omitted but got HTTP "
					+ statusNoParam + ".");
		}

		OrientationInteractiveTestResult ir = getInteractiveResult(context);
		if (ir != null && ir.isEnabled() && !ir.isRow7DefaultEqualsZero()) {
			errors.add("[Req37/B] Interactive: The map with no " + PARAM_ORIENTATION
					+ " parameter was reported as NOT visually identical to " + PARAM_ORIENTATION + "=0.");
		}

		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.37 verifyReq37B failed:\n" + String.join("\n", errors));
		}
	}

	/**
	 * A.37 Req 37/C: The rotation pivot is the center of the map extent (no bbox) or the
	 * geographic center of the bbox when subsetting is used.
	 *
	 * <p>
	 * Interactive-only test — skipped if interactive orientation tests were not enabled
	 * by the tester.
	 *
	 * <p>
	 * Assertion (row 8): center feature of the map remains at the center of the output
	 * after rotation (no bbox).
	 *
	 * <p>
	 * Assertion (row 9, conditional): center of the bbox remains at the center of the
	 * output after rotation with bbox (only if spatial subsetting is supported).
	 * @param context the TestNG test context
	 */
	@Test(description = "A.37 Req 37/C: Rotation pivot is center of map extent (no bbox) "
			+ "and center of bbox when subsetting is used (interactive).")
	public void verifyReq37C(ITestContext context) {
		if (skipReason != null)
			throw new SkipException(skipReason);
		OrientationInteractiveTestResult ir = getInteractiveResult(context);
		if (ir == null || !ir.isEnabled()) {
			throw new SkipException("Interactive orientation tests not enabled. Skipping Req 37/C pivot checks.");
		}

		List<String> errors = new ArrayList<>();

		if (!ir.isRow8PivotCenter()) {
			errors.add("[Req37/C] Interactive: The rotation pivot for " + PARAM_ORIENTATION
					+ "=45 (no bbox) was reported as NOT at the center of the map extent.");
		}

		if (hasSubsetting && !ir.isRow9PivotBboxCenter()) {
			errors.add("[Req37/C] Interactive: The rotation pivot for " + PARAM_ORIENTATION
					+ "=45 with bbox was reported as NOT at the geographic center of the bbox.");
		}

		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.37 verifyReq37C failed:\n" + String.join("\n", errors));
		}
	}

	/**
	 * A.37 Req 37/D: Combining orientation with bbox returns HTTP 200 and produces no
	 * empty corners (clipping box is rotated in data space, not pixel space).
	 *
	 * <p>
	 * Skipped when spatial subsetting is not supported.
	 *
	 * <p>
	 * Automated assertion (row 10): orientation=45 combined with bbox returns HTTP 200.
	 *
	 * <p>
	 * Interactive assertion (row 11): the rotated-bbox map has no empty or transparent
	 * corners.
	 * @param context the TestNG test context
	 */
	@Test(description = "A.37 Req 37/D: orientation combined with bbox returns HTTP 200 "
			+ "and produces no empty corners in the output (conditional on spatial subsetting; interactive).")
	public void verifyReq37D(ITestContext context) {
		if (skipReason != null)
			throw new SkipException(skipReason);
		if (!hasSubsetting) {
			throw new SkipException("Server does not declare spatial-subsetting support. Skipping Req 37/D.");
		}

		List<String> errors = new ArrayList<>();

		int statusBbox = getStatusRaw(mapUrl + sep + PARAM_ORIENTATION + "=45&bbox=-180,-90,180,90&f=png");
		if (statusBbox != 200) {
			errors.add("[Req37/D] Expected HTTP 200 for " + PARAM_ORIENTATION
					+ "=45 combined with bbox=-180,-90,180,90 but got HTTP " + statusBbox + ".");
		}

		OrientationInteractiveTestResult ir = getInteractiveResult(context);
		if (ir != null && ir.isEnabled() && !ir.isRow11NoEmptyCorners()) {
			errors.add("[Req37/D] Interactive: The map for " + PARAM_ORIENTATION
					+ "=45 with bbox was reported as having empty or transparent corners.");
		}

		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.37 verifyReq37D failed:\n" + String.join("\n", errors));
		}
	}

	// -------------------------------------------------------------------------
	// Private helpers
	// -------------------------------------------------------------------------

	private OrientationInteractiveTestResult getInteractiveResult(ITestContext context) {
		return (OrientationInteractiveTestResult) context.getSuite()
			.getAttribute(SuiteAttribute.ORIENTATION_INTERACTIVE_TEST_RESULT.getName());
	}

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
			applyAuth(conn);
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
			applyAuth(conn);
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
