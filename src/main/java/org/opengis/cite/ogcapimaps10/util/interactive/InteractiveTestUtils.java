package org.opengis.cite.ogcapimaps10.util.interactive;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	 * Builds a dataset map URL using two renderable leaf collection IDs in their selected
	 * order: {@code ?collections=id1,id2}.
	 *
	 * <p>
	 * Parent containers (collections whose ID is a prefix of another collection's ID) are
	 * excluded because they may render nothing. A raster + non-raster pair is preferred
	 * to maximise visual distinctiveness when the order is reversed.
	 *
	 * <p>
	 * Used to display Map A in the interactive rendering-order verification form.
	 * @param landingPageUrl the API landing page URL
	 * @return the map URL with collections in natural order, or a NOT_FOUND: prefixed
	 * string if the map endpoint or two renderable leaf collections cannot be resolved
	 */
	public static String buildFirstOrderCollectionsUrl(String landingPageUrl) {
		return buildCollectionsUrl(landingPageUrl, false);
	}

	/**
	 * Builds a dataset map URL using two renderable leaf collection IDs in reversed
	 * order: {@code ?collections=id2,id1}.
	 *
	 * <p>
	 * Same collection-selection logic as {@link #buildFirstOrderCollectionsUrl}.
	 * Reversing the order changes which layer is painted last and should produce a
	 * visually different map if the server honours Req 12B.
	 *
	 * <p>
	 * Used to display Map B in the interactive rendering-order verification form.
	 * @param landingPageUrl the API landing page URL
	 * @return the map URL with collections in reversed order, or a NOT_FOUND: prefixed
	 * string if the map endpoint or two renderable leaf collections cannot be resolved
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

		List<String> ids = findTwoRenderableCollectionIds(landingPageUrl);
		if (ids == null || ids.size() < 2) {
			return NOT_FOUND_PREFIX + mapUrl;
		}

		// Reversed: id2,id1 — natural: id1,id2
		// Raw concatenation: colons in namespaced IDs (e.g. NaturalEarth:cultural:...)
		// and commas between IDs are valid query-string characters (RFC 3986
		// sub-delimiters) and must NOT be percent-encoded.
		String collections = reversed ? ids.get(1) + "," + ids.get(0) : ids.get(0) + "," + ids.get(1);
		String separator = mapUrl.contains("?") ? "&" : "?";
		return mapUrl + separator + "collections=" + collections + "&f=png&width=800&height=400";
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
	 * Discovers two renderable leaf collection IDs from {landingPageUrl}/collections.
	 * Parent containers are excluded: a collection is a parent if any other collection's
	 * ID starts with {@code {id}:}. Among the leaf collections, a raster + non-raster
	 * pair is preferred so that reversing the order produces a visually distinct result.
	 * @param landingPageUrl the API landing page URL
	 * @return a list of exactly two leaf collection IDs, or {@code null} if fewer than
	 * two leaf collections are available
	 */
	@SuppressWarnings("unchecked")
	private static List<String> findTwoRenderableCollectionIds(String landingPageUrl) {
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

			// Collect all IDs preserving encounter order
			Set<String> allIds = new LinkedHashSet<>();
			for (Map<String, Object> c : collections) {
				String id = (String) c.get("id");
				if (id != null) {
					allIds.add(id);
				}
			}

			// Filter out parent containers: a collection is a parent if any other
			// collection's ID starts with "{id}:"
			List<String> leafIds = new ArrayList<>();
			for (String id : allIds) {
				boolean isParent = false;
				for (String other : allIds) {
					if (other.startsWith(id + ":")) {
						isParent = true;
						break;
					}
				}
				if (!isParent) {
					leafIds.add(id);
				}
			}

			if (leafIds.size() < 2) {
				return null;
			}

			// Prefer a raster leaf + non-raster leaf pair for visual distinctiveness
			// when rendering order is reversed (one raster, one vector/cultural layer)
			String rasterLeaf = null;
			String otherLeaf = null;
			for (String id : leafIds) {
				if (rasterLeaf == null && id.toLowerCase().contains("raster")) {
					rasterLeaf = id;
				}
				else if (otherLeaf == null) {
					otherLeaf = id;
				}
				if (rasterLeaf != null && otherLeaf != null) {
					break;
				}
			}

			String id1;
			String id2;
			if (rasterLeaf != null && otherLeaf != null) {
				id1 = rasterLeaf;
				id2 = otherLeaf;
			}
			else {
				// No raster/non-raster split available; fall back to first two leaf IDs
				id1 = leafIds.get(0);
				id2 = leafIds.get(1);
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