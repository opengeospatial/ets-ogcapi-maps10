package org.opengis.cite.ogcapimaps10.util.interactive;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility methods for building map request URLs used in the interactive (manual)
 * verification of Abstract Test A.36 Req 36/A (collections filter) and Req 36/B (draw
 * order).
 *
 * <p>
 * Two comparisons are produced for side-by-side review by the human tester:
 * </p>
 * <ul>
 * <li>Filter verification (Req 36/A): filtered map ({@code ?collections=id1}) vs default
 * map (no parameter) — tester confirms the server restricts rendering to the specified
 * collection.</li>
 * <li>Draw order verification (Req 36/B): forward-order map
 * ({@code ?collections=id1,id2}) vs reverse-order map ({@code ?collections=id2,id1}) —
 * tester confirms the leftmost collection renders at the bottom.</li>
 * </ul>
 */
public final class CollectionsResponseInteractiveTestUtils {

	/**
	 * Sentinel prefix returned when no valid dataset map URL or collection IDs can be
	 * found. Allows CTL scripts to detect and display a warning to the tester.
	 */
	public static final String NOT_FOUND_PREFIX = "NOT_FOUND:";

	private static final String MAP_REL_HTTPS = "https://www.opengis.net/def/rel/ogc/1.0/map";

	private static final String MAP_REL_HTTP = "http://www.opengis.net/def/rel/ogc/1.0/map";

	private static final String MAP_REL_COMPACT = "ogc-rel:map";

	private static final String COVERAGE_REL = "http://www.opengis.net/def/rel/ogc/1.0/coverage";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private CollectionsResponseInteractiveTestUtils() {
	}

	/**
	 * Builds the filtered map URL for Req 36/A verification: the dataset map endpoint
	 * with {@code ?collections=id1}, where {@code id1} is the first discovered collection
	 * identifier.
	 * @param landingPageUrl the API landing page URL
	 * @return the filtered map URL, or a NOT_FOUND: prefixed string if discovery fails
	 */
	public static String buildFilteredMapUrl(String landingPageUrl) {
		DiscoveryResult discovery = discover(landingPageUrl);
		if (discovery == null || discovery.datasetMapUrl == null || discovery.collectionId1 == null) {
			return NOT_FOUND_PREFIX + landingPageUrl;
		}
		String sep = discovery.datasetMapUrl.contains("?") ? "&" : "?";
		return discovery.datasetMapUrl + sep + "collections=" + discovery.collectionId1;
	}

	/**
	 * Builds the default map URL for Req 36/A verification: the dataset map endpoint with
	 * no {@code collections} parameter.
	 * @param landingPageUrl the API landing page URL
	 * @return the default map URL, or a NOT_FOUND: prefixed string if discovery fails
	 */
	public static String buildDefaultMapUrl(String landingPageUrl) {
		DiscoveryResult discovery = discover(landingPageUrl);
		if (discovery == null || discovery.datasetMapUrl == null) {
			return NOT_FOUND_PREFIX + landingPageUrl;
		}
		return discovery.datasetMapUrl;
	}

	/**
	 * Builds the forward-order map URL for Req 36/B verification: the dataset map
	 * endpoint with {@code ?collections=id1,id2}.
	 * @param landingPageUrl the API landing page URL
	 * @return the forward-order map URL, or a NOT_FOUND: prefixed string if discovery
	 * fails or fewer than 2 collections are available
	 */
	public static String buildForwardOrderMapUrl(String landingPageUrl) {
		DiscoveryResult discovery = discover(landingPageUrl);
		if (discovery == null || discovery.datasetMapUrl == null || discovery.collectionId1 == null
				|| discovery.collectionId2 == null) {
			return NOT_FOUND_PREFIX + landingPageUrl;
		}
		String sep = discovery.datasetMapUrl.contains("?") ? "&" : "?";
		return discovery.datasetMapUrl + sep + "collections=" + discovery.collectionId1 + "," + discovery.collectionId2;
	}

	/**
	 * Builds the reverse-order map URL for Req 36/B verification: the dataset map
	 * endpoint with {@code ?collections=id2,id1}.
	 * @param landingPageUrl the API landing page URL
	 * @return the reverse-order map URL, or a NOT_FOUND: prefixed string if discovery
	 * fails or fewer than 2 collections are available
	 */
	public static String buildReverseOrderMapUrl(String landingPageUrl) {
		DiscoveryResult discovery = discover(landingPageUrl);
		if (discovery == null || discovery.datasetMapUrl == null || discovery.collectionId1 == null
				|| discovery.collectionId2 == null) {
			return NOT_FOUND_PREFIX + landingPageUrl;
		}
		String sep = discovery.datasetMapUrl.contains("?") ? "&" : "?";
		return discovery.datasetMapUrl + sep + "collections=" + discovery.collectionId2 + "," + discovery.collectionId1;
	}

	// -------------------------------------------------------------------------
	// Private discovery helpers
	// -------------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	private static DiscoveryResult discover(String landingPageUrl) {
		try {
			String base = landingPageUrl.endsWith("/") ? landingPageUrl.substring(0, landingPageUrl.length() - 1)
					: landingPageUrl;

			// Find dataset map URL from landing page links
			Map<String, Object> landingPage = fetchJson(base);
			if (landingPage == null) {
				return null;
			}
			List<Object> links = (List<Object>) landingPage.get("links");
			String datasetMapUrl = null;
			if (links != null) {
				for (Object linkObj : links) {
					if (!(linkObj instanceof Map)) {
						continue;
					}
					Map<String, Object> link = (Map<String, Object>) linkObj;
					String rel = (String) link.get("rel");
					if (MAP_REL_HTTPS.equals(rel) || MAP_REL_HTTP.equals(rel) || MAP_REL_COMPACT.equals(rel)) {
						String href = (String) link.get("href");
						if (href != null && !href.isEmpty()) {
							datasetMapUrl = resolveUrl(landingPageUrl, href);
							break;
						}
					}
				}
			}
			if (datasetMapUrl == null) {
				return null;
			}

			// Find two collection IDs suitable for draw-order testing.
			// Two fully opaque rasters produce identical visuals regardless of order,
			// making Req 36/B untestable. Prefer collections that have a map link but
			// no coverage link (vector/feature). Raster collections (coverage link) are
			// accepted only as a last resort, and at most one in the final pair.
			Map<String, Object> collectionsDoc = fetchJson(base + "/collections?f=json");
			List<String> preferredIds = new ArrayList<>();
			List<String> fallbackIds = new ArrayList<>();
			if (collectionsDoc != null) {
				List<Object> collections = (List<Object>) collectionsDoc.get("collections");
				if (collections != null) {
					for (Object collObj : collections) {
						if (!(collObj instanceof Map)) {
							continue;
						}
						Map<String, Object> coll = (Map<String, Object>) collObj;
						String id = (String) coll.get("id");
						if (id == null || id.isEmpty()) {
							continue;
						}
						List<Object> collLinks = (List<Object>) coll.get("links");
						boolean hasMapLink = false;
						boolean hasCoverageLink = false;
						if (collLinks != null) {
							for (Object linkObj : collLinks) {
								if (!(linkObj instanceof Map)) {
									continue;
								}
								String rel = (String) ((Map<String, Object>) linkObj).get("rel");
								if (MAP_REL_HTTPS.equals(rel) || MAP_REL_HTTP.equals(rel)
										|| MAP_REL_COMPACT.equals(rel)) {
									hasMapLink = true;
								}
								if (COVERAGE_REL.equals(rel)) {
									hasCoverageLink = true;
								}
							}
						}
						if (!hasMapLink) {
							continue;
						}
						if (!hasCoverageLink) {
							preferredIds.add(id);
						}
						else {
							fallbackIds.add(id);
						}
						if (preferredIds.size() >= 2) {
							break;
						}
					}
				}
			}
			List<String> candidates = new ArrayList<>(preferredIds);
			candidates.addAll(fallbackIds);
			String collectionId1 = candidates.size() > 0 ? candidates.get(0) : null;
			String collectionId2 = candidates.size() > 1 ? candidates.get(1) : null;

			return new DiscoveryResult(datasetMapUrl, collectionId1, collectionId2);
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
			conn.setConnectTimeout(30000);
			conn.setReadTimeout(30000);
			conn.setInstanceFollowRedirects(true);
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

	private static String resolveUrl(String baseUrl, String href) {
		if (href.startsWith("http://") || href.startsWith("https://")) {
			return href;
		}
		try {
			return URI.create(baseUrl).resolve(href).toString();
		}
		catch (Exception e) {
			return href;
		}
	}

	// -------------------------------------------------------------------------
	// Inner class
	// -------------------------------------------------------------------------

	private static class DiscoveryResult {

		final String datasetMapUrl;

		final String collectionId1;

		final String collectionId2;

		DiscoveryResult(String datasetMapUrl, String collectionId1, String collectionId2) {
			this.datasetMapUrl = datasetMapUrl;
			this.collectionId1 = collectionId1;
			this.collectionId2 = collectionId2;
		}

	}

}
