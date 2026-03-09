package org.opengis.cite.ogcapimaps10.util.interactive;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility methods called from CTL scripts via the XSLT Java bridge to build map request
 * URLs for interactive (manual) verification of A.12 collections-response (Req 12B).
 */
public final class InteractiveTestUtils {

	/**
	 * Sentinel prefix returned when no valid map URL or collections can be found. CTL
	 * scripts detect this prefix and display an appropriate warning.
	 */
	public static final String NOT_FOUND_PREFIX = "NOT_FOUND:";

	private static final String REL_MAP = "http://www.opengis.net/def/rel/ogc/1.0/map";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private InteractiveTestUtils() {
	}

	/**
	 * Builds a dataset map URL using the first two available collection IDs in their
	 * natural order: {@code ?collections=id1,id2}.
	 * <p>
	 * Used to display Map A in the interactive rendering-order verification form.
	 * </p>
	 * @param landingPageUrl The API landing page URL.
	 * @return The map URL with collections in natural order, or a NOT_FOUND: prefixed
	 * string if the map endpoint or collections cannot be resolved.
	 */
	public static String buildFirstOrderCollectionsUrl(String landingPageUrl) {
		return buildCollectionsUrl(landingPageUrl, false);
	}

	/**
	 * Builds a dataset map URL using the first two available collection IDs in reversed
	 * order: {@code ?collections=id2,id1}.
	 * <p>
	 * Used to display Map B in the interactive rendering-order verification form.
	 * </p>
	 * @param landingPageUrl The API landing page URL.
	 * @return The map URL with collections in reversed order, or a NOT_FOUND: prefixed
	 * string if the map endpoint or collections cannot be resolved.
	 */
	public static String buildSecondOrderCollectionsUrl(String landingPageUrl) {
		return buildCollectionsUrl(landingPageUrl, true);
	}

	// -------------------------------------------------------------------------
	// Private helpers
	// -------------------------------------------------------------------------

	private static String buildCollectionsUrl(String landingPageUrl, boolean reversed) {
		String mapUrl = findMapUrl(landingPageUrl);
		if (mapUrl == null) {
			return NOT_FOUND_PREFIX + landingPageUrl;
		}

		List<String> ids = findFirstTwoCollectionIds(landingPageUrl);
		if (ids == null || ids.size() < 2) {
			return NOT_FOUND_PREFIX + mapUrl;
		}

		String collections = reversed ? ids.get(1) + "," + ids.get(0) : ids.get(0) + "," + ids.get(1);
		return appendParam(mapUrl, "collections", collections) + "&f=png";
	}

	/**
	 * Finds the dataset map URL from the landing page links (rel=ogc/1.0/map), falling
	 * back to {landingPageUrl}/map.
	 */
	@SuppressWarnings("unchecked")
	private static String findMapUrl(String landingPageUrl) {
		try {
			Map<String, Object> landingPage = fetchJson(landingPageUrl);
			if (landingPage != null) {
				List<Map<String, Object>> links = (List<Map<String, Object>>) landingPage.get("links");
				if (links != null) {
					for (Map<String, Object> link : links) {
						String rel = (String) link.get("rel");
						if (rel != null && matchesRelIgnoringScheme(rel, REL_MAP)) {
							String href = (String) link.get("href");
							return resolveUrl(landingPageUrl, href);
						}
					}
				}
			}
		}
		catch (Exception e) {
			// fall through to default
		}

		// Default: dataset map is conventionally at {landingPageUrl}/map
		String base = landingPageUrl.endsWith("/") ? landingPageUrl.substring(0, landingPageUrl.length() - 1)
				: landingPageUrl;
		return base + "/map";
	}

	/**
	 * Fetches the first two collection IDs from {landingPageUrl}/collections.
	 */
	@SuppressWarnings("unchecked")
	private static List<String> findFirstTwoCollectionIds(String landingPageUrl) {
		try {
			String base = landingPageUrl.endsWith("/") ? landingPageUrl.substring(0, landingPageUrl.length() - 1)
					: landingPageUrl;
			Map<String, Object> collectionsResponse = fetchJson(base + "/collections?f=json");
			if (collectionsResponse == null) {
				return null;
			}

			List<Map<String, Object>> collections = (List<Map<String, Object>>) collectionsResponse.get("collections");
			if (collections == null || collections.size() < 2) {
				return null;
			}

			String id1 = (String) collections.get(0).get("id");
			String id2 = (String) collections.get(1).get("id");
			if (id1 == null || id2 == null) {
				return null;
			}

			return List.of(id1, id2);
		}
		catch (Exception e) {
			return null;
		}
	}

	private static Map<String, Object> fetchJson(String urlString) {
		try {
			URL url = URI.create(urlString).toURL();
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");
			conn.setConnectTimeout(10000);
			conn.setReadTimeout(10000);
			if (conn.getResponseCode() == 200) {
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
		if (url == null) {
			return null;
		}
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

	private static String appendParam(String url, String param, String value) {
		String separator = url.contains("?") ? "&" : "?";
		return url + separator + param + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	private static boolean matchesRelIgnoringScheme(String actual, String expected) {
		return normalizeScheme(actual).equals(normalizeScheme(expected));
	}

	private static String normalizeScheme(String rel) {
		if (rel.startsWith("https://")) {
			return "http://" + rel.substring("https://".length());
		}
		return rel;
	}

}