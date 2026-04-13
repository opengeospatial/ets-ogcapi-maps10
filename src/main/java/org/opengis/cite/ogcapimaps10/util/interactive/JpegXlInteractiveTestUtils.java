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
 * Utility methods for interactive CTL tests related to JPEG XL map content verification.
 * These methods are called from CTL scripts (interactive-jpegxl.xml) to build map request
 * URLs for manual verification of Part B (color representation) and Part C (portrayal
 * consistency).
 */
public final class JpegXlInteractiveTestUtils {

	private static final String REL_MAP = "https://www.opengis.net/def/rel/ogc/1.0/map";

	/**
	 * Prefix added to URLs when no valid map URL could be found. This allows CTL scripts
	 * to detect and display an appropriate warning message.
	 */
	public static final String NOT_FOUND_PREFIX = "NOT_FOUND:";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private JpegXlInteractiveTestUtils() {
		// Utility class, no instantiation
	}

	/**
	 * Builds a map request URL for JPEG XL content verification (Part B). Returns the map
	 * URL that can be used to request the map as JPEG XL.
	 * @param landingPageUrl The landing page URL of the implementation under test.
	 * @return The map URL, or a NOT_FOUND prefixed fallback URL.
	 */
	public static String buildMapRequestAsJpegXl(String landingPageUrl) {
		String mapUrl = findMapUrl(landingPageUrl);
		if (mapUrl == null) {
			return NOT_FOUND_PREFIX + landingPageUrl + "/map";
		}
		String separator = mapUrl.contains("?") ? "&" : "?";
		return mapUrl + separator + "f=jxl";
	}

	/**
	 * Builds a second map request URL with a different bbox for Part C portrayal
	 * consistency comparison. Appends a bbox parameter to request a different area of the
	 * same map resource.
	 * @param landingPageUrl The landing page URL of the implementation under test.
	 * @param bbox The bbox parameter value (e.g., "-5,40,5,50" for
	 * minLon,minLat,maxLon,maxLat).
	 * @return The map URL with bbox parameter, or a NOT_FOUND prefixed fallback URL.
	 */
	public static String buildMapRequestWithBbox(String landingPageUrl, String bbox) {
		String mapUrl = findMapUrl(landingPageUrl);
		if (mapUrl == null) {
			return NOT_FOUND_PREFIX + landingPageUrl + "/map?f=jxl&bbox=" + bbox;
		}
		String separator = mapUrl.contains("?") ? "&" : "?";
		return mapUrl + separator + "f=jxl&bbox=" + bbox;
	}

	/**
	 * Builds a map request URL for the left-center area of the collection's spatial
	 * extent (centered at 35% of extent width). Used as Map 1 for Part C portrayal
	 * consistency comparison. The bbox has the same size as
	 * {@link #buildMapRequestRightBbox(String)} so that both maps are at the same
	 * scale-denominator, with ~40% overlap for easier visual comparison.
	 * @param landingPageUrl The landing page URL of the implementation under test.
	 * @return The map URL with left-center bbox, or a NOT_FOUND prefixed fallback URL.
	 */
	public static String buildMapRequestLeftBbox(String landingPageUrl) {
		double[] bbox = findCollectionBbox(landingPageUrl);
		if (bbox == null) {
			return buildMapRequestWithBbox(landingPageUrl, "-67.5,-22.5,-22.5,22.5");
		}
		double width = bbox[2] - bbox[0];
		double height = bbox[3] - bbox[1];
		double quarterWidth = width / 4.0;
		double quarterHeight = height / 4.0;
		// Center of the left portion (35% of extent width for ~40% overlap with right)
		double leftCenterLon = bbox[0] + 0.35 * width;
		double centerLat = (bbox[1] + bbox[3]) / 2.0;
		String leftBbox = (leftCenterLon - quarterWidth) + "," + (centerLat - quarterHeight) + ","
				+ (leftCenterLon + quarterWidth) + "," + (centerLat + quarterHeight);
		return buildMapRequestWithBbox(landingPageUrl, leftBbox);
	}

	/**
	 * Builds a map request URL for the right-center area of the collection's spatial
	 * extent (centered at 65% of extent width). Used as Map 2 for Part C portrayal
	 * consistency comparison. The bbox has the same size as
	 * {@link #buildMapRequestLeftBbox(String)} so that both maps are at the same
	 * scale-denominator, with ~40% overlap for easier visual comparison.
	 * @param landingPageUrl The landing page URL of the implementation under test.
	 * @return The map URL with right-center bbox, or a NOT_FOUND prefixed fallback URL.
	 */
	public static String buildMapRequestRightBbox(String landingPageUrl) {
		double[] bbox = findCollectionBbox(landingPageUrl);
		if (bbox == null) {
			return buildMapRequestWithBbox(landingPageUrl, "22.5,-22.5,67.5,22.5");
		}
		double width = bbox[2] - bbox[0];
		double height = bbox[3] - bbox[1];
		double quarterWidth = width / 4.0;
		double quarterHeight = height / 4.0;
		// Center of the right portion (65% of extent width for ~40% overlap with left)
		double rightCenterLon = bbox[0] + 0.65 * width;
		double centerLat = (bbox[1] + bbox[3]) / 2.0;
		String rightBbox = (rightCenterLon - quarterWidth) + "," + (centerLat - quarterHeight) + ","
				+ (rightCenterLon + quarterWidth) + "," + (centerLat + quarterHeight);
		return buildMapRequestWithBbox(landingPageUrl, rightBbox);
	}

	/**
	 * Finds the spatial bbox from the first collection that has a map link. Reads
	 * extent.spatial.bbox from the collection metadata.
	 * @param landingPageUrl The landing page URL.
	 * @return A double array [minLon, minLat, maxLon, maxLat], or null if not found.
	 */
	@SuppressWarnings("unchecked")
	private static double[] findCollectionBbox(String landingPageUrl) {
		try {
			String collectionsUrl = landingPageUrl.endsWith("/") ? landingPageUrl + "collections"
					: landingPageUrl + "/collections";
			Map<String, Object> collectionsData = fetchJson(collectionsUrl);
			if (collectionsData == null) {
				return null;
			}
			List<Map<String, Object>> collections = (List<Map<String, Object>>) collectionsData.get("collections");
			if (collections == null) {
				return null;
			}
			for (Map<String, Object> collection : collections) {
				List<Map<String, Object>> links = (List<Map<String, Object>>) collection.get("links");
				if (findLinkHrefByRel(links, REL_MAP) == null) {
					continue;
				}
				Map<String, Object> extent = (Map<String, Object>) collection.get("extent");
				if (extent == null) {
					continue;
				}
				Map<String, Object> spatial = (Map<String, Object>) extent.get("spatial");
				if (spatial == null) {
					continue;
				}
				List<List<Number>> bboxList = (List<List<Number>>) spatial.get("bbox");
				if (bboxList == null || bboxList.isEmpty()) {
					continue;
				}
				List<Number> firstBbox = bboxList.get(0);
				if (firstBbox != null && firstBbox.size() >= 4) {
					return new double[] { firstBbox.get(0).doubleValue(), firstBbox.get(1).doubleValue(),
							firstBbox.get(2).doubleValue(), firstBbox.get(3).doubleValue() };
				}
			}
		}
		catch (Exception e) {
			// Failed to find collection bbox
		}
		return null;
	}

	/**
	 * Finds the map resource URL from a landing page or its collections.
	 * @param landingPageUrl The landing page URL.
	 * @return The map URL, or null if not found.
	 */
	@SuppressWarnings("unchecked")
	private static String findMapUrl(String landingPageUrl) {
		try {
			Map<String, Object> data = fetchJson(landingPageUrl);
			if (data == null) {
				return null;
			}

			// 1. Check landing page links for [ogc-rel:map]
			List<Map<String, Object>> links = (List<Map<String, Object>>) data.get("links");
			String mapHref = findLinkHrefByRel(links, REL_MAP);
			if (mapHref != null) {
				return resolveUrl(landingPageUrl, mapHref);
			}

			// 2. Check collections for [ogc-rel:map]
			String collectionsUrl = landingPageUrl.endsWith("/") ? landingPageUrl + "collections"
					: landingPageUrl + "/collections";
			Map<String, Object> collectionsData = fetchJson(collectionsUrl);
			if (collectionsData != null) {
				List<Map<String, Object>> collections = (List<Map<String, Object>>) collectionsData.get("collections");
				if (collections != null) {
					for (Map<String, Object> collection : collections) {
						List<Map<String, Object>> collectionLinks = (List<Map<String, Object>>) collection.get("links");
						mapHref = findLinkHrefByRel(collectionLinks, REL_MAP);
						if (mapHref != null) {
							return resolveUrl(collectionsUrl, mapHref);
						}
					}
				}
			}
		}
		catch (Exception e) {
			// Failed to find map URL
		}
		return null;
	}

	/**
	 * Finds the href of a link matching the given rel value.
	 * @param links The list of link objects.
	 * @param rel The expected rel value.
	 * @return The href string, or null if not found.
	 */
	private static String findLinkHrefByRel(List<Map<String, Object>> links, String rel) {
		if (links == null) {
			return null;
		}
		for (Map<String, Object> link : links) {
			String linkRel = (String) link.get("rel");
			if (linkRel != null && matchesRelIgnoringScheme(linkRel, rel)) {
				return (String) link.get("href");
			}
		}
		return null;
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

	/**
	 * Resolves a potentially relative URL against a base URL.
	 * @param baseUrl The base URL.
	 * @param url The URL to resolve (may be relative or absolute).
	 * @return The resolved absolute URL.
	 */
	private static String resolveUrl(String baseUrl, String url) {
		if (url == null) {
			return null;
		}
		if (url.startsWith("http://") || url.startsWith("https://")) {
			return url;
		}
		try {
			URI base = URI.create(baseUrl);
			return base.resolve(url).toString();
		}
		catch (Exception e) {
			if (url.startsWith("/")) {
				try {
					URI base = URI.create(baseUrl);
					return base.getScheme() + "://" + base.getAuthority() + url;
				}
				catch (Exception ex) {
					return url;
				}
			}
			return url;
		}
	}

	/**
	 * Fetches JSON from a URL.
	 * @param urlString The URL to fetch.
	 * @return The parsed JSON as a Map, or null if failed.
	 */
	private static Map<String, Object> fetchJson(String urlString) {
		try {
			URL url = URI.create(urlString).toURL();
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Accept", "application/json");
			connection.setConnectTimeout(10000);
			connection.setReadTimeout(10000);

			if (connection.getResponseCode() == 200) {
				try (InputStream is = connection.getInputStream()) {
					return OBJECT_MAPPER.readValue(is, new TypeReference<Map<String, Object>>() {
					});
				}
			}
		}
		catch (Exception e) {
			// Failed to fetch JSON
		}
		return null;
	}

}
