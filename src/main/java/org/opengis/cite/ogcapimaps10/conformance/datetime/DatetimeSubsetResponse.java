package org.opengis.cite.ogcapimaps10.conformance.datetime;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.opengis.cite.ogcapimaps10.conformance.CommonFixture;
import org.opengis.cite.ogcapimaps10.conformance.SuiteAttribute;
import org.opengis.cite.ogcapimaps10.domain.DatetimeSubsetResponseInteractiveTestResult;
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Implements Abstract Test A.30: /conf/datetime/subset-response
 *
 * <p>
 * Test Purpose: Verify that the implementation responds correctly to temporal subsetting
 * requests using the {@code subset} parameter (Requirement 30:
 * /req/datetime/subset-response).
 *
 * <p>
 * Assertions verified:
 * <ul>
 * <li>[Req30/A-instant] {@code subset=time("<narrow instant>")} response body differs
 * from full-extent body — temporal snapshot filter is applied to response content.</li>
 * <li>[Req30/A-interval] {@code subset=time("<low>":"<high>")} response body differs from
 * full-extent body — temporal interval filter is applied to response content.</li>
 * <li>[Req30/A-interactive] User visually confirms that the subset-filtered map shows
 * different content from the full-extent map, proving only data within the specified time
 * bounds is returned.</li>
 * </ul>
 */
public class DatetimeSubsetResponse extends CommonFixture {

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
		datetimeSupported = isDatetimeDeclared();
		if (!datetimeSupported) {
			return;
		}
		TemporalCollectionInfo info = findTemporalCollection();
		if (info != null) {
			temporalMapUrl = info.mapUrl;
			temporalFmtSuffix = hasFormatExtension(temporalMapUrl) ? "" : "&f=png";
			deriveTestDates(info.startDate, info.endDate);
		}
	}

	// -------------------------------------------------------------------------
	// Tests
	// -------------------------------------------------------------------------

	/**
	 * A.30 - Req 30/A (instant): verifies that the response body for
	 * {@code subset=time("<narrow instant>")} differs from the full-extent (no subset)
	 * body, proving that the temporal snapshot filter is applied.
	 */
	@Test(description = "A.30 Req 30/A (instant): subset=time(\"instant\") response body "
			+ "differs from full-extent body — temporal snapshot filter is applied.")
	public void verifySubsetInstantFilterApplied() {
		if (!datetimeSupported) {
			throw new SkipException("Server does not declare " + DATETIME_CONF_URI + ". Skipping A.30.");
		}
		if (temporalMapUrl == null) {
			throw new SkipException(
					"No temporal collection map found at " + rootUri + ". Skipping A.30 instant filter test.");
		}
		String tSep = temporalMapUrl.contains("?") ? "&" : "?";
		String fullFmtParam = temporalFmtSuffix.isEmpty() ? "" : tSep + "f=png";
		List<String> errors = new ArrayList<>();
		HttpResult resultFull = fetch(temporalMapUrl + fullFmtParam);
		HttpResult resultFiltered = fetch(
				temporalMapUrl + tSep + "subset=time(%22" + instant + "%22)" + temporalFmtSuffix);
		if (resultFull.status == 200 && resultFiltered.status == 200) {
			if (Arrays.equals(resultFull.body, resultFiltered.body)) {
				errors.add("[Req30/A-instant] subset=time(\"" + instant
						+ "\") response body is identical to the full-extent body"
						+ " — the server does not appear to be applying the temporal filter.");
			}
		}
		else if (resultFiltered.status != 200 && resultFiltered.status != 204) {
			errors.add("[Req30/A-instant] Expected HTTP 200 or 204 for subset=time(\"" + instant + "\") but got HTTP "
					+ resultFiltered.status + ".");
		}
		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.30 verifySubsetInstantFilterApplied failed:\n" + String.join("\n", errors));
		}
	}

	/**
	 * A.30 - Req 30/A (interval): verifies that the response body for
	 * {@code subset=time("<low>":"<high>")} differs from the full-extent (no subset)
	 * body, proving that the temporal interval filter is applied.
	 */
	@Test(description = "A.30 Req 30/A (interval): subset=time(\"low\":\"high\") response body "
			+ "differs from full-extent body — temporal interval filter is applied.")
	public void verifySubsetIntervalFilterApplied() {
		if (!datetimeSupported) {
			throw new SkipException("Server does not declare " + DATETIME_CONF_URI + ". Skipping A.30.");
		}
		if (temporalMapUrl == null) {
			throw new SkipException(
					"No temporal collection map found at " + rootUri + ". Skipping A.30 interval filter test.");
		}
		String tSep = temporalMapUrl.contains("?") ? "&" : "?";
		String fullFmtParam = temporalFmtSuffix.isEmpty() ? "" : tSep + "f=png";
		List<String> errors = new ArrayList<>();
		HttpResult resultFull = fetch(temporalMapUrl + fullFmtParam);
		HttpResult resultFiltered = fetch(temporalMapUrl + tSep + "subset=time(%22" + intervalLow + "%22:%22"
				+ intervalHigh + "%22)" + temporalFmtSuffix);
		if (resultFull.status == 200 && resultFiltered.status == 200) {
			if (Arrays.equals(resultFull.body, resultFiltered.body)) {
				errors.add("[Req30/A-interval] subset=time(\"" + intervalLow + "\":\"" + intervalHigh
						+ "\") response body is identical to the full-extent body"
						+ " — the server does not appear to be applying the temporal filter.");
			}
		}
		else if (resultFiltered.status != 200 && resultFiltered.status != 204) {
			errors.add("[Req30/A-interval] Expected HTTP 200 or 204 for subset=time(\"" + intervalLow + "\":\""
					+ intervalHigh + "\") but got HTTP " + resultFiltered.status + ".");
		}
		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.30 verifySubsetIntervalFilterApplied failed:\n" + String.join("\n", errors));
		}
	}

	/**
	 * A.30 - Req 30/A (interactive): the human tester visually confirms that the
	 * subset-filtered map shows different content from the full-extent map, proving that
	 * only data within the specified time bounds is returned.
	 */
	@Test(description = "A.30 Req 30/A (interactive): user confirms subset-filtered map "
			+ "differs from full-extent map — only data within time bounds returned.")
	public void verifySubsetResponseInteractive(ITestContext testContext) {
		if (!datetimeSupported) {
			throw new SkipException("Server does not declare " + DATETIME_CONF_URI + ". Skipping A.30.");
		}
		DatetimeSubsetResponseInteractiveTestResult result = (DatetimeSubsetResponseInteractiveTestResult) testContext
			.getSuite()
			.getAttribute(SuiteAttribute.DATETIME_SUBSET_RESPONSE_INTERACTIVE_TEST_RESULT.getName());
		if (result == null || !result.isEnabled()) {
			throw new SkipException("Interactive subset-response verification (A.30 Req 30/A) not enabled. "
					+ "Enable via TEAMENGINE form or set datetime_subset_response_interactive_enabled=true.");
		}
		if (!result.isFilterAppliedCorrect()) {
			throw new AssertionError(
					"A.30 Req 30/A: Tester reported that the subset-filtered map does NOT visually differ "
							+ "from the full-extent map — the server is not correctly applying the temporal filter.");
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
			// fall through
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
			long days = ChronoUnit.DAYS.between(start, end);
			LocalDate mid = start.plusDays(Math.max(1, days / 2));
			instant = mid + "T00:00:00Z";
			intervalLow = start + "T00:00:00Z";
			intervalHigh = mid + "T00:00:00Z";
		}
		catch (Exception e) {
			// keep defaults
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
			System.err.println("[A.30] HTTP request failed for: " + urlString + " => " + e.getMessage());
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
