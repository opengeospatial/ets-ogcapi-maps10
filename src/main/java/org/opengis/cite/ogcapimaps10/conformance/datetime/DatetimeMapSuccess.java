package org.opengis.cite.ogcapimaps10.conformance.datetime;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.opengis.cite.ogcapimaps10.conformance.CommonFixture;
import org.opengis.cite.ogcapimaps10.conformance.SuiteAttribute;
import org.opengis.cite.ogcapimaps10.domain.DatetimeMapSuccessInteractiveTestResult;
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Implements Abstract Test A.32: /conf/datetime/map-success
 *
 * <p>
 * Test Purpose: Verify that the implementation responds correctly to temporal subsetting
 * requests using the {@code subset} parameter (Requirement 32:
 * /req/datetime/map-success).
 *
 * <p>
 * Assertions verified:
 * <ul>
 * <li>[Req32/A-instant] {@code subset=time("<instant>")} returns HTTP 200 with a
 * non-empty body — the server successfully processes temporal snapshot requests.</li>
 * <li>[Req32/A-interval] {@code subset=time("<low>":"<high>")} returns HTTP 200 with a
 * non-empty body — the server successfully processes temporal interval requests.</li>
 * <li>[Req32/A-interactive] User visually confirms that the map content is consistent
 * with the requested datetime, proving only data from the specified time is
 * returned.</li>
 * </ul>
 */
public class DatetimeMapSuccess extends CommonFixture {

	private static final String DATETIME_CONF_URI = "https://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/datetime";

	private static final String REL_MAP = "http://www.opengis.net/def/rel/ogc/1.0/map";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private boolean datetimeSupported;

	/** Map URL for a collection with a temporal extent, or null if none found. */
	private String temporalMapUrl;

	/**
	 * Suffix appended to temporal map URLs. Empty when the URL already carries a format
	 * extension (e.g. {@code .map.png}); {@code &f=png} otherwise.
	 */
	private String temporalFmtSuffix;

	// Test dates derived from the discovered collection's temporal extent at runtime.
	private String instant = "2021-01-01T00:00:00Z";

	private String intervalLow = "2020-01-01T00:00:00Z";

	private String intervalHigh = "2021-01-01T00:00:00Z";

	// -------------------------------------------------------------------------
	// Setup
	// -------------------------------------------------------------------------

	/**
	 * Discovers whether the server declares the datetime conformance class, locates a
	 * temporal collection, reads its temporal extent, and derives test dates from it.
	 * @param testContext the TestNG test context
	 */
	@BeforeClass
	public void discoverTargets(ITestContext testContext) {
		System.out.println("[DatetimeMapSuccess] IUT: " + rootUri);
		datetimeSupported = isDatetimeDeclared();
		if (!datetimeSupported) {
			System.out.println("[DatetimeMapSuccess] Server does not declare " + DATETIME_CONF_URI + " — will skip.");
			return;
		}
		TemporalCollectionInfo info = findTemporalCollection();
		if (info != null) {
			temporalMapUrl = info.mapUrl;
			temporalFmtSuffix = hasFormatExtension(temporalMapUrl) ? "" : "&f=png";
			deriveTestDates(info.startDate, info.endDate);
			System.out.println("[DatetimeMapSuccess] Temporal map URL: " + temporalMapUrl + temporalFmtSuffix
					+ " | instant: " + instant + " | interval: [" + intervalLow + ", " + intervalHigh + "]");
		}
		else {
			System.out.println("[DatetimeMapSuccess] No temporal map collection found.");
		}
	}

	// -------------------------------------------------------------------------
	// Tests
	// -------------------------------------------------------------------------

	/**
	 * <pre>
	 * Abstract test A.32 — Case 1
	 *
	 * Identifier: /conf/datetime/map-success
	 * Requirement: Requirement 32: /req/datetime/map-success
	 * Test purpose: Verify that a temporal subsetting request with a single instant succeeds
	 *
	 * Given: a map resource that supports the datetime conformance class
	 * When: retrieving a map with subset=time("instant")
	 * Then:
	 * - assert that the response has HTTP status code 200,
	 * - assert that the response body is non-empty.
	 * </pre>
	 */
	@Test(description = "A.32 Req 32/A (instant): subset=time(\"instant\") returns HTTP 200 "
			+ "with non-empty body — temporal snapshot request succeeds (Requirement /req/datetime/map-success).")
	public void verifyDatetimeMapSuccessInstant() {
		if (!datetimeSupported) {
			throw new SkipException("Server does not declare " + DATETIME_CONF_URI + ". Skipping A.32.");
		}
		if (temporalMapUrl == null) {
			throw new SkipException("No temporal collection map found at " + rootUri + ". Skipping A.32 instant test.");
		}
		String tSep = temporalMapUrl.contains("?") ? "&" : "?";
		HttpResult result = fetch(temporalMapUrl + tSep + "subset=time(%22" + instant + "%22)" + temporalFmtSuffix);
		clearMessages();
		if (result.status != 200) {
			throw new AssertionError("A.32 verifyDatetimeMapSuccessInstant failed:\n"
					+ "[Req32/A-instant] subset=time(\"" + instant + "\") returned HTTP " + result.status
					+ " — the server SHALL return HTTP 200 for a valid temporal subsetting request (Req 32/A).");
		}
		if (result.body.length == 0) {
			throw new AssertionError("A.32 verifyDatetimeMapSuccessInstant failed:\n"
					+ "[Req32/A-instant] subset=time(\"" + instant + "\") returned an empty response body"
					+ " — the server SHALL return a non-empty map for a valid temporal subsetting request.");
		}
	}

	/**
	 * <pre>
	 * Abstract test A.32 — Case 2
	 *
	 * Identifier: /conf/datetime/map-success
	 * Requirement: Requirement 32: /req/datetime/map-success
	 * Test purpose: Verify that a temporal subsetting request with an interval succeeds
	 *
	 * Given: a map resource that supports the datetime conformance class
	 * When: retrieving a map with subset=time("low":"high")
	 * Then:
	 * - assert that the response has HTTP status code 200,
	 * - assert that the response body is non-empty.
	 * </pre>
	 */
	@Test(description = "A.32 Req 32/A (interval): subset=time(\"low\":\"high\") returns HTTP 200 "
			+ "with non-empty body — temporal interval request succeeds (Requirement /req/datetime/map-success).")
	public void verifyDatetimeMapSuccessInterval() {
		if (!datetimeSupported) {
			throw new SkipException("Server does not declare " + DATETIME_CONF_URI + ". Skipping A.32.");
		}
		if (temporalMapUrl == null) {
			throw new SkipException(
					"No temporal collection map found at " + rootUri + ". Skipping A.32 interval test.");
		}
		String tSep = temporalMapUrl.contains("?") ? "&" : "?";
		HttpResult result = fetch(temporalMapUrl + tSep + "subset=time(%22" + intervalLow + "%22:%22" + intervalHigh
				+ "%22)" + temporalFmtSuffix);
		clearMessages();
		if (result.status != 200) {
			throw new AssertionError("A.32 verifyDatetimeMapSuccessInterval failed:\n"
					+ "[Req32/A-interval] subset=time(\"" + intervalLow + "\":\"" + intervalHigh + "\") returned HTTP "
					+ result.status
					+ " — the server SHALL return HTTP 200 for a valid temporal subsetting request (Req 32/A).");
		}
		if (result.body.length == 0) {
			throw new AssertionError(
					"A.32 verifyDatetimeMapSuccessInterval failed:\n" + "[Req32/A-interval] subset=time(\""
							+ intervalLow + "\":\"" + intervalHigh + "\") returned an empty response body"
							+ " — the server SHALL return a non-empty map for a valid temporal subsetting request.");
		}
	}

	/**
	 * <pre>
	 * Abstract test A.32 — Case 3 (Interactive)
	 *
	 * Identifier: /conf/datetime/map-success
	 * Requirement: Requirement 32: /req/datetime/map-success
	 * Test purpose: Verify that the map content is consistent with the requested datetime
	 *
	 * Given: a map resource that supports the datetime conformance class
	 * When: retrieving a map with subset=time(...)
	 * Then:
	 * - assert (via user visual confirmation) that the map content reflects data from the
	 *   requested time period, not a default or arbitrary time.
	 * </pre>
	 */
	@Test(description = "A.32 Req 32/A (interactive): user confirms map content is consistent with requested datetime "
			+ "— temporal subsetting content verified (Requirement /req/datetime/map-success).")
	public void verifyDatetimeMapSuccessInteractive(ITestContext testContext) {
		if (!datetimeSupported) {
			throw new SkipException("Server does not declare " + DATETIME_CONF_URI + ". Skipping A.32.");
		}
		DatetimeMapSuccessInteractiveTestResult result = (DatetimeMapSuccessInteractiveTestResult) testContext
			.getSuite()
			.getAttribute(SuiteAttribute.DATETIME_INTERACTIVE_TEST_RESULT.getName());
		if (result == null || !result.isEnabled()) {
			throw new SkipException("Interactive datetime map-success verification (A.32) not enabled. "
					+ "Enable via TEAMENGINE form or set datetime_interactive_tests_enabled=true.");
		}
		if (!result.isTemporallyConsistent()) {
			throw new AssertionError(
					"A.32 Req 32/A: Tester reported that the map returned for a subset=time(...) request does NOT "
							+ "visually reflect data from the requested time period.");
		}
	}

	// -------------------------------------------------------------------------
	// Discovery helpers
	// -------------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	private boolean isDatetimeDeclared() {
		try {
			Map<String, Object> conformance = fetchJson(baseUrl() + "/conformance?f=json");
			if (conformance == null) {
				return false;
			}
			List<String> conformsTo = (List<String>) conformance.get("conformsTo");
			return conformsTo != null && conformsTo.stream()
				.anyMatch(u -> u != null && normalizeScheme(u).equals(normalizeScheme(DATETIME_CONF_URI)));
		}
		catch (Exception e) {
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	private TemporalCollectionInfo findTemporalCollection() {
		try {
			Map<String, Object> collectionsDoc = fetchJson(baseUrl() + "/collections?f=json");
			if (collectionsDoc == null) {
				return null;
			}
			List<Map<String, Object>> collections = (List<Map<String, Object>>) collectionsDoc.get("collections");
			if (collections == null) {
				return null;
			}
			for (Map<String, Object> collection : collections) {
				Map<String, Object> extent = castMap(collection.get("extent"));
				if (extent == null) {
					continue;
				}
				Map<String, Object> temporal = castMap(extent.get("temporal"));
				if (temporal == null) {
					continue;
				}
				List<Map<String, Object>> links = (List<Map<String, Object>>) collection.get("links");
				String href = findMapHref(links);
				if (href == null) {
					continue;
				}
				String mapUrl = resolveUrl(rootUri.toString(), href);
				String startDate = null;
				String endDate = null;
				List<Object> intervals = (List<Object>) temporal.get("interval");
				if (intervals != null && !intervals.isEmpty() && intervals.get(0) instanceof List) {
					List<Object> first = (List<Object>) intervals.get(0);
					if (first.size() >= 2) {
						startDate = first.get(0) != null ? first.get(0).toString() : null;
						endDate = first.get(1) != null ? first.get(1).toString() : null;
					}
				}
				return new TemporalCollectionInfo(mapUrl, startDate, endDate);
			}
		}
		catch (Exception e) {
			// fall through to null
		}
		return null;
	}

	private void deriveTestDates(String startDateStr, String endDateStr) {
		try {
			LocalDate start = parseDate(startDateStr);
			LocalDate end = parseDate(endDateStr);
			if (start == null) {
				start = LocalDate.now().minusYears(5);
			}
			if (end == null) {
				end = LocalDate.now();
			}
			if (end.isBefore(start)) {
				LocalDate swap = start;
				start = end;
				end = swap;
			}
			long days = ChronoUnit.DAYS.between(start, end);
			LocalDate mid = start.plusDays(Math.max(1, days / 2));
			instant = mid + "T00:00:00Z";
			intervalLow = start + "T00:00:00Z";
			intervalHigh = mid + "T00:00:00Z";
		}
		catch (Exception e) {
			// keep default values
		}
	}

	private LocalDate parseDate(String dateStr) {
		if (dateStr == null || dateStr.isEmpty() || "..".equals(dateStr)) {
			return null;
		}
		try {
			return LocalDate.parse(dateStr.substring(0, Math.min(10, dateStr.length())));
		}
		catch (Exception e) {
			return null;
		}
	}

	private String findMapHref(List<Map<String, Object>> links) {
		if (links == null) {
			return null;
		}
		for (Map<String, Object> link : links) {
			String rel = (String) link.get("rel");
			if (rel != null && matchesRelIgnoringScheme(rel, REL_MAP)) {
				return (String) link.get("href");
			}
		}
		return null;
	}

	// -------------------------------------------------------------------------
	// HTTP helpers
	// -------------------------------------------------------------------------

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
			System.err.println("[A.32] HTTP request failed for: " + urlString + " => " + e.getMessage());
			return new HttpResult(-1, new byte[0]);
		}
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

	private byte[] readAllBytes(InputStream is) throws Exception {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		byte[] chunk = new byte[8192];
		int n;
		while ((n = is.read(chunk)) != -1) {
			buffer.write(chunk, 0, n);
		}
		return buffer.toByteArray();
	}

	// -------------------------------------------------------------------------
	// Utility helpers
	// -------------------------------------------------------------------------

	private String baseUrl() {
		String url = rootUri.toString();
		return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
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

	private boolean hasFormatExtension(String url) {
		if (url == null) {
			return false;
		}
		String lower = url.toLowerCase();
		return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".tif")
				|| lower.endsWith(".tiff") || lower.endsWith(".webp");
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> castMap(Object obj) {
		return obj instanceof Map ? (Map<String, Object>) obj : null;
	}

	// -------------------------------------------------------------------------
	// Inner classes
	// -------------------------------------------------------------------------

	private static class HttpResult {

		final int status;

		final byte[] body;

		HttpResult(int status, byte[] body) {
			this.status = status;
			this.body = body;
		}

	}

	private static class TemporalCollectionInfo {

		final String mapUrl;

		final String startDate;

		final String endDate;

		TemporalCollectionInfo(String mapUrl, String startDate, String endDate) {
			this.mapUrl = mapUrl;
			this.startDate = startDate;
			this.endDate = endDate;
		}

	}

}
