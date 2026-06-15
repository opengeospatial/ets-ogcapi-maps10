package org.opengis.cite.ogcapimaps10.conformance.datetime;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
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
 * Implements Abstract Test A.28: /conf/datetime/datetime-response
 *
 * <p>
 * Test Purpose: Verify that the implementation responds correctly to map requests using
 * the {@code datetime} parameter (Requirement 28: /req/datetime/datetime-response).
 *
 * <p>
 * Assertions verified:
 * <ul>
 * <li>[Req28/A-broad] {@code datetime=2000/2030} broad interval → HTTP 200, non-empty
 * body — server accepts datetime and returns data.</li>
 * <li>[Req28/A-filter] Narrow {@code datetime} response body differs from full-extent (no
 * {@code datetime}) body — proves temporal filter is applied.</li>
 * <li>[Req28/A-header] {@code OGCAPI-datetime} response header (if present) is
 * structurally valid RFC 3339 — proves server reports actual content datetime.</li>
 * <li>[Req28/B] Non-temporal collection map with {@code datetime} filter → HTTP 200,
 * non-empty body — non-temporal resources are always included.</li>
 * </ul>
 */
public class DatetimeResponse extends CommonFixture {

	private static final String DATETIME_CONF_URI = "https://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/datetime";

	private static final String REL_MAP = "http://www.opengis.net/def/rel/ogc/1.0/map";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	/** Narrow instant used to test that temporal filtering changes the response. */
	private static final String NARROW_INSTANT = "2020-01-01T00:00:00Z";

	/** Broad interval start — wide enough to cover most temporal collections. */
	private static final String BROAD_START = "2000-01-01T00:00:00Z";

	/** Broad interval end — wide enough to cover most temporal collections. */
	private static final String BROAD_END = "2030-12-31T23:59:59Z";

	private String mapUrl;

	private String sep;

	private boolean datetimeSupported;

	/** Map URL for a collection with no temporal extent, or null if none found. */
	private String nonTemporalMapUrl;

	/** Map URL for a collection with a temporal extent, or null if none found. */
	private String temporalMapUrl;

	/**
	 * Suffix appended to temporal map URLs. Empty when the URL already carries a format
	 * extension (e.g. {@code .map.png}); {@code &f=png} otherwise.
	 */
	private String temporalFmtSuffix;

	// -------------------------------------------------------------------------
	// Setup
	// -------------------------------------------------------------------------

	/**
	 * Checks whether the server declares the datetime conformance class, discovers the
	 * dataset map endpoint, and discovers temporal and non-temporal collection maps.
	 * SkipException is not thrown here; each test method guards itself.
	 * @param testContext the TestNG test context
	 */
	@BeforeClass
	public void discoverTargets(ITestContext testContext) {
		datetimeSupported = isDatetimeDeclared();
		if (!datetimeSupported) {
			return;
		}
		mapUrl = findDatasetMapUrl(rootUri.toString());
		if (mapUrl != null) {
			sep = mapUrl.contains("?") ? "&" : "?";
		}
		temporalMapUrl = findTemporalCollectionMapUrl();
		temporalFmtSuffix = hasFormatExtension(temporalMapUrl) ? "" : "&f=png";
		nonTemporalMapUrl = findNonTemporalCollectionMapUrl();
	}

	// -------------------------------------------------------------------------
	// Tests
	// -------------------------------------------------------------------------

	/**
	 * A.28 — Req 28/A (broad): verifies that a very broad {@code datetime} interval
	 * returns HTTP 200 with a non-empty body. Uses the first temporal collection map if
	 * found, otherwise falls back to the dataset map.
	 */
	@Test(description = "A.28 Req 28/A (broad): datetime=2000/2030 broad interval → HTTP 200, non-empty body.")
	public void verifyBroadDatetimeReturnsData() {
		if (!datetimeSupported) {
			throw new SkipException("Server does not declare " + DATETIME_CONF_URI + ". Skipping A.28.");
		}
		String targetUrl = temporalMapUrl != null ? temporalMapUrl : mapUrl;
		if (targetUrl == null) {
			throw new SkipException("No map endpoint found at " + rootUri + ". Skipping A.28 broad datetime test.");
		}
		String targetSep = targetUrl.contains("?") ? "&" : "?";
		String fmtSuffix = hasFormatExtension(targetUrl) ? "" : "&f=png";
		List<String> errors = new ArrayList<>();
		String datetimeValue = BROAD_START + "/" + BROAD_END;
		HttpResult result = fetch(targetUrl + targetSep + "datetime=" + datetimeValue + fmtSuffix);
		if (result.status != 200 && result.status != 204) {
			errors.add("[Req28/A-broad] Expected HTTP 200 or 204 for datetime=" + datetimeValue + " but got HTTP "
					+ result.status + ".");
		}
		else if (result.status == 200 && result.body.length == 0) {
			errors.add("[Req28/A-broad] HTTP 200 received but response body was empty.");
		}
		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.28 verifyBroadDatetimeReturnsData failed:\n" + String.join("\n", errors));
		}
	}

	/**
	 * A.28 — Req 28/A (filter): verifies that the narrow {@code datetime} response body
	 * differs from the full-extent (no {@code datetime}) body on a temporal collection
	 * map, proving the server applies the temporal filter. Skipped if no temporal
	 * collection with a map link is found.
	 */
	@Test(description = "A.28 Req 28/A (filter): narrow datetime response body differs from full-extent "
			+ "(no datetime) body on a temporal collection — temporal filter applied.")
	public void verifyTemporalFilterApplied() {
		if (!datetimeSupported) {
			throw new SkipException("Server does not declare " + DATETIME_CONF_URI + ". Skipping A.28.");
		}
		if (temporalMapUrl == null) {
			throw new SkipException("No collection with temporal extent and a map link found at " + rootUri
					+ ". Skipping A.28 filter test.");
		}
		String tSep = temporalMapUrl.contains("?") ? "&" : "?";
		String fullFmtParam = temporalFmtSuffix.isEmpty() ? "" : tSep + "f=png";
		List<String> errors = new ArrayList<>();
		HttpResult resultFull = fetch(temporalMapUrl + fullFmtParam);
		HttpResult resultNarrow = fetch(temporalMapUrl + tSep + "datetime=" + NARROW_INSTANT + temporalFmtSuffix);
		if (resultFull.status == 200 && resultNarrow.status == 200) {
			if (Arrays.equals(resultFull.body, resultNarrow.body)) {
				errors.add("[Req28/A-filter] Narrow datetime=" + NARROW_INSTANT
						+ " response body is identical to the full-extent body"
						+ " — the server does not appear to be applying the temporal filter.");
			}
		}
		else if (resultNarrow.status != 200 && resultNarrow.status != 204) {
			errors.add("[Req28/A-filter] Expected HTTP 200 or 204 for datetime=" + NARROW_INSTANT + " but got HTTP "
					+ resultNarrow.status + ".");
		}
		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.28 verifyTemporalFilterApplied failed:\n" + String.join("\n", errors));
		}
	}

	/**
	 * A.28 — Req 28/A (header): verifies that the {@code OGCAPI-datetime} response
	 * header, when present, is structurally valid RFC 3339 (instant or interval with Z
	 * notation). Skipped when the header is absent, since it is only a SHOULD per
	 * Recommendation 14.
	 */
	@Test(description = "A.28 Req 28/A (header): OGCAPI-datetime response header (if present) "
			+ "is structurally valid RFC 3339.")
	public void verifyOgcApiDatetimeHeader() {
		if (!datetimeSupported) {
			throw new SkipException("Server does not declare " + DATETIME_CONF_URI + ". Skipping A.28.");
		}
		if (mapUrl == null) {
			throw new SkipException("No map endpoint found at " + rootUri + ". Skipping A.28 header test.");
		}
		HttpResult result = fetch(mapUrl + sep + "datetime=" + NARROW_INSTANT + "&f=png");
		if (result.ogcApiDatetime == null) {
			throw new SkipException(
					"OGCAPI-datetime response header not present (SHOULD per Rec. 14). Skipping header validation.");
		}
		List<String> errors = new ArrayList<>();
		String headerError = validateOgcApiDatetimeHeader(result.ogcApiDatetime);
		if (headerError != null) {
			errors.add("[Req28/A-header] " + headerError);
		}
		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.28 verifyOgcApiDatetimeHeader failed:\n" + String.join("\n", errors));
		}
	}

	/**
	 * A.28 — Req 28/B: verifies that non-temporal resources are always included even when
	 * a {@code datetime} filter is active. Uses the first collection that declares no
	 * temporal extent in its metadata. Skipped if no such collection is found.
	 */
	@Test(description = "A.28 Req 28/B: non-temporal collection map with datetime filter "
			+ "→ HTTP 200, non-empty body (non-temporal resources always present).")
	public void verifyNonTemporalResourcesPresent() {
		if (!datetimeSupported) {
			throw new SkipException("Server does not declare " + DATETIME_CONF_URI + ". Skipping A.28.");
		}
		if (nonTemporalMapUrl == null) {
			throw new SkipException(
					"No collection without temporal extent found at " + rootUri + ". Skipping A.28 28/B test.");
		}
		List<String> errors = new ArrayList<>();
		String ntSep = nonTemporalMapUrl.contains("?") ? "&" : "?";
		String ntFmtSuffix = hasFormatExtension(nonTemporalMapUrl) ? "" : "&f=png";
		HttpResult result = fetch(nonTemporalMapUrl + ntSep + "datetime=" + NARROW_INSTANT + ntFmtSuffix);
		if (result.status != 200) {
			errors.add("[Req28/B] Expected HTTP 200 for non-temporal collection map with datetime=" + NARROW_INSTANT
					+ " but got HTTP " + result.status
					+ ". Non-temporal resources must always be present in the result.");
		}
		else if (result.body.length == 0) {
			errors.add("[Req28/B] HTTP 200 received but response body was empty"
					+ " — non-temporal resources should always appear in the map.");
		}
		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.28 verifyNonTemporalResourcesPresent failed:\n" + String.join("\n", errors));
		}
	}

	// -------------------------------------------------------------------------
	// OAS-datetime header validation
	// -------------------------------------------------------------------------

	private String validateOgcApiDatetimeHeader(String headerValue) {
		String trimmed = headerValue.trim();
		if (trimmed.contains("/")) {
			return validateOgcApiDatetimeInterval(trimmed);
		}
		return validateRfc3339Instant(trimmed);
	}

	private String validateOgcApiDatetimeInterval(String value) {
		String[] parts = value.split("/", 2);
		String startError = isUnbounded(parts[0]) ? null : validateRfc3339Instant(parts[0].trim());
		String endError = isUnbounded(parts[1]) ? null : validateRfc3339Instant(parts[1].trim());
		if (startError != null) {
			return "OGCAPI-datetime interval start is not valid RFC 3339: '" + parts[0] + "'. " + startError;
		}
		if (endError != null) {
			return "OGCAPI-datetime interval end is not valid RFC 3339: '" + parts[1] + "'. " + endError;
		}
		return null;
	}

	private boolean isUnbounded(String part) {
		String trimmed = part.trim();
		return trimmed.isEmpty() || "..".equals(trimmed);
	}

	private String validateRfc3339Instant(String value) {
		try {
			Instant.parse(value);
			return null;
		}
		catch (Exception e) {
			return "'" + value + "' cannot be parsed as RFC 3339 Z-notation instant: " + e.getMessage();
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
	private String findDatasetMapUrl(String landingPageUrl) {
		try {
			Map<String, Object> landingPage = fetchJson(baseUrl() + "?f=json");
			if (landingPage != null) {
				List<Map<String, Object>> links = (List<Map<String, Object>>) landingPage.get("links");
				String href = findMapHref(links);
				if (href != null) {
					return resolveUrl(landingPageUrl, href);
				}
			}
		}
		catch (Exception e) {
			// fall through to default
		}
		return baseUrl() + "/map";
	}

	@SuppressWarnings("unchecked")
	private String findNonTemporalCollectionMapUrl() {
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
				if (isNonTemporalCollection(collection)) {
					List<Map<String, Object>> links = (List<Map<String, Object>>) collection.get("links");
					String href = findMapHref(links);
					if (href != null) {
						return resolveUrl(rootUri.toString(), href);
					}
				}
			}
		}
		catch (Exception e) {
			// fall through
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private String findTemporalCollectionMapUrl() {
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
				if (isTemporalCollection(collection)) {
					List<Map<String, Object>> links = (List<Map<String, Object>>) collection.get("links");
					String href = findMapHref(links);
					if (href != null) {
						return resolveUrl(rootUri.toString(), href);
					}
				}
			}
		}
		catch (Exception e) {
			// fall through
		}
		return null;
	}

	private boolean isTemporalCollection(Map<String, Object> collection) {
		Map<String, Object> extent = castMap(collection.get("extent"));
		if (extent == null) {
			return false;
		}
		return castMap(extent.get("temporal")) != null;
	}

	private boolean isNonTemporalCollection(Map<String, Object> collection) {
		Map<String, Object> extent = castMap(collection.get("extent"));
		if (extent == null) {
			return true;
		}
		return castMap(extent.get("temporal")) == null;
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

	private HttpResult fetch(String urlString) {
		try {
			HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(30000);
			conn.setReadTimeout(60000);
			conn.setInstanceFollowRedirects(true);
			applyAuth(conn);
			int status = conn.getResponseCode();
			String ogcApiDatetime = conn.getHeaderField("OGCAPI-datetime");
			byte[] body = new byte[0];
			if (status == 200) {
				try (InputStream is = conn.getInputStream()) {
					body = readAllBytes(is);
				}
			}
			return new HttpResult(status, body, ogcApiDatetime);
		}
		catch (Exception e) {
			System.err.println("[A.28] HTTP request failed for: " + urlString + " => " + e.getMessage());
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
	// Inner class
	// -------------------------------------------------------------------------

	private static class HttpResult {

		final int status;

		final byte[] body;

		final String ogcApiDatetime;

		HttpResult(int status, byte[] body, String ogcApiDatetime) {
			this.status = status;
			this.body = body;
			this.ogcApiDatetime = ogcApiDatetime;
		}

	}

}
