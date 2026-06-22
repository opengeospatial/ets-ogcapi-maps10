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
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Implements Abstract Test A.31: /conf/datetime/axis
 *
 * <p>
 * Test Purpose: Verify that the implementation supports the time axis for temporal
 * subsetting using the {@code subset} parameter (Requirement 31: /req/datetime/axis).
 *
 * <p>
 * Assertions verified:
 * <ul>
 * <li>[Req31/A-instant] {@code subset=time("<instant>")} is accepted — the server
 * recognizes {@code time} as a valid axis name for a single time coordinate.</li>
 * <li>[Req31/A-interval] {@code subset=time("<low>":"<high>")} is accepted — the server
 * recognizes {@code time} as a valid axis name for an interval.</li>
 * </ul>
 */
public class DatetimeAxis extends CommonFixture {

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
	// Only read by the test methods when temporalMapUrl is non-null, by which point
	// deriveTestDates() has always set them.
	private String instant;

	private String intervalLow;

	private String intervalHigh;

	// -------------------------------------------------------------------------
	// Setup
	// -------------------------------------------------------------------------

	/**
	 * Discovers whether the server declares the datetime conformance class, locates a
	 * temporal collection, reads its temporal extent, and derives test dates from it.
	 */
	@BeforeClass
	public void discoverTargets() {
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
	 * A.31 - Req 31/A (instant): verifies that {@code subset=time("<instant>")} is
	 * accepted, proving that the server recognizes {@code time} as a valid axis name for
	 * a single time coordinate.
	 */
	@Test(description = "A.31 Req 31/A (instant): subset=time(\"instant\") is accepted "
			+ "— the server recognizes time as a valid axis name.")
	public void verifySubsetInstantAxisAccepted() {
		if (!datetimeSupported) {
			throw new SkipException("Server does not declare " + DATETIME_CONF_URI + ". Skipping A.31.");
		}
		if (temporalMapUrl == null) {
			throw new SkipException(
					"No temporal collection map found at " + rootUri + ". Skipping A.31 instant axis test.");
		}
		String tSep = temporalMapUrl.contains("?") ? "&" : "?";
		String fullFmtParam = temporalFmtSuffix.isEmpty() ? "" : tSep + "f=png";
		HttpResult resultFull = fetch(temporalMapUrl + fullFmtParam);
		HttpResult resultSubset = fetch(
				temporalMapUrl + tSep + "subset=time(%22" + instant + "%22)" + temporalFmtSuffix);
		clearMessages();
		if (resultFull.status != 200 && resultFull.status != 204) {
			throw new AssertionError("A.31 verifySubsetInstantAxisAccepted failed:\n"
					+ "[Req31/A-instant] Full-extent map request returned HTTP " + resultFull.status
					+ " — check that basicAuth is configured correctly.");
		}
		if (isRejectedAxisName(resultSubset.status)) {
			throw new AssertionError("A.31 verifySubsetInstantAxisAccepted failed:\n"
					+ "[Req31/A-instant] subset=time(\"" + instant + "\") returned HTTP " + resultSubset.status
					+ " — the server SHALL support \"time\" as axis name in the subset parameter (Req 31/A).");
		}
	}

	/**
	 * A.31 - Req 31/A (interval): verifies that {@code subset=time("<low>":"<high>")} is
	 * accepted, proving that the server recognizes {@code time} as a valid axis name for
	 * an interval.
	 */
	@Test(description = "A.31 Req 31/A (interval): subset=time(\"low\":\"high\") is accepted "
			+ "— the server recognizes time as a valid axis name.")
	public void verifySubsetIntervalAxisAccepted() {
		if (!datetimeSupported) {
			throw new SkipException("Server does not declare " + DATETIME_CONF_URI + ". Skipping A.31.");
		}
		if (temporalMapUrl == null) {
			throw new SkipException(
					"No temporal collection map found at " + rootUri + ". Skipping A.31 interval axis test.");
		}
		String tSep = temporalMapUrl.contains("?") ? "&" : "?";
		String fullFmtParam = temporalFmtSuffix.isEmpty() ? "" : tSep + "f=png";
		HttpResult resultFull = fetch(temporalMapUrl + fullFmtParam);
		HttpResult resultSubset = fetch(temporalMapUrl + tSep + "subset=time(%22" + intervalLow + "%22:%22"
				+ intervalHigh + "%22)" + temporalFmtSuffix);
		clearMessages();
		if (resultFull.status != 200 && resultFull.status != 204) {
			throw new AssertionError("A.31 verifySubsetIntervalAxisAccepted failed:\n"
					+ "[Req31/A-interval] Full-extent map request returned HTTP " + resultFull.status
					+ " — check that basicAuth is configured correctly.");
		}
		if (isRejectedAxisName(resultSubset.status)) {
			throw new AssertionError(
					"A.31 verifySubsetIntervalAxisAccepted failed:\n" + "[Req31/A-interval] subset=time(\""
							+ intervalLow + "\":\"" + intervalHigh + "\") returned HTTP " + resultSubset.status
							+ " — the server SHALL support \"time\" as axis name in the subset parameter (Req 31/A).");
		}
	}

	// -------------------------------------------------------------------------
	// Assertion helper
	// -------------------------------------------------------------------------

	/**
	 * A 404 is permitted by Req 29/D when the time coordinate falls entirely outside the
	 * resource's valid range. Any other 4xx indicates the {@code time} axis name itself
	 * was not recognized, which violates Req 31/A.
	 */
	private boolean isRejectedAxisName(int status) {
		return status >= 400 && status < 500 && status != 404;
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
			System.err.println("[A.31] HTTP request failed for: " + urlString + " => " + e.getMessage());
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
