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
 * URLs for interactive (manual) verification of A.15 scale-denominator-definition (Req
 * 15/B, C, G).
 *
 * <p>
 * Two URLs are produced for side-by-side comparison in the CTL form:
 * <ul>
 * <li>Explicit-scale map: {@code scale-denominator=40000000&width=1024&height=512&f=png}
 * - server renders at the requested scale with explicit dimensions (reference).</li>
 * <li>Scale-only map: {@code scale-denominator=40000000&f=png} - no width, height, or
 * bbox; the server must interpret the scale and derive appropriate defaults.</li>
 * </ul>
 * The human tester confirms whether Map A is rendered at approximately 1:40,000,000 scale
 * (verifying Req 15/B and C) and whether Map B covers a geographically sensible extent
 * consistent with the scale denominator (verifying Req 15/G when Subsetting is
 * supported).
 */
public final class ScaleDenominatorInteractiveTestUtils {

	/**
	 * Sentinel prefix returned when no valid map URL can be found. CTL scripts detect
	 * this prefix and display an appropriate warning.
	 */
	public static final String NOT_FOUND_PREFIX = "NOT_FOUND:";

	private static final String REL_MAP = "http://www.opengis.net/def/rel/ogc/1.0/map";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private ScaleDenominatorInteractiveTestUtils() {
	}

	/**
	 * Builds a map URL with an explicit scale-denominator, width, and height, serving as
	 * the reference map for verifying correct scale interpretation (Req 15/B and C).
	 * @param landingPageUrl the API landing page URL
	 * @return the map URL with explicit scale and dimensions, or a NOT_FOUND: prefixed
	 * string if the map endpoint cannot be resolved
	 */
	public static String buildExplicitScaleMapUrl(String landingPageUrl) {
		String mapUrl = findMapUrl(landingPageUrl);
		if (mapUrl == null) {
			return NOT_FOUND_PREFIX + landingPageUrl + "/map";
		}
		String separator = mapUrl.contains("?") ? "&" : "?";
		return mapUrl + separator + "scale-denominator=40000000&width=1024&height=512&f=png";
	}

	/**
	 * Builds a map URL with only a scale-denominator and no width, height, or bbox. The
	 * server must derive appropriate defaults, which verifies Req 15/G when the server
	 * supports Spatial Subsetting.
	 * @param landingPageUrl the API landing page URL
	 * @return the map URL with scale-denominator only, or a NOT_FOUND: prefixed string if
	 * the map endpoint cannot be resolved
	 */
	public static String buildScaleOnlyMapUrl(String landingPageUrl) {
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
