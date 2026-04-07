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
	 * Builds both the natural-order and reversed-order dataset map URLs in a single
	 * discovery pass and returns them as a pipe-delimited string:
	 * {@code "{urlA}|{urlB}"}.
	 *
	 * <p>
	 * urlA uses {@code ?collections=id1,id2} (natural order, Map A). urlB uses
	 * {@code ?collections=id2,id1} (reversed order, Map B). Either part may be prefixed
	 * with {@link #NOT_FOUND_PREFIX} if discovery fails.
	 *
	 * <p>
	 * Performing discovery once avoids independent HTTP calls for Map A and Map B that
	 * could produce asymmetric results if a second network call fails.
	 * @param landingPageUrl the API landing page URL
	 * @return pipe-delimited string {@code "urlA|urlB"}
	 */
	public static String buildBothOrderCollectionsUrls(String landingPageUrl) {
		String mapUrl = findMapUrl(landingPageUrl);
		if (mapUrl == null) {
			String notFound = NOT_FOUND_PREFIX + landingPageUrl;
			return notFound + "|" + notFound;
		}

		List<String> ids = findTwoRenderableCollectionIds(landingPageUrl);
		if (ids == null || ids.size() < 2) {
			String notFound = NOT_FOUND_PREFIX + mapUrl;
			return notFound + "|" + notFound;
		}

		String sep = mapUrl.contains("?") ? "&" : "?";
		String urlA = mapUrl + sep + "collections=" + ids.get(0) + "," + ids.get(1) + "&f=png&width=800&height=400";
		String urlB = mapUrl + sep + "collections=" + ids.get(1) + "," + ids.get(0) + "&f=png&width=800&height=400";
		return urlA + "|" + urlB;
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
			String base = landingPageUrl.endsWith("/") ? landingPageUrl.substring(0, landingPageUrl.length() - 1)
					: landingPageUrl;
			// Append ?f=json so the server returns JSON even when HTML is the default
			// (consistent with CollectionsResponseTest.findDatasetMapUrl)
			Map<String, Object> landingPage = fetchJson(base + "?f=json");
			if (landingPage != null) {
				List<Map<String, Object>> links = (List<Map<String, Object>>) landingPage.get("links");
				if (links != null) {
					for (Map<String, Object> link : links) {
						String rel = (String) link.get("rel");
						if (rel != null && matchesRelIgnoringScheme(rel, REL_MAP)) {
							String href = (String) link.get("href");
							String resolved = resolveUrl(base, href);
							if (resolved != null) {
								return resolved;
							}
						}
					}
				}
			}
		}
		catch (Exception e) {
			System.err
				.println("[A.12/InteractiveTestUtils] findMapUrl failed for " + landingPageUrl + ": " + e.getMessage());
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
		String base = landingPageUrl.endsWith("/") ? landingPageUrl.substring(0, landingPageUrl.length() - 1)
				: landingPageUrl;
		String collectionsUrl = base + "/collections?f=json";
		try {
			Map<String, Object> collectionsResponse = fetchJson(collectionsUrl);
			if (collectionsResponse == null) {
				System.err.println("[A.12/InteractiveTestUtils] fetchJson returned null for: " + collectionsUrl);
				return null;
			}

			List<Map<String, Object>> collections = (List<Map<String, Object>>) collectionsResponse.get("collections");
			if (collections == null) {
				System.err
					.println("[A.12/InteractiveTestUtils] No 'collections' key in response from: " + collectionsUrl);
				return null;
			}
			if (collections.size() < 2) {
				System.err.println("[A.12/InteractiveTestUtils] Fewer than 2 collections returned from: "
						+ collectionsUrl + " (got " + collections.size() + ")");
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
				System.err.println("[A.12/InteractiveTestUtils] Fewer than 2 leaf collections after parent "
						+ "filtering (allIds=" + allIds.size() + ", leafIds=" + leafIds.size() + ")");
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
			System.err.println("[A.12/InteractiveTestUtils] Exception discovering collections from " + collectionsUrl
					+ ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
			return null;
		}
	}

	private static Map<String, Object> fetchJson(String urlString) {
		try {
			HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");
			conn.setConnectTimeout(10000);
			conn.setReadTimeout(10000);
			int status = conn.getResponseCode();
			if (status == HttpURLConnection.HTTP_OK) {
				try (InputStream is = conn.getInputStream()) {
					return OBJECT_MAPPER.readValue(is, new TypeReference<Map<String, Object>>() {
					});
				}
			}
			System.err.println("[A.12/InteractiveTestUtils] fetchJson got HTTP " + status + " for: " + urlString);
		}
		catch (Exception e) {
			System.err
				.println("[A.12/InteractiveTestUtils] fetchJson exception for: " + urlString + " => " + e.getMessage());
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