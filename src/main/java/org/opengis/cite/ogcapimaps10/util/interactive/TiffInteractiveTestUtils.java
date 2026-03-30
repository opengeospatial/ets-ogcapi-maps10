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
 * Utility methods for interactive CTL tests related to TIFF map content verification.
 * These methods are called from CTL scripts (interactive-tiff.xml) to build map request
 * URLs for manual verification of Part B (palette/RGB color representation) and Part C
 * (portrayal consistency).
 */
public final class TiffInteractiveTestUtils {

	private static final String REL_MAP = "https://www.opengis.net/def/rel/ogc/1.0/map";

	/**
	 * Prefix added to URLs when no valid map URL could be found. This allows CTL scripts
	 * to detect and display an appropriate warning message.
	 */
	public static final String NOT_FOUND_PREFIX = "NOT_FOUND:";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private TiffInteractiveTestUtils() {
		// Utility class, no instantiation
	}

	/**
	 * Builds a map request URL with a bbox parameter for TIFF format. Appends f=tiff and
	 * bbox parameters to request a specific area of the map resource.
	 * @param landingPageUrl The landing page URL of the implementation under test.
	 * @param bbox The bbox parameter value (e.g., "-5,40,5,50" for
	 * minLon,minLat,maxLon,maxLat).
	 * @return The map URL with bbox parameter, or a NOT_FOUND prefixed fallback URL.
	 */
	public static String buildMapRequestWithBbox(String landingPageUrl, String bbox) {
		String mapUrl = findMapUrl(landingPageUrl);
		if (mapUrl == null) {
			return NOT_FOUND_PREFIX + landingPageUrl + "/map?f=tiff&bbox=" + bbox;
		}
		String withTiff = mapUrl.contains("f=tiff") ? mapUrl : mapUrl + (mapUrl.contains("?") ? "&" : "?") + "f=tiff";
		return withTiff + "&bbox=" + bbox;
	}

	/**
	 * Builds a map request URL for a small area around the center of the collection's
	 * spatial extent (25% of extent dimensions). Used as the inner/smaller map for Part C
	 * portrayal consistency comparison.
	 * @param landingPageUrl The landing page URL of the implementation under test.
	 * @return The map URL with small bbox, or a NOT_FOUND prefixed fallback URL.
	 */
	public static String buildMapRequestSmallBbox(String landingPageUrl) {
		double[] bbox = findCollectionBbox(landingPageUrl);
		if (bbox == null) {
			return buildMapRequestWithBbox(landingPageUrl, "-45,-22.5,45,22.5");
		}
		double centerLon = (bbox[0] + bbox[2]) / 2.0;
		double centerLat = (bbox[1] + bbox[3]) / 2.0;
		double quarterWidth = (bbox[2] - bbox[0]) / 4.0;
		double quarterHeight = (bbox[3] - bbox[1]) / 4.0;
		String smallBbox = (centerLon - quarterWidth) + "," + (centerLat - quarterHeight) + ","
				+ (centerLon + quarterWidth) + "," + (centerLat + quarterHeight);
		return buildMapRequestWithBbox(landingPageUrl, smallBbox);
	}

	/**
	 * Builds a map request URL for a large area around the center of the collection's
	 * spatial extent (50% of extent dimensions). The small bbox from
	 * {@link #buildMapRequestSmallBbox} is fully contained within this bbox. Used as the
	 * outer/larger map for Part C portrayal consistency comparison.
	 * @param landingPageUrl The landing page URL of the implementation under test.
	 * @return The map URL with large bbox, or a NOT_FOUND prefixed fallback URL.
	 */
	public static String buildMapRequestLargeBbox(String landingPageUrl) {
		double[] bbox = findCollectionBbox(landingPageUrl);
		if (bbox == null) {
			return buildMapRequestWithBbox(landingPageUrl, "-90,-45,90,45");
		}
		double centerLon = (bbox[0] + bbox[2]) / 2.0;
		double centerLat = (bbox[1] + bbox[3]) / 2.0;
		double halfWidth = (bbox[2] - bbox[0]) / 2.0;
		double halfHeight = (bbox[3] - bbox[1]) / 2.0;
		String largeBbox = (centerLon - halfWidth) + "," + (centerLat - halfHeight) + "," + (centerLon + halfWidth)
				+ "," + (centerLat + halfHeight);
		return buildMapRequestWithBbox(landingPageUrl, largeBbox);
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
