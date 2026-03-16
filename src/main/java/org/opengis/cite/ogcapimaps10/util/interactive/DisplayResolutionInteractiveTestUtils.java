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
 * Utility methods called from CTL scripts via the XSLT Java bridge to build map request
 * URLs for interactive (manual) verification of A.16 display-resolution (Req 16).
 *
 * <p>
 * Two URLs are produced for side-by-side comparison in the CTL form:
 * <ul>
 * <li>Default resolution map: {@code mm-per-pixel=0.28} (standard 90 DPI screen)</li>
 * <li>High-DPI map: {@code mm-per-pixel=0.1} (small physical pixels, more detail
 * expected)</li>
 * </ul>
 * The human tester confirms whether the two maps look visually different.
 */
public final class DisplayResolutionInteractiveTestUtils {

	/**
	 * Sentinel prefix returned when no valid map URL can be found. CTL scripts detect
	 * this prefix and display an appropriate warning.
	 */
	public static final String NOT_FOUND_PREFIX = "NOT_FOUND:";

	private static final String REL_MAP = "http://www.opengis.net/def/rel/ogc/1.0/map";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private DisplayResolutionInteractiveTestUtils() {
	}

	/**
	 * Builds a map URL with the default display resolution ({@code mm-per-pixel=0.28}).
	 * @param landingPageUrl the API landing page URL
	 * @return the map URL with default resolution, or a NOT_FOUND: prefixed string if the
	 * map endpoint cannot be resolved
	 */
	public static String buildDefaultResolutionMapUrl(String landingPageUrl) {
		String mapUrl = findMapUrl(landingPageUrl);
		if (mapUrl == null) {
			return NOT_FOUND_PREFIX + landingPageUrl + "/map";
		}
		String separator = mapUrl.contains("?") ? "&" : "?";
		return mapUrl + separator + "f=png&mm-per-pixel=0.28";
	}

	/**
	 * Builds a map URL with no {@code mm-per-pixel} parameter, relying on the server's
	 * default assumption of 0.28 mm/pixel.
	 * @param landingPageUrl the API landing page URL
	 * @return the map URL without mm-per-pixel, or a NOT_FOUND: prefixed string if the
	 * map endpoint cannot be resolved
	 */
	public static String buildNoParamMapUrl(String landingPageUrl) {
		String mapUrl = findMapUrl(landingPageUrl);
		if (mapUrl == null) {
			return NOT_FOUND_PREFIX + landingPageUrl + "/map";
		}
		String separator = mapUrl.contains("?") ? "&" : "?";
		return mapUrl + separator + "f=png";
	}

	/**
	 * Builds a map URL with a high-DPI display resolution ({@code mm-per-pixel=0.1}).
	 * This should produce a map with finer detail than the default resolution.
	 * @param landingPageUrl the API landing page URL
	 * @return the map URL with high-DPI resolution, or a NOT_FOUND: prefixed string if
	 * the map endpoint cannot be resolved
	 */
	public static String buildHighDpiMapUrl(String landingPageUrl) {
		String mapUrl = findMapUrl(landingPageUrl);
		if (mapUrl == null) {
			return NOT_FOUND_PREFIX + landingPageUrl + "/map";
		}
		String separator = mapUrl.contains("?") ? "&" : "?";
		return mapUrl + separator + "f=png&mm-per-pixel=0.1";
	}

	// -------------------------------------------------------------------------
	// Private helpers
	// -------------------------------------------------------------------------

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

	private static String normalizeScheme(String rel) {
		if (rel.startsWith("https://")) {
			return "http://" + rel.substring("https://".length());
		}
		return rel;
	}

}