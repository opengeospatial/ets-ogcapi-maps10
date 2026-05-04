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
 * Implements Abstract Test A.24: /conf/spatial-subsetting/center-definition
 *
 * <p>
 * Test Purpose: Verify that the implementation supports the {@code center} parameter
 * correctly (Requirement 24: /req/spatial-subsetting/center-definition).
 *
 * <p>
 * Assertions verified:
 * <ul>
 * <li>[Req24/A-basic] The server returns HTTP 200 with a non-empty body for a valid
 * {@code center} request without an explicit {@code center-crs} (CRS84 assumed, axis
 * order longitude,latitude).</li>
 * <li>[Req24/A-explicit-crs] Same center coordinates with an explicit
 * {@code center-crs=[OGC:CRS84]} → HTTP 200, non-empty body.</li>
 * <li>[Req24/B-conflict-bbox] {@code center} used together with {@code bbox} → HTTP 4xx
 * client error.</li>
 * <li>[Req24/B-conflict-subset] {@code center} used together with {@code subset}
 * including spatial axes (Lat, Lon) → HTTP 4xx client error.</li>
 * </ul>
 */
public class CenterDefinitionTest extends CommonFixture {

	/** URL-encoded safe CURIE for CRS84: [OGC:CRS84] */
	private static final String CRS84_SAFE_CURIE = "%5BOGC%3ACRS84%5D";

	private static final String REL_MAP = "http://www.opengis.net/def/rel/ogc/1.0/map";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	/** Map URL discovered in {@link #discoverTargets}. */
	private String mapUrl;

	/** Query-string separator for {@link #mapUrl}. */
	private String sep;

	// -------------------------------------------------------------------------
	// Setup
	// -------------------------------------------------------------------------

	/**
	 * Discovers the dataset map endpoint. Does not throw {@link SkipException} here; each
	 * test method guards itself so that skip messages appear correctly in the report.
	 * @param testContext the TestNG test context
	 */
	@BeforeClass
	public void discoverTargets(ITestContext testContext) {
		String landingPageUrl = rootUri.toString();
		mapUrl = findMapUrl(landingPageUrl);
		if (mapUrl != null) {
			sep = mapUrl.contains("?") ? "&" : "?";
		}
	}

	// -------------------------------------------------------------------------
	// Tests
	// -------------------------------------------------------------------------

	/**
	 * A.24 — Req 24/A: verifies that the server accepts a {@code center} parameter with
	 * CRS84 coordinates (with and without an explicit {@code center-crs}) and returns
	 * HTTP 200 with a non-empty body.
	 */
	@Test(description = "A.24 Req 24/A: center parameter (with and without center-crs=[OGC:CRS84]) "
			+ "returns HTTP 200 with non-empty body.")
	public void verifyCenterBasic() {
		if (mapUrl == null) {
			throw new SkipException("No map endpoint (rel=ogc/1.0/map) found at " + rootUri + ". Skipping A.24 test.");
		}

		List<String> errors = new ArrayList<>();

		// --- Req 24/A (row 1): center without center-crs → CRS84 assumed (lon,lat order)
		// center=0,0 (longitude=0, latitude=0)
		HttpResult resultBasic = fetch(mapUrl + sep + "center=0,0&f=png");
		if (resultBasic.status != 200) {
			errors.add("[Req24/A-basic] Expected HTTP 200 for center=0,0 (no center-crs) but got HTTP "
					+ resultBasic.status + ".");
		}
		else if (resultBasic.body.length == 0) {
			errors.add("[Req24/A-basic] HTTP 200 received but response body was empty.");
		}

		// --- Req 24/A (row 2): center with explicit center-crs=[OGC:CRS84]
		HttpResult resultExplicitCrs = fetch(mapUrl + sep + "center=0,0&center-crs=" + CRS84_SAFE_CURIE + "&f=png");
		if (resultExplicitCrs.status != 200) {
			errors.add("[Req24/A-explicit-crs] Expected HTTP 200 for center=0,0&center-crs=[OGC:CRS84]"
					+ " but got HTTP " + resultExplicitCrs.status + ".");
		}
		else if (resultExplicitCrs.body.length == 0) {
			errors.add("[Req24/A-explicit-crs] HTTP 200 received but response body was empty.");
		}

		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.24 verifyCenterBasic failed:\n" + String.join("\n", errors));
		}
	}

	/**
	 * A.24 — Req 24/B: verifies that the server returns a 4xx client error when
	 * {@code center} is combined with {@code bbox} or with a {@code subset} parameter
	 * that includes spatial axes.
	 */
	@Test(description = "A.24 Req 24/B: center combined with bbox or spatial subset returns HTTP 4xx.")
	public void verifyCenterConflict() {
		if (mapUrl == null) {
			throw new SkipException("No map endpoint (rel=ogc/1.0/map) found at " + rootUri + ". Skipping A.24 test.");
		}

		List<String> errors = new ArrayList<>();

		// --- Req 24/B (row 3): center + bbox → 4xx
		// bbox=-180,-90,180,90 (full extent), center=0,0
		HttpResult resultBbox = fetch(mapUrl + sep + "center=0,0&bbox=-180,-90,180,90&f=png");
		if (resultBbox.status < 400 || resultBbox.status >= 500) {
			errors.add("[Req24/B-conflict-bbox] Expected HTTP 4xx for center=0,0&bbox=-180,-90,180,90"
					+ " but got HTTP " + resultBbox.status + ".");
		}

		// --- Req 24/B (row 4): center + subset (spatial axes Lat+Lon) → 4xx
		// subset=Lat(-10:10),Lon(-10:10) URL-encoded
		HttpResult resultSubset = fetch(mapUrl + sep + "center=0,0&subset=Lat%28-10%3A10%29%2CLon%28-10%3A10%29&f=png");
		if (resultSubset.status < 400 || resultSubset.status >= 500) {
			errors.add("[Req24/B-conflict-subset] Expected HTTP 4xx for center=0,0"
					+ "&subset=Lat(-10:10),Lon(-10:10) but got HTTP " + resultSubset.status + ".");
		}

		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.24 verifyCenterConflict failed:\n" + String.join("\n", errors));
		}
	}

	// -------------------------------------------------------------------------
	// Private helpers
	// -------------------------------------------------------------------------

	/**
	 * Executes an HTTP GET and returns the status and response body. Authentication is
	 * applied via {@link #applyAuth(HttpURLConnection)}.
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
			byte[] body = new byte[0];
			if (status == 200) {
				try (InputStream is = conn.getInputStream()) {
					body = readAllBytes(is);
				}
			}
			return new HttpResult(status, body);
		}
		catch (Exception e) {
			System.err.println("[A.24] HTTP request failed for: " + urlString + " => " + e.getMessage());
			return new HttpResult(-1, new byte[0]);
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

	private boolean matchesRelIgnoringScheme(String actual, String expected) {
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

		HttpResult(int status, byte[] body) {
			this.status = status;
			this.body = body;
		}

	}

}
