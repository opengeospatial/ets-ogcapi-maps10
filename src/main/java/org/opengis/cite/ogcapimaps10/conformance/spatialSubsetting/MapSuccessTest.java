package org.opengis.cite.ogcapimaps10.conformance.spatialSubsetting;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
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
 * Implements Abstract Test A.26: /conf/spatial-subsetting/map-success
 *
 * <p>
 * Test Purpose: Verify that the implementation responds correctly to map requests using
 * subsetting parameters ({@code bbox}, {@code subset} or {@code center}) (Requirement 26:
 * /req/spatial-subsetting/map-success).
 *
 * <p>
 * Assertions verified:
 * <ul>
 * <li>[Req26/A-bbox] {@code bbox} request → HTTP 200, non-empty body.</li>
 * <li>[Req26/A-subset] {@code subset} request → HTTP 200, non-empty body.</li>
 * <li>[Req26/A-center] {@code center + width + height} request → HTTP 200, non-empty
 * body.</li>
 * <li>[Req26/A-content-bbox-bbox] If {@code Content-Bbox} is present for the bbox
 * request, it must intersect or fall within the requested bbox bounds.</li>
 * <li>[Req26/A-content-bbox-subset] If {@code Content-Bbox} is present for the subset
 * request, it must intersect or fall within the requested subset bounds.</li>
 * <li>[Req26/A-distinct-bbox] bbox response body must differ from the full-extent body —
 * proves the spatial filter was applied.</li>
 * <li>[Req26/A-distinct-subset] subset response body must differ from the full-extent
 * body — proves the spatial filter was applied.</li>
 * </ul>
 */
public class MapSuccessTest extends CommonFixture {

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
	 * A.26 — Req 26/A (bbox): verifies that a {@code bbox} request returns HTTP 200 with
	 * a non-empty body, that the {@code Content-Bbox} header (if present) falls within
	 * the requested bounds, and that the response body differs from a full-extent
	 * response, proving the spatial filter was applied.
	 */
	@Test(description = "A.26 Req 26/A (bbox): bbox request returns HTTP 200 with non-empty body; "
			+ "Content-Bbox (if present) within requested bounds; body differs from full-extent.")
	public void verifyMapSuccessBbox() {
		if (mapUrl == null) {
			throw new SkipException("No map endpoint (rel=ogc/1.0/map) found at " + rootUri + ". Skipping A.26 test.");
		}

		List<String> errors = new ArrayList<>();

		// --- Req 26/A (row 1): bbox=-10,-10,10,10 (small area around origin)
		HttpResult resultBbox = fetch(mapUrl + sep + "bbox=-10,-10,10,10&f=png");
		if (resultBbox.status != 200) {
			errors.add("[Req26/A-bbox] Expected HTTP 200 for bbox=-10,-10,10,10" + " but got HTTP " + resultBbox.status
					+ ".");
		}
		else if (resultBbox.body.length == 0) {
			errors.add("[Req26/A-bbox] HTTP 200 received but response body was empty.");
		}

		// --- Req 26/A (row 4): Content-Bbox must intersect or fall within requested bbox
		if (resultBbox.contentBbox != null) {
			String bboxError = validateContentBboxIntersects(resultBbox.contentBbox, -10.0, -10.0, 10.0, 10.0);
			if (bboxError != null) {
				errors.add("[Req26/A-content-bbox-bbox] " + bboxError);
			}
		}

		// --- Req 26/A (row 6): bbox response body must differ from full-extent body
		HttpResult resultFull = fetch(mapUrl + sep + "f=png");
		if (resultFull.status == 200 && resultBbox.status == 200) {
			if (Arrays.equals(resultFull.body, resultBbox.body)) {
				errors.add("[Req26/A-distinct-bbox] bbox response body is identical to the full-extent"
						+ " response body — the server does not appear to be applying the spatial filter.");
			}
		}

		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.26 verifyMapSuccessBbox failed:\n" + String.join("\n", errors));
		}
	}

	/**
	 * A.26 — Req 26/A (subset): verifies that a {@code subset} request returns HTTP 200
	 * with a non-empty body, that the {@code Content-Bbox} header (if present) falls
	 * within the requested bounds, and that the response body differs from a full-extent
	 * response, proving the spatial filter was applied.
	 */
	@Test(description = "A.26 Req 26/A (subset): subset request returns HTTP 200 with non-empty body; "
			+ "Content-Bbox (if present) within requested bounds; body differs from full-extent.")
	public void verifyMapSuccessSubset() {
		if (mapUrl == null) {
			throw new SkipException("No map endpoint (rel=ogc/1.0/map) found at " + rootUri + ". Skipping A.26 test.");
		}

		List<String> errors = new ArrayList<>();

		// --- Req 26/A (row 2): subset=Lat(-10:10),Lon(-10:10) URL-encoded
		HttpResult resultSubset = fetch(mapUrl + sep + "subset=Lat%28-10%3A10%29%2CLon%28-10%3A10%29&f=png");
		if (resultSubset.status != 200) {
			errors.add("[Req26/A-subset] Expected HTTP 200 for subset=Lat(-10:10),Lon(-10:10)" + " but got HTTP "
					+ resultSubset.status + ".");
		}
		else if (resultSubset.body.length == 0) {
			errors.add("[Req26/A-subset] HTTP 200 received but response body was empty.");
		}

		// --- Req 26/A (row 5): Content-Bbox must intersect or fall within requested
		// subset
		// bounds (CRS84 order: minLon, minLat, maxLon, maxLat)
		if (resultSubset.contentBbox != null) {
			String bboxError = validateContentBboxIntersects(resultSubset.contentBbox, -10.0, -10.0, 10.0, 10.0);
			if (bboxError != null) {
				errors.add("[Req26/A-content-bbox-subset] " + bboxError);
			}
		}

		// --- Req 26/A (row 7): subset response body must differ from full-extent body
		HttpResult resultFull = fetch(mapUrl + sep + "f=png");
		if (resultFull.status == 200 && resultSubset.status == 200) {
			if (Arrays.equals(resultFull.body, resultSubset.body)) {
				errors.add("[Req26/A-distinct-subset] subset response body is identical to the full-extent"
						+ " response body — the server does not appear to be applying the spatial filter.");
			}
		}

		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.26 verifyMapSuccessSubset failed:\n" + String.join("\n", errors));
		}
	}

	/**
	 * A.26 — Req 26/A (center): verifies that a {@code center + width + height} request
	 * returns HTTP 200 with a non-empty body.
	 */
	@Test(description = "A.26 Req 26/A (center): center+width+height request returns HTTP 200 "
			+ "with non-empty body.")
	public void verifyMapSuccessCenter() {
		if (mapUrl == null) {
			throw new SkipException("No map endpoint (rel=ogc/1.0/map) found at " + rootUri + ". Skipping A.26 test.");
		}

		List<String> errors = new ArrayList<>();

		// --- Req 26/A (row 3): center=0,0 (lon=0, lat=0), width=256px, height=256px
		HttpResult resultCenter = fetch(mapUrl + sep + "center=0,0&width=256&height=256&f=png");
		if (resultCenter.status != 200) {
			errors.add("[Req26/A-center] Expected HTTP 200 for center=0,0&width=256&height=256" + " but got HTTP "
					+ resultCenter.status + ".");
		}
		else if (resultCenter.body.length == 0) {
			errors.add("[Req26/A-center] HTTP 200 received but response body was empty.");
		}

		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.26 verifyMapSuccessCenter failed:\n" + String.join("\n", errors));
		}
	}

	// -------------------------------------------------------------------------
	// Private helpers
	// -------------------------------------------------------------------------

	/**
	 * Validates that the {@code Content-Bbox} header value intersects the requested
	 * bounds (CRS84 order: minLon, minLat, maxLon, maxLat). A tolerance of 1.0 degree is
	 * applied to account for rounding in server-produced header values.
	 * @return an error message, or {@code null} when valid
	 */
	private String validateContentBboxIntersects(String contentBbox, double reqMinLon, double reqMinLat,
			double reqMaxLon, double reqMaxLat) {
		try {
			String[] parts = contentBbox.trim().split(",");
			if (parts.length < 4) {
				return "Content-Bbox header '" + contentBbox + "' does not contain at least 4 values.";
			}
			double minLon = Double.parseDouble(parts[0].trim());
			double minLat = Double.parseDouble(parts[1].trim());
			double maxLon = Double.parseDouble(parts[2].trim());
			double maxLat = Double.parseDouble(parts[3].trim());
			double tolerance = 1.0;
			// Verify the returned bbox intersects the requested area (not entirely
			// outside)
			if (maxLon < reqMinLon - tolerance || minLon > reqMaxLon + tolerance || maxLat < reqMinLat - tolerance
					|| minLat > reqMaxLat + tolerance) {
				return "Content-Bbox '" + contentBbox + "' does not intersect the requested bounds [" + reqMinLon + ","
						+ reqMinLat + "," + reqMaxLon + "," + reqMaxLat + "]" + " (tolerance=" + tolerance + " deg).";
			}
		}
		catch (NumberFormatException e) {
			return "Could not parse Content-Bbox header value '" + contentBbox + "': " + e.getMessage();
		}
		return null;
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
			System.err.println("[A.26] HTTP request failed for: " + urlString + " => " + e.getMessage());
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

		final String contentBbox;

		HttpResult(int status, byte[] body, String contentBbox) {
			this.status = status;
			this.body = body;
			this.contentBbox = contentBbox;
		}

	}

}
