package org.opengis.cite.ogcapimaps10.util.interactive;

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

/**
 * Utility methods for building map request URLs used in the interactive (manual)
 * verification of Abstract Test A.32 Req 32/A (datetime map-success).
 *
 * <p>
 * Two URLs are produced for side-by-side comparison by the human tester:
 * <ul>
 * <li>Subset-filtered map: {@code subset=time("instant")} — server renders only data
 * consistent with the given time instant.</li>
 * <li>Full-extent map: no temporal filter — server renders the entire temporal
 * extent.</li>
 * </ul>
 * The tester confirms that the content of the filtered map is consistent with the
 * requested datetime (Req 32/A).
 */
public final class DatetimeMapSuccessInteractiveTestUtils {

	/**
	 * Sentinel prefix returned when no valid temporal collection map URL can be found.
	 */
	public static final String NOT_FOUND_PREFIX = "NOT_FOUND:";

	private static final String REL_MAP = "http://www.opengis.net/def/rel/ogc/1.0/map";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private DatetimeMapSuccessInteractiveTestUtils() {
	}

	/**
	 * Builds a map URL filtered to a time instant derived from the collection's temporal
	 * extent (midpoint). Used as the temporal snapshot for verifying that the response
	 * content is consistent with the requested datetime (Req 32/A).
	 * @param landingPageUrl the API landing page URL
	 * @return the filtered map URL, or a NOT_FOUND: prefixed string if no temporal
	 * collection map can be resolved
	 */
	public static String buildInstantSubsetMapUrl(String landingPageUrl) {
		TemporalMapInfo info = findTemporalMapInfo(landingPageUrl);
		if (info == null) {
			return NOT_FOUND_PREFIX + landingPageUrl;
		}
		String sep = info.mapUrl.contains("?") ? "&" : "?";
		return info.mapUrl + sep + "subset=time(\"" + info.instant + "\")&f=png";
	}

	/**
	 * Builds the time instant string that is used in the subset-filtered URL, so the CTL
	 * form can display it to the tester.
	 * @param landingPageUrl the API landing page URL
	 * @return an ISO-8601 date-time string, or an empty string if not determinable
	 */
	public static String getInstant(String landingPageUrl) {
		TemporalMapInfo info = findTemporalMapInfo(landingPageUrl);
		return info != null ? info.instant : "";
	}

	/**
	 * Builds a map URL with no temporal filter, used as the full-extent reference for
	 * comparison (Req 32/A).
	 * @param landingPageUrl the API landing page URL
	 * @return the full-extent map URL, or a NOT_FOUND: prefixed string if no temporal
	 * collection map can be resolved
	 */
	public static String buildFullExtentMapUrl(String landingPageUrl) {
		TemporalMapInfo info = findTemporalMapInfo(landingPageUrl);
		if (info == null) {
			return NOT_FOUND_PREFIX + landingPageUrl;
		}
		String sep = info.mapUrl.contains("?") ? "&" : "?";
		return info.mapUrl + sep + "f=png";
	}

	// -------------------------------------------------------------------------
	// Private helpers
	// -------------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	private static TemporalMapInfo findTemporalMapInfo(String landingPageUrl) {
		try {
			String base = landingPageUrl.endsWith("/") ? landingPageUrl.substring(0, landingPageUrl.length() - 1)
					: landingPageUrl;
			Map<String, Object> collectionsDoc = fetchJson(base + "/collections?f=json");
			if (collectionsDoc == null) {
				return null;
			}
			List<Map<String, Object>> collections = (List<Map<String, Object>>) collectionsDoc.get("collections");
			if (collections == null) {
				return null;
			}
			for (Map<String, Object> collection : collections) {
				if (!isTemporalCollection(collection)) {
					continue;
				}
				List<Map<String, Object>> links = (List<Map<String, Object>>) collection.get("links");
				String href = findMapHref(links);
				if (href == null) {
					continue;
				}
				String mapUrl = resolveUrl(base, href);
				String instant = deriveInstant(collection);
				return new TemporalMapInfo(mapUrl, instant);
			}
		}
		catch (Exception e) {
			// fall through
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static boolean isTemporalCollection(Map<String, Object> collection) {
		Object extentObj = collection.get("extent");
		if (!(extentObj instanceof Map)) {
			return false;
		}
		Map<String, Object> extent = (Map<String, Object>) extentObj;
		return extent.get("temporal") instanceof Map;
	}

	@SuppressWarnings("unchecked")
	private static String deriveInstant(Map<String, Object> collection) {
		try {
			Map<String, Object> extent = (Map<String, Object>) collection.get("extent");
			Map<String, Object> temporal = (Map<String, Object>) extent.get("temporal");
			List<Object> intervals = (List<Object>) temporal.get("interval");
			if (intervals == null || intervals.isEmpty() || !(intervals.get(0) instanceof List)) {
				return "2021-01-01T00:00:00Z";
			}
			List<Object> first = (List<Object>) intervals.get(0);
			String startStr = first.size() > 0 && first.get(0) != null ? first.get(0).toString() : null;
			String endStr = first.size() > 1 && first.get(1) != null ? first.get(1).toString() : null;
			LocalDate start = parseDate(startStr);
			LocalDate end = parseDate(endStr);
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
			return mid + "T00:00:00Z";
		}
		catch (Exception e) {
			return "2021-01-01T00:00:00Z";
		}
	}

	private static LocalDate parseDate(String dateStr) {
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

	@SuppressWarnings("unchecked")
	private static String findMapHref(List<Map<String, Object>> links) {
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

	private static Map<String, Object> fetchJson(String urlString) {
		try {
			URL url = URI.create(urlString).toURL();
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");
			conn.setConnectTimeout(30000);
			conn.setReadTimeout(30000);
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

	private static String resolveUrl(String baseUrl, String url) {
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

	private static boolean matchesRelIgnoringScheme(String actual, String expected) {
		return normalizeScheme(actual).equals(normalizeScheme(expected));
	}

	private static String normalizeScheme(String uri) {
		if (uri != null && uri.startsWith("https://")) {
			return "http://" + uri.substring("https://".length());
		}
		return uri;
	}

	// -------------------------------------------------------------------------
	// Inner class
	// -------------------------------------------------------------------------

	private static class TemporalMapInfo {

		final String mapUrl;

		final String instant;

		TemporalMapInfo(String mapUrl, String instant) {
			this.mapUrl = mapUrl;
			this.instant = instant;
		}

	}

}
