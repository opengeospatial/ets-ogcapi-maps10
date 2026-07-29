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
 * URLs for interactive (manual) verification of A.37 orientation-definition (Req 37/A,
 * B).
 *
 * <p>
 * Two URLs are produced for side-by-side comparison in the CTL form:
 * <ul>
 * <li>Default map: {@code f=png} - no orientation parameter; the server renders north-up
 * (0°) by default (Req 37/B).</li>
 * <li>Rotated map: {@code orientation=45&f=png} - the server must rotate the map content
 * 45° counterclockwise (Req 37/A).</li>
 * </ul>
 * The human tester confirms whether Map B appears visually rotated 45° counterclockwise
 * relative to Map A, verifying that the server correctly implements the orientation
 * parameter direction (Req 37/A) and that the default is north-up (Req 37/B).
 */
public final class OrientationInteractiveTestUtils {

	/**
	 * Sentinel prefix returned when no valid map URL can be found. CTL scripts detect
	 * this prefix and display an appropriate warning.
	 */
	public static final String NOT_FOUND_PREFIX = "NOT_FOUND:";

	private static final String REL_MAP = "http://www.opengis.net/def/rel/ogc/1.0/map";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private OrientationInteractiveTestUtils() {
	}

	/**
	 * Builds a map URL with no orientation parameter, producing the default north-up
	 * reference map (Req 37/B: zero orientation assumed when parameter is omitted).
	 * @param landingPageUrl the API landing page URL
	 * @return the default map URL, or a NOT_FOUND: prefixed string if the map endpoint
	 * cannot be resolved
	 */
	public static String buildDefaultOrientationMapUrl(String landingPageUrl) {
		String mapUrl = findMapUrl(landingPageUrl);
		if (mapUrl == null) {
			return NOT_FOUND_PREFIX + landingPageUrl + "/map";
		}
		String separator = mapUrl.contains("?") ? "&" : "?";
		return mapUrl + separator + "f=png";
	}

	/**
	 * Builds a map URL with {@code orientation=45}, requesting the map content to be
	 * rotated 45° counterclockwise (Req 37/A). The human tester confirms this map appears
	 * visually rotated relative to the default north-up map.
	 * @param landingPageUrl the API landing page URL
	 * @return the rotated map URL, or a NOT_FOUND: prefixed string if the map endpoint
	 * cannot be resolved
	 */
	public static String buildRotatedMapUrl(String landingPageUrl) {
		String mapUrl = findMapUrl(landingPageUrl);
		if (mapUrl == null) {
			return NOT_FOUND_PREFIX + landingPageUrl + "/map";
		}
		String separator = mapUrl.contains("?") ? "&" : "?";
		return mapUrl + separator + "orientation=45&f=png";
	}

	/**
	 * Builds a map URL with {@code orientation=0}, the explicit north-up reference (Req
	 * 37/B). The human tester confirms this map appears visually identical to the default
	 * map (no orientation parameter).
	 * @param landingPageUrl the API landing page URL
	 * @return the orientation=0 map URL, or a NOT_FOUND: prefixed string if the map
	 * endpoint cannot be resolved
	 */
	public static String buildZeroOrientationMapUrl(String landingPageUrl) {
		String mapUrl = findMapUrl(landingPageUrl);
		if (mapUrl == null) {
			return NOT_FOUND_PREFIX + landingPageUrl + "/map";
		}
		String separator = mapUrl.contains("?") ? "&" : "?";
		return mapUrl + separator + "orientation=0&f=png";
	}

	/**
	 * Builds a map URL combining {@code orientation=45} with a global
	 * {@code bbox=-180,-90,180,90} (Req 37/C and 37/D). The human tester confirms the
	 * rotation pivot is the geographic center of the bbox, and that no empty corners
	 * appear in the output.
	 * @param landingPageUrl the API landing page URL
	 * @return the orientation+bbox map URL, or a NOT_FOUND: prefixed string if the map
	 * endpoint cannot be resolved
	 */
	public static String buildBboxOrientationMapUrl(String landingPageUrl) {
		String mapUrl = findMapUrl(landingPageUrl);
		if (mapUrl == null) {
			return NOT_FOUND_PREFIX + landingPageUrl + "/map";
		}
		String separator = mapUrl.contains("?") ? "&" : "?";
		return mapUrl + separator + "orientation=45&bbox=-180,-90,180,90&f=png";
	}

	/**
	 * Returns {@code "true"} if the server declares the spatial-subsetting conformance
	 * class, {@code "false"} otherwise. Used by CTL scripts to decide whether to show the
	 * conditional bbox-orientation forms (rows 9 and 11).
	 * @param landingPageUrl the API landing page URL
	 * @return {@code "true"} or {@code "false"}
	 */
	@SuppressWarnings("unchecked")
	public static String isSubsettingSupported(String landingPageUrl) {
		try {
			Map<String, Object> conf = fetchJson(landingPageUrl + "/conformance?f=json");
			if (conf == null) {
				return "false";
			}
			List<String> classes = (List<String>) conf.get("conformsTo");
			if (classes == null) {
				return "false";
			}
			String target = normalizeScheme("https://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/spatial-subsetting");
			for (String c : classes) {
				if (normalizeScheme(c).equals(target)) {
					return "true";
				}
			}
		}
		catch (Exception e) {
			// return false
		}
		return "false";
	}

	/**
	 * Returns {@code "true"} if the server declares the orientation conformance class,
	 * {@code "false"} otherwise. Used by CTL scripts to suppress the orientation
	 * interactive forms when the server does not claim orientation support.
	 * @param landingPageUrl the API landing page URL
	 * @return {@code "true"} or {@code "false"}
	 */
	@SuppressWarnings("unchecked")
	public static String isOrientationSupported(String landingPageUrl) {
		try {
			Map<String, Object> conf = fetchJson(landingPageUrl + "/conformance?f=json");
			if (conf == null) {
				return "false";
			}
			List<String> classes = (List<String>) conf.get("conformsTo");
			if (classes == null) {
				return "false";
			}
			String target = normalizeScheme("https://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/orientation");
			for (String c : classes) {
				if (normalizeScheme(c).equals(target)) {
					return "true";
				}
			}
		}
		catch (Exception e) {
			// return false
		}
		return "false";
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
