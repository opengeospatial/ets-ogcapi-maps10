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
 * Implements Abstract Test A.23: /conf/spatial-subsetting/subset-response
 *
 * <p>
 * Test Purpose: Verify that the implementation responds correctly to map requests using
 * the {@code subset} parameter (Requirement 23: /req/spatial-subsetting/subset-response).
 *
 * <p>
 * Assertions verified:
 * <ul>
 * <li>[Req23/A-interval] The server returns HTTP 200 with a non-empty body for a valid
 * spatial subset interval (Lat+Lon) without an explicit subset-crs (CRS84 assumed).</li>
 * <li>[Req23/A-interval-crs] Same subset interval with an explicit
 * subset-crs=[OGC:CRS84].</li>
 * <li>[Req23/A-point] The server returns HTTP 200 with a non-empty body for a
 * single-point (slicing) subset.</li>
 * <li>[Req23/A-content-bbox] If the {@code Content-Bbox} response header is present, its
 * bounding box values must fall within the requested subset bounds.</li>
 * <li>[Req23/A-distinct] The subset response body must differ from the full-extent
 * (no-subset) response body, proving the server actually applied the spatial filter.</li>
 * </ul>
 */
public class SubsetResponseTest extends CommonFixture {

	/** URL-encoded safe CURIE for CRS84: [OGC:CRS84] */
	private static final String CRS84_SAFE_CURIE = "%5BOGC%3ACRS84%5D";

	private static final String REL_MAP = "http://www.opengis.net/def/rel/ogc/1.0/map";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	/** Map URL discovered in {@link #discoverTargets}. */
	private String mapUrl;

	/** Query-string separator for {@link #mapUrl}. */
	private String sep;

	/**
	 * Holds the HTTP status, response body bytes, and Content-Bbox header value for one
	 * HTTP GET response.
	 */
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
	 * A.23 — Req 23/A: verifies that the server returns a correct, non-empty response for
	 * subset interval requests (with and without explicit subset-crs), for a single-point
	 * (slicing) subset, and that the subset response content differs from a full-extent
	 * response. Also checks the Content-Bbox header when present.
	 */
	@Test(description = "A.23 Req 23/A: subset interval and point requests return HTTP 200 "
			+ "with non-empty body; Content-Bbox (if present) is within requested bounds; "
			+ "subset content differs from full-extent content.")
	public void verifySubsetResponse() {
		if (mapUrl == null) {
			throw new SkipException("No map endpoint (rel=ogc/1.0/map) found at " + rootUri + ". Skipping A.23 test.");
		}

		List<String> errors = new ArrayList<>();

		// --- Req 23/A (row 1): interval subset without subset-crs → HTTP 200, non-empty
		// body
		// subset=Lat(-10:10),Lon(-10:10) URL-encoded
		HttpResult resultInterval = fetch(mapUrl + sep + "subset=Lat%28-10%3A10%29%2CLon%28-10%3A10%29&f=png");
		if (resultInterval.status != 200) {
			errors.add("[Req23/A-interval] Expected HTTP 200 for subset=Lat(-10:10),Lon(-10:10)" + " but got HTTP "
					+ resultInterval.status + ".");
		}
		else if (resultInterval.body.length == 0) {
			errors.add("[Req23/A-interval] HTTP 200 received but response body was empty.");
		}

		// --- Req 23/A (row 2): same interval with explicit subset-crs=[OGC:CRS84] → HTTP
		// 200, non-empty body
		HttpResult resultIntervalCrs = fetch(mapUrl + sep + "subset=Lat%28-10%3A10%29%2CLon%28-10%3A10%29&subset-crs="
				+ CRS84_SAFE_CURIE + "&f=png");
		if (resultIntervalCrs.status != 200) {
			errors.add("[Req23/A-interval-crs] Expected HTTP 200 for subset=Lat(-10:10),Lon(-10:10)"
					+ "&subset-crs=[OGC:CRS84] but got HTTP " + resultIntervalCrs.status + ".");
		}
		else if (resultIntervalCrs.body.length == 0) {
			errors.add("[Req23/A-interval-crs] HTTP 200 received but response body was empty.");
		}

		// --- Req 23/A (row 3): single-point (slicing) subset → HTTP 200, non-empty body
		// subset=Lat(0),Lon(0) URL-encoded
		HttpResult resultPoint = fetch(mapUrl + sep + "subset=Lat%280%29%2CLon%280%29&f=png");
		if (resultPoint.status != 200) {
			errors.add("[Req23/A-point] Expected HTTP 200 for subset=Lat(0),Lon(0)" + " but got HTTP "
					+ resultPoint.status + ".");
		}
		else if (resultPoint.body.length == 0) {
			errors.add("[Req23/A-point] HTTP 200 received but response body was empty.");
		}

		// --- Req 23/A (row 4): Content-Bbox header check (conditional — only if header
		// is present)
		if (resultInterval.contentBbox != null) {
			String bboxError = validateContentBbox(resultInterval.contentBbox, -10.0, -10.0, 10.0, 10.0);
			if (bboxError != null) {
				errors.add("[Req23/A-content-bbox] " + bboxError);
			}
		}

		// --- Req 23/A (row 5): subset body must differ from the full-extent (no-subset)
		// body
		HttpResult resultFull = fetch(mapUrl + sep + "f=png");
		if (resultFull.status == 200 && resultInterval.status == 200) {
			if (Arrays.equals(resultFull.body, resultInterval.body)) {
				errors.add("[Req23/A-distinct] Subset response body is identical to the full-extent"
						+ " response body — the server does not appear to be applying the spatial filter.");
			}
		}

		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.23 verifySubsetResponse failed:\n" + String.join("\n", errors));
		}
	}

	// -------------------------------------------------------------------------
	// Private helpers
	// -------------------------------------------------------------------------

	/**
	 * Validates that the Content-Bbox header value falls within the requested subset
	 * bounds (CRS84 order: minLon, minLat, maxLon, maxLat). A tolerance of 1.0 degree is
	 * applied to account for rounding in server-produced header values.
	 * @return an error message, or {@code null} when the header is valid
	 */
	private String validateContentBbox(String contentBbox, double reqMinLon, double reqMinLat, double reqMaxLon,
			double reqMaxLat) {
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
			if (minLon < reqMinLon - tolerance || minLat < reqMinLat - tolerance || maxLon > reqMaxLon + tolerance
					|| maxLat > reqMaxLat + tolerance) {
				return "Content-Bbox '" + contentBbox + "' falls outside the requested subset bounds [" + reqMinLon
						+ "," + reqMinLat + "," + reqMaxLon + "," + reqMaxLat + "]" + " (tolerance=" + tolerance
						+ " deg).";
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
			System.err.println("[A.23] HTTP request failed for: " + urlString + " => " + e.getMessage());
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

}
