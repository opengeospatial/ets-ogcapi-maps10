package org.opengis.cite.ogcapimaps10.util.interactive;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility methods for building map request URLs used in the interactive (manual)
 * verification of Abstract Test A.30 Req 30/A (datetime subset-response).
 *
 * <p>
 * Two URLs are produced for side-by-side comparison by the human tester:
 * <ul>
 * <li>Subset-filtered map: {@code subset=time("2021-01-01T00:00:00Z")} - server renders
 * only data intersecting January 1, 2021.</li>
 * <li>Full-extent map: no temporal filter - server renders the entire temporal
 * extent.</li>
 * </ul>
 * The tester confirms whether the filtered map visually differs from the full-extent map,
 * proving that only data within the specified time bounds is returned (Req 30/A).
 */
public final class DatetimeSubsetResponseInteractiveTestUtils {

	/**
	 * Sentinel prefix returned when no valid temporal collection map URL can be found.
	 */
	public static final String NOT_FOUND_PREFIX = "NOT_FOUND:";

	private static final String REL_MAP = "http://www.opengis.net/def/rel/ogc/1.0/map";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private DatetimeSubsetResponseInteractiveTestUtils() {
	}

	/**
	 * Builds a map URL filtered to {@code subset=time("2021-01-01T00:00:00Z")}, used as
	 * the temporal snapshot for verifying that the server applies the temporal filter
	 * (Req 30/A).
	 * @param landingPageUrl the API landing page URL
	 * @return the filtered map URL, or a NOT_FOUND: prefixed string if no temporal
	 * collection map can be resolved
	 */
	public static String buildSubsetFilteredMapUrl(String landingPageUrl) {
		String mapUrl = findTemporalMapUrl(landingPageUrl);
		if (mapUrl == null) {
			return NOT_FOUND_PREFIX + landingPageUrl;
		}
		String sep = mapUrl.contains("?") ? "&" : "?";
		return mapUrl + sep + "subset=time(\"2021-01-01T00:00:00Z\")";
	}

	/**
	 * Builds a map URL with no temporal filter, used as the full-extent reference for
	 * verifying that the server applies the temporal filter (Req 30/A).
	 * @param landingPageUrl the API landing page URL
	 * @return the full-extent map URL, or a NOT_FOUND: prefixed string if no temporal
	 * collection map can be resolved
	 */
	public static String buildFullExtentMapUrl(String landingPageUrl) {
		String mapUrl = findTemporalMapUrl(landingPageUrl);
		if (mapUrl == null) {
			return NOT_FOUND_PREFIX + landingPageUrl;
		}
		return mapUrl;
	}

	// -------------------------------------------------------------------------
	// Private helpers
	// -------------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	private static String findTemporalMapUrl(String landingPageUrl) {
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
				if (isTemporalCollection(collection)) {
					List<Map<String, Object>> links = (List<Map<String, Object>>) collection.get("links");
					String href = findMapHref(links);
					if (href != null) {
						return resolveUrl(landingPageUrl, href);
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
	private static boolean isTemporalCollection(Map<String, Object> collection) {
		Object extentObj = collection.get("extent");
		if (!(extentObj instanceof Map)) {
			return false;
		}
		Map<String, Object> extent = (Map<String, Object>) extentObj;
		return extent.get("temporal") instanceof Map;
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

}
