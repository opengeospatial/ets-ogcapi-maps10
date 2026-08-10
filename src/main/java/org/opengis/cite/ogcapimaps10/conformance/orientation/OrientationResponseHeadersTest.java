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
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Implements Abstract Test A.38: /conf/orientation/response-headers
 *
 * <p>
 * Test Purpose: Verify that the server returns the correct HTTP response headers when the
 * {@code orientation} query parameter is used (Requirement 38:
 * /req/orientation/response-headers).
 *
 * <p>
 * Two test methods correspond to the two sub-requirements:
 * <ul>
 * <li>{@link #verifyReq38A} — the {@code Content-Orientation} header is present and its
 * decimal-degree value matches the requested orientation.</li>
 * <li>{@link #verifyReq38B} — the {@code Content-Bbox} header is present and contains
 * exactly four comma-separated numeric components (minX,minY,maxX,maxY) representing the
 * bounding box in the map output CRS prior to the orientation being applied.</li>
 * </ul>
 *
 * <p>
 * Both tests skip automatically when the server does not declare
 * {@code /conf/orientation} in its conformance declaration.
 */
public class OrientationResponseHeadersTest extends CommonFixture {

	private static final String PARAM_ORIENTATION = "orientation";

	private static final String HEADER_CONTENT_ORIENTATION = "Content-Orientation";

	private static final String HEADER_CONTENT_BBOX = "Content-Bbox";

	private static final String REL_MAP = "http://www.opengis.net/def/rel/ogc/1.0/map";

	private static final String CONF_ORIENTATION = "https://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/orientation";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private static final int TEST_ORIENTATION = 40;

	private String mapUrl;

	private String sep;

	private String skipReason;

	/**
	 * Discovers the dataset map endpoint once before all A.38 tests run. Sets
	 * {@link #skipReason} instead of throwing so that each {@code @Test} method throws
	 * its own {@link SkipException} with a visible reason in the TEAMENGINE result page.
	 */
	@BeforeClass(alwaysRun = true)
	public void discoverEndpoints() {
		String landingPageUrl = rootUri.toString();
		if (!hasConformanceClass(landingPageUrl, CONF_ORIENTATION)) {
			skipReason = "Server does not declare /conf/orientation support (" + CONF_ORIENTATION
					+ "). Skipping A.38 tests.";
			return;
		}
		mapUrl = findMapUrl(landingPageUrl);
		if (mapUrl == null) {
			skipReason = "Could not find a dataset map endpoint (rel=ogc/1.0/map) " + "from the landing page at "
					+ landingPageUrl + ". Skipping A.38 tests.";
			return;
		}
		sep = mapUrl.contains("?") ? "&" : "?";
	}

	// -------------------------------------------------------------------------
	// Test methods — one per sub-requirement
	// -------------------------------------------------------------------------

	/**
	 * A.38 Req 38/A: The server returns a {@code Content-Orientation} response header
	 * whose decimal-degree value matches the requested orientation.
	 *
	 * <p>
	 * Rows 1-2: header present; value is a valid decimal equal to
	 * {@value #TEST_ORIENTATION}.
	 */
	@Test(description = "A.38 Req 38/A: Response to a map request with the orientation "
			+ "parameter includes a Content-Orientation header whose value in decimal degrees "
			+ "corresponds to the orientation applied to the map.")
	public void verifyReq38A() {
		if (skipReason != null)
			throw new SkipException(skipReason);

		String requestUrl = mapUrl + sep + PARAM_ORIENTATION + "=" + TEST_ORIENTATION + "&f=png";
		List<String> errors = new ArrayList<>();

		Map<String, List<String>> headers = getResponseHeaders(requestUrl);
		if (headers == null) {
			errors.add("[Req38/A] HTTP request to " + requestUrl + " failed (connection error).");
		}
		else {
			String value = getHeaderValue(headers, HEADER_CONTENT_ORIENTATION);
			if (value == null) {
				errors.add("[Req38/A] Missing response header '" + HEADER_CONTENT_ORIENTATION + "' for request with "
						+ PARAM_ORIENTATION + "=" + TEST_ORIENTATION + ". "
						+ "The header is required by Req 38/A for all map requests that use the orientation parameter.");
			}
			else {
				try {
					double returned = Double.parseDouble(value.trim());
					if (Math.abs(returned - TEST_ORIENTATION) > 0.001) {
						errors.add("[Req38/A] " + HEADER_CONTENT_ORIENTATION + " value '" + value
								+ "' does not match the requested " + PARAM_ORIENTATION + "=" + TEST_ORIENTATION
								+ " (expected " + TEST_ORIENTATION + ", got " + returned + ").");
					}
				}
				catch (NumberFormatException e) {
					errors.add("[Req38/A] " + HEADER_CONTENT_ORIENTATION + " value '" + value
							+ "' is not a valid decimal degree number.");
				}
			}
		}

		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.38 verifyReq38A failed:\n" + String.join("\n", errors));
		}
	}

	/**
	 * A.38 Req 38/B: The server returns a {@code Content-Bbox} response header whose
	 * value is the bounding box in the map output CRS before the orientation is applied.
	 *
	 * <p>
	 * Rows 3-4: header present; value contains exactly four comma-separated numeric
	 * components (minX,minY,maxX,maxY).
	 */
	@Test(description = "A.38 Req 38/B: Response to a map request with the orientation "
			+ "parameter includes a Content-Bbox header reflecting the bounding box in the "
			+ "map output CRS prior to the orientation being applied.")
	public void verifyReq38B() {
		if (skipReason != null)
			throw new SkipException(skipReason);

		String requestUrl = mapUrl + sep + PARAM_ORIENTATION + "=" + TEST_ORIENTATION + "&f=png";
		List<String> errors = new ArrayList<>();

		Map<String, List<String>> headers = getResponseHeaders(requestUrl);
		if (headers == null) {
			errors.add("[Req38/B] HTTP request to " + requestUrl + " failed (connection error).");
		}
		else {
			String value = getHeaderValue(headers, HEADER_CONTENT_BBOX);
			if (value == null) {
				errors.add("[Req38/B] Missing response header '" + HEADER_CONTENT_BBOX + "' for request with "
						+ PARAM_ORIENTATION + "=" + TEST_ORIENTATION + ". "
						+ "The header must reflect the bounding box in the map output CRS "
						+ "prior to the orientation being applied (Req 38/B).");
			}
			else {
				String[] parts = value.trim().split(",");
				if (parts.length != 4) {
					errors.add("[Req38/B] " + HEADER_CONTENT_BBOX + " value '" + value
							+ "' must contain exactly 4 comma-separated components " + "(minX,minY,maxX,maxY), found "
							+ parts.length + ".");
				}
				else {
					for (int i = 0; i < 4; i++) {
						try {
							Double.parseDouble(parts[i].trim());
						}
						catch (NumberFormatException e) {
							errors.add("[Req38/B] " + HEADER_CONTENT_BBOX + " component[" + i + "] '" + parts[i].trim()
									+ "' is not a valid number.");
						}
					}
				}
			}
		}

		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.38 verifyReq38B failed:\n" + String.join("\n", errors));
		}
	}

	// -------------------------------------------------------------------------
	// Private helpers
	// -------------------------------------------------------------------------

	private Map<String, List<String>> getResponseHeaders(String urlString) {
		try {
			URL url = URI.create(urlString).toURL();
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(15000);
			conn.setReadTimeout(15000);
			applyAuth(conn);
			conn.connect();
			conn.getResponseCode();
			return conn.getHeaderFields();
		}
		catch (Exception e) {
			return null;
		}
	}

	private String getHeaderValue(Map<String, List<String>> headers, String headerName) {
		for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
			if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(headerName)) {
				List<String> values = entry.getValue();
				if (values != null && !values.isEmpty()) {
					return values.get(0);
				}
			}
		}
		return null;
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

}
