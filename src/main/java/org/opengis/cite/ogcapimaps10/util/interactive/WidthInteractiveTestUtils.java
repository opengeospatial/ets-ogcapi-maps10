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
 * URLs for interactive (manual) verification of A.13 width-definition (Req 13/H).
 *
 * <p>
 * Two URLs are produced for side-by-side comparison in the CTL form. Both include a fixed
 * {@code scale-denominator} so that the server must compute the spatial extent from that
 * parameter:
 * <ul>
 * <li>Explicit-width map: {@code scale-denominator=40000000&width=1024&height=512&f=png}
 * - server uses the provided dimensions.</li>
 * <li>Default-width map: {@code scale-denominator=40000000&f=png} - no width or height
 * specified; the server must choose appropriate default dimensions.</li>
 * </ul>
 * The human tester confirms whether the default-width map appears to be rendered at a
 * reasonable scale and resolution consistent with the scale denominator.
 */
public final class WidthInteractiveTestUtils {

	/**
	 * Sentinel prefix returned when no valid map URL can be found. CTL scripts detect
	 * this prefix and display an appropriate warning.
	 */
	public static final String NOT_FOUND_PREFIX = "NOT_FOUND:";

	private static final String REL_MAP = "http://www.opengis.net/def/rel/ogc/1.0/map";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private WidthInteractiveTestUtils() {
	}

	/**
	 * Builds a map URL with explicit dimensions ({@code width=1024&height=512}) combined
	 * with a fixed scale denominator, serving as the reference map for comparison.
	 * @param landingPageUrl the API landing page URL
	 * @return the map URL with explicit dimensions, or a NOT_FOUND: prefixed string if
	 * the map endpoint cannot be resolved
	 */
	public static String buildExplicitWidthMapUrl(String landingPageUrl) {
		String mapUrl = findMapUrl(landingPageUrl);
		if (mapUrl == null) {
			return NOT_FOUND_PREFIX + landingPageUrl + "/map";
		}
		String separator = mapUrl.contains("?") ? "&" : "?";
		return mapUrl + separator + "scale-denominator=40000000&width=1024&height=512&f=png";
	}

	/**
	 * Builds a map URL with no {@code width} or {@code height} parameters, using only a
	 * fixed scale denominator. The server must choose appropriate default dimensions,
	 * which is the behavior being verified by Req 13/H.
	 * @param landingPageUrl the API landing page URL
	 * @return the map URL without width/height, or a NOT_FOUND: prefixed string if the
	 * map endpoint cannot be resolved
	 */
	public static String buildDefaultWidthMapUrl(String landingPageUrl) {
		String mapUrl = findMapUrl(landingPageUrl);
		if (mapUrl == null) {
			return NOT_FOUND_PREFIX + landingPageUrl + "/map";
		}
		String separator = mapUrl.contains("?") ? "&" : "?";
		return mapUrl + separator + "scale-denominator=40000000&f=png";
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
