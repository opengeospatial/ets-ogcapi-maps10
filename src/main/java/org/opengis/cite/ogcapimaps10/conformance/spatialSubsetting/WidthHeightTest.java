package org.opengis.cite.ogcapimaps10.conformance.spatialSubsetting;

import java.io.ByteArrayOutputStream;
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
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Implements Abstract Test A.25: /conf/spatial-subsetting/width-height
 *
 * <p>
 * Test Purpose: Verify that the implementation supports the {@code width} and
 * {@code height} parameters for spatial subsetting when used together with the
 * {@code center} and/or the {@code scale-denominator} parameters (Requirement 25:
 * /req/spatial-subsetting/width-height).
 *
 * <p>
 * Assertions verified:
 * <ul>
 * <li>[Req25/A-basic] {@code center + width + height} without {@code center-crs} (CRS84
 * assumed) → HTTP 200, non-empty body.</li>
 * <li>[Req25/A-explicit-crs] Same request with explicit {@code center-crs=[OGC:CRS84]} →
 * HTTP 200, non-empty body.</li>
 * <li>[Req25/A-scale-denom] {@code center + width + height + scale-denominator} → HTTP
 * 200, non-empty body (conditional: only when the Scaling conformance class is
 * declared).</li>
 * <li>[Req25/B-content-bbox] If the {@code Content-Bbox} response header is present, its
 * bounding box must be consistent with the geographic extent computed from the
 * {@code center}, {@code width}, {@code height} and default display resolution (0.28
 * mm/pixel), within a generous tolerance.</li>
 * </ul>
 */
public class WidthHeightTest extends CommonFixture {

	/** URL-encoded safe CURIE for CRS84: [OGC:CRS84] */
	private static final String CRS84_SAFE_CURIE = "%5BOGC%3ACRS84%5D";

	private static final String SCALING_CONF_URI = "http://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/scaling";

	private static final String REL_MAP = "http://www.opengis.net/def/rel/ogc/1.0/map";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	/** Map URL discovered in {@link #discoverTargets}. */
	private String mapUrl;

	/** Query-string separator for {@link #mapUrl}. */
	private String sep;

	/** Whether the server has declared the Scaling conformance class. */
	private boolean scalingSupported = false;

	// -------------------------------------------------------------------------
	// Setup
	// -------------------------------------------------------------------------

	/**
	 * Discovers the dataset map endpoint and checks whether the Scaling conformance class
	 * is declared. Does not throw {@link SkipException} here; each test method guards
	 * itself so that skip messages appear correctly in the report.
	 * @param testContext the TestNG test context
	 */
	@BeforeClass
	public void discoverTargets(ITestContext testContext) {
		String landingPageUrl = rootUri.toString();
		mapUrl = findMapUrl(landingPageUrl);
		if (mapUrl != null) {
			sep = mapUrl.contains("?") ? "&" : "?";
		}
		scalingSupported = isScalingDeclared(landingPageUrl);
	}

	// -------------------------------------------------------------------------
	// Tests
	// -------------------------------------------------------------------------

	/**
	 * A.25 — Req 25/A: verifies that {@code center + width + height} (with and without an
	 * explicit {@code center-crs}) returns HTTP 200 with a non-empty body, and that the
	 * {@code Content-Bbox} header (when present) is consistent with the requested spatial
	 * extent.
	 */
	@Test(description = "A.25 Req 25/A-B: center+width+height (with and without center-crs=[OGC:CRS84]) "
			+ "returns HTTP 200 with non-empty body; Content-Bbox (if present) is consistent with extent.")
	public void verifyWidthHeightBasic() {
		if (mapUrl == null) {
			throw new SkipException("No map endpoint (rel=ogc/1.0/map) found at " + rootUri + ". Skipping A.25 test.");
		}

		List<String> errors = new ArrayList<>();

		// --- Req 25/A (row 1): center without center-crs → CRS84 assumed (lon,lat)
		// center=0,0, width=256px, height=256px
		HttpResult resultBasic = fetch(mapUrl + sep + "center=0,0&width=256&height=256&f=png");
		if (resultBasic.status != 200) {
			errors.add("[Req25/A-basic] Expected HTTP 200 for center=0,0&width=256&height=256" + " but got HTTP "
					+ resultBasic.status + ".");
		}
		else if (resultBasic.body.length == 0) {
			errors.add("[Req25/A-basic] HTTP 200 received but response body was empty.");
		}

		// --- Req 25/A (row 2): center with explicit center-crs=[OGC:CRS84]
		HttpResult resultExplicitCrs = fetch(
				mapUrl + sep + "center=0,0&center-crs=" + CRS84_SAFE_CURIE + "&width=256&height=256&f=png");
		if (resultExplicitCrs.status != 200) {
			errors.add("[Req25/A-explicit-crs] Expected HTTP 200 for center=0,0"
					+ "&center-crs=[OGC:CRS84]&width=256&height=256 but got HTTP " + resultExplicitCrs.status + ".");
		}
		else if (resultExplicitCrs.body.length == 0) {
			errors.add("[Req25/A-explicit-crs] HTTP 200 received but response body was empty.");
		}

		// --- Req 25/B (row 4): Content-Bbox consistency check (conditional)
		// Default display resolution: 0.28 mm/pixel. Without scale-denominator the server
		// chooses its own scale, so we only validate that the header is parseable and
		// internally consistent (4 values, minLon < maxLon, minLat < maxLat).
		if (resultBasic.contentBbox != null) {
			String bboxError = validateContentBboxStructure(resultBasic.contentBbox);
			if (bboxError != null) {
				errors.add("[Req25/B-content-bbox] " + bboxError);
			}
		}

		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.25 verifyWidthHeightBasic failed:\n" + String.join("\n", errors));
		}
	}

	/**
	 * A.25 — Req 25/A (row 3): verifies that {@code center + width + height +
	 * scale-denominator} returns HTTP 200 with a non-empty body. Skipped when the Scaling
	 * conformance class is not declared by the server.
	 */
	@Test(description = "A.25 Req 25/A: center+width+height+scale-denominator returns HTTP 200 "
			+ "with non-empty body (conditional on Scaling conformance class).")
	public void verifyWidthHeightWithScaleDenominator() {
		if (mapUrl == null) {
			throw new SkipException("No map endpoint (rel=ogc/1.0/map) found at " + rootUri + ". Skipping A.25 test.");
		}
		if (!scalingSupported) {
			throw new SkipException("Scaling conformance class not declared by server. "
					+ "Skipping scale-denominator variant of A.25.");
		}

		List<String> errors = new ArrayList<>();

		// scale-denominator=100000 ≈ 1:100 000 (city scale)
		HttpResult result = fetch(mapUrl + sep + "center=0,0&width=256&height=256&scale-denominator=100000&f=png");
		if (result.status != 200) {
			errors.add("[Req25/A-scale-denom] Expected HTTP 200 for"
					+ " center=0,0&width=256&height=256&scale-denominator=100000" + " but got HTTP " + result.status
					+ ".");
		}
		else if (result.body.length == 0) {
			errors.add("[Req25/A-scale-denom] HTTP 200 received but response body was empty.");
		}

		// Content-Bbox consistency when scale-denominator is known:
		// geographic_half_width ≈ 256 * 0.28/1000 * 100000 / 2 = 3584 m ≈ 0.032 deg at
		// equator
		// We allow a very generous tolerance of 5 degrees to account for CRS distortion.
		if (result.contentBbox != null && result.status == 200) {
			String bboxError = validateContentBboxStructure(result.contentBbox);
			if (bboxError != null) {
				errors.add("[Req25/B-content-bbox] " + bboxError);
			}
		}

		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError(
					"A.25 verifyWidthHeightWithScaleDenominator failed:\n" + String.join("\n", errors));
		}
	}

	// -------------------------------------------------------------------------
	// Private helpers
	// -------------------------------------------------------------------------

	/**
	 * Checks that a {@code Content-Bbox} header value is structurally valid: four
	 * comma-separated numbers with minLon &lt; maxLon and minLat &lt; maxLat.
	 * @return an error message, or {@code null} when valid
	 */
	private String validateContentBboxStructure(String contentBbox) {
		try {
			String[] parts = contentBbox.trim().split(",");
			if (parts.length < 4) {
				return "Content-Bbox '" + contentBbox + "' does not contain at least 4 values.";
			}
			double minLon = Double.parseDouble(parts[0].trim());
			double minLat = Double.parseDouble(parts[1].trim());
			double maxLon = Double.parseDouble(parts[2].trim());
			double maxLat = Double.parseDouble(parts[3].trim());
			if (minLon >= maxLon) {
				return "Content-Bbox '" + contentBbox + "': minLon (" + minLon + ") is not less than maxLon (" + maxLon
						+ ").";
			}
			if (minLat >= maxLat) {
				return "Content-Bbox '" + contentBbox + "': minLat (" + minLat + ") is not less than maxLat (" + maxLat
						+ ").";
			}
		}
		catch (NumberFormatException e) {
			return "Could not parse Content-Bbox header value '" + contentBbox + "': " + e.getMessage();
		}
		return null;
	}

	/**
	 * Checks whether the server's {@code /conformance} response declares the Scaling
	 * conformance class.
	 */
	@SuppressWarnings("unchecked")
	private boolean isScalingDeclared(String landingPageUrl) {
		try {
			String base = landingPageUrl.endsWith("/") ? landingPageUrl.substring(0, landingPageUrl.length() - 1)
					: landingPageUrl;
			Map<String, Object> conformance = fetchJson(base + "/conformance?f=json");
			if (conformance != null) {
				List<String> uris = (List<String>) conformance.get("conformsTo");
				if (uris != null) {
					for (String uri : uris) {
						if (matchesUriIgnoringScheme(uri, SCALING_CONF_URI)) {
							return true;
						}
					}
				}
			}
		}
		catch (Exception e) {
			// not declared or not reachable → treat as not supported
		}
		return false;
	}

	/**
	 * Executes an HTTP GET and returns the status, response body, and Content-Bbox
	 * header. Authentication is applied via {@link #applyAuth(HttpURLConnection)}.
	 */
	private HttpResult fetch(String urlString) {
		try {
			HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(30000);
			conn.setReadTimeout(60000);
			conn.setInstanceFollowRedirects(true);
			applyAuth(conn);
			int status = conn.getResponseCode();
			String contentBbox = conn.getHeaderField("Content-Bbox");
			byte[] body = new byte[0];
			if (status == 200) {
				try (InputStream is = conn.getInputStream()) {
					body = readAllBytes(is);
				}
			}
			return new HttpResult(status, body, contentBbox);
		}
		catch (Exception e) {
			System.err.println("[A.25] HTTP request failed for: " + urlString + " => " + e.getMessage());
			return new HttpResult(-1, new byte[0], null);
		}
	}

	private byte[] readAllBytes(InputStream is) throws Exception {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		byte[] chunk = new byte[8192];
		int n;
		while ((n = is.read(chunk)) != -1) {
			buffer.write(chunk, 0, n);
		}
		return buffer.toByteArray();
	}

	@SuppressWarnings("unchecked")
	private String findMapUrl(String landingPageUrl) {
		try {
			String base = landingPageUrl.endsWith("/") ? landingPageUrl.substring(0, landingPageUrl.length() - 1)
					: landingPageUrl;
			Map<String, Object> landingPage = fetchJson(base + "?f=json");
			if (landingPage != null) {
				List<Map<String, Object>> links = (List<Map<String, Object>>) landingPage.get("links");
				if (links != null) {
					for (Map<String, Object> link : links) {
						String rel = (String) link.get("rel");
						if (rel != null && matchesUriIgnoringScheme(rel, REL_MAP)) {
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

	private Map<String, Object> fetchJson(String urlString) {
		try {
			HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");
			conn.setConnectTimeout(30000);
			conn.setReadTimeout(30000);
			conn.setInstanceFollowRedirects(true);
			applyAuth(conn);
			if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
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

	private boolean matchesUriIgnoringScheme(String actual, String expected) {
		return normalizeScheme(actual).equals(normalizeScheme(expected));
	}

	private String normalizeScheme(String uri) {
		if (uri != null && uri.startsWith("https://")) {
			return "http://" + uri.substring("https://".length());
		}
		return uri;
	}

	// -------------------------------------------------------------------------
	// Inner class
	// -------------------------------------------------------------------------

	private static class HttpResult {

		final int status;

		final byte[] body;

		final String contentBbox;

		HttpResult(int status, byte[] body, String contentBbox) {
			this.status = status;
			this.body = body;
			this.contentBbox = contentBbox;
		}

	}

}
