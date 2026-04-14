package org.opengis.cite.ogcapimaps10.conformance.spatialSubsetting;

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.opengis.cite.ogcapimaps10.conformance.CommonFixture;
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Implements Abstract Test A.21: /conf/spatial-subsetting/bbox-definition
 *
 * <p>
 * Test Purpose: Verify that the implementation supports the {@code bbox} parameter
 * correctly for map requests (Requirement 21: /req/spatial-subsetting/bbox-definition).
 *
 * <p>
 * Assertions verified:
 * <ul>
 * <li>[Req21/bbox-2d] The server returns HTTP 200 for a 2D bbox (four floats,
 * comma-separated) without an explicit bbox-crs (CRS84 assumed).</li>
 * <li>[Req21/bbox-2d-explicit-crs] The server returns HTTP 200 for a 2D bbox with an
 * explicit bbox-crs=[OGC:CRS84].</li>
 * <li>[Req21/bbox-3d] If a collection with a 3D spatial extent is found, the server
 * returns HTTP 200 or 204 for a 3D bbox (six floats) with bbox-crs=[OGC:CRS84h]. Skipped
 * when no 3D collection is discovered.</li>
 * <li>[Req21/bbox-center-conflict] The server returns HTTP 4xx when bbox and center are
 * combined.</li>
 * <li>[Req21/bbox-subset-lat-conflict] The server returns HTTP 4xx when bbox and
 * subset=Lat(...) are combined.</li>
 * <li>[Req21/bbox-subset-lon-conflict] The server returns HTTP 4xx when bbox and
 * subset=Lon(...) are combined.</li>
 * </ul>
 */
public class BBoxDefinitionTest extends CommonFixture {

	/** URL-encoded safe CURIE for CRS84: [OGC:CRS84] */
	private static final String CRS84_SAFE_CURIE = "%5BOGC%3ACRS84%5D";

	/** URL-encoded safe CURIE for CRS84h: [OGC:CRS84h] */
	private static final String CRS84H_SAFE_CURIE = "%5BOGC%3ACRS84h%5D";

	private static final String REL_MAP = "http://www.opengis.net/def/rel/ogc/1.0/map";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	/** Map URL discovered in {@link #discoverTargets}. */
	private String mapUrl;

	/** Query-string separator for {@link #mapUrl}. */
	private String sep;

	/**
	 * First collection with a 3D spatial extent discovered in {@link #discoverTargets};
	 * {@code null} when no such collection exists.
	 */
	private ThreeDEntry threeDEntry;

	/**
	 * Holds map URL and vertical bounds for a collection whose spatial extent is
	 * three-dimensional.
	 */
	private static class ThreeDEntry {

		final String mapUrl;

		final double minZ;

		final double maxZ;

		ThreeDEntry(String mapUrl, double minZ, double maxZ) {
			this.mapUrl = mapUrl;
			this.minZ = minZ;
			this.maxZ = maxZ;
		}

	}

	// -------------------------------------------------------------------------
	// Setup
	// -------------------------------------------------------------------------

	/**
	 * Discovers the dataset map endpoint and, separately, the first collection with a 3D
	 * spatial extent. Throws {@link SkipException} if the primary map endpoint is
	 * unreachable.
	 * @param testContext the TestNG test context
	 */
	@BeforeClass
	public void discoverTargets(ITestContext testContext) {
		String landingPageUrl = rootUri.toString();

		mapUrl = findMapUrl(landingPageUrl);
		if (mapUrl == null) {
			throw new SkipException("Could not find a dataset map endpoint (rel=ogc/1.0/map) from the landing page at "
					+ landingPageUrl + ". Skipping A.21 tests.");
		}
		sep = mapUrl.contains("?") ? "&" : "?";

		int reachabilityStatus = getStatusRaw(mapUrl + sep + "f=png");
		if (reachabilityStatus == -1) {
			throw new SkipException("Map endpoint unreachable (HTTP -1) at " + mapUrl + ". Skipping A.21 tests.");
		}

		threeDEntry = discoverThreeDCollection(landingPageUrl);
	}

	// -------------------------------------------------------------------------
	// Tests
	// -------------------------------------------------------------------------

	/**
	 * A.21 — Req 21/A (2D) and Req 21/B: verifies that the server accepts a 2D
	 * {@code bbox} and rejects forbidden combinations with {@code center} and
	 * {@code subset}.
	 */
	@Test(description = "A.21 Req 21/A (2D) and Req 21/B: bbox (4 floats) accepted, "
			+ "bbox+center and bbox+subset rejected with HTTP 4xx.")
	public void verifyBBoxDefinition() {
		List<String> errors = new ArrayList<>();

		// --- Assertion 21/A-2D: 2D bbox (4 floats), no explicit bbox-crs → HTTP 200
		int statusA2D = getStatusRaw(mapUrl + sep + "bbox=-180,-90,180,90&f=png");
		if (statusA2D != 200) {
			errors.add("[Req21/bbox-2d] Expected HTTP 200 for bbox=-180,-90,180,90 but got HTTP " + statusA2D + ".");
		}

		// --- Assertion 21/A-2D-explicit-crs: 2D bbox with explicit bbox-crs=[OGC:CRS84]
		// → HTTP 200
		int statusA2DCrs = getStatusRaw(mapUrl + sep + "bbox=-180,-90,180,90&bbox-crs=" + CRS84_SAFE_CURIE + "&f=png");
		if (statusA2DCrs != 200) {
			errors.add("[Req21/bbox-2d-explicit-crs] Expected HTTP 200 for bbox=-180,-90,180,90"
					+ "&bbox-crs=[OGC:CRS84] but got HTTP " + statusA2DCrs + ".");
		}

		// --- Assertion 21/B: bbox + center → HTTP 4xx
		int statusBCenter = getStatusRaw(mapUrl + sep + "bbox=-180,-90,180,90&center=0,0&f=png");
		if (!is4xx(statusBCenter)) {
			errors.add("[Req21/bbox-center-conflict] Expected HTTP 4xx when combining bbox and center"
					+ " but got HTTP " + statusBCenter + ".");
		}

		// --- Assertion 21/B: bbox + subset=Lat(-90:90) → HTTP 4xx
		int statusBSubsetLat = getStatusRaw(mapUrl + sep + "bbox=-180,-90,180,90&subset=Lat%28-90%3A90%29&f=png");
		if (!is4xx(statusBSubsetLat)) {
			errors.add("[Req21/bbox-subset-lat-conflict] Expected HTTP 4xx when combining bbox"
					+ " and subset=Lat(-90:90) but got HTTP " + statusBSubsetLat + ".");
		}

		// --- Assertion 21/B: bbox + subset=Lon(-180:180) → HTTP 4xx
		int statusBSubsetLon = getStatusRaw(mapUrl + sep + "bbox=-180,-90,180,90&subset=Lon%28-180%3A180%29&f=png");
		if (!is4xx(statusBSubsetLon)) {
			errors.add("[Req21/bbox-subset-lon-conflict] Expected HTTP 4xx when combining bbox"
					+ " and subset=Lon(-180:180) but got HTTP " + statusBSubsetLon + ".");
		}

		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.21 verifyBBoxDefinition failed:\n" + String.join("\n", errors));
		}
	}

	/**
	 * A.21 — Req 21/A (3D): verifies that the server accepts a 3D {@code bbox} (six
	 * floats) with {@code bbox-crs=[OGC:CRS84h]}.
	 *
	 * <p>
	 * Skipped when no collection with a 3D spatial extent is found on this server.
	 */
	@Test(description = "A.21 Req 21/A (3D): bbox (6 floats) with bbox-crs=[OGC:CRS84h] " + "returns HTTP 200 or 204.")
	public void verifyBBoxDefinition3D() {
		if (threeDEntry == null) {
			throw new SkipException("No collection with a 3D spatial extent found at " + rootUri + "/collections. "
					+ "Skipping 21/A-3D assertion.");
		}

		String bbox3D = "-180,-90," + formatZ(threeDEntry.minZ) + ",180,90," + formatZ(threeDEntry.maxZ);
		String threeDSep = threeDEntry.mapUrl.contains("?") ? "&" : "?";
		int statusA3D = getStatusRaw(
				threeDEntry.mapUrl + threeDSep + "bbox=" + bbox3D + "&bbox-crs=" + CRS84H_SAFE_CURIE + "&f=png");

		if (statusA3D != 200 && statusA3D != 204) {
			throw new AssertionError(
					"A.21 verifyBBoxDefinition3D failed:\n" + "[Req21/bbox-3d] Expected HTTP 200 or 204 for 3D bbox="
							+ bbox3D + "&bbox-crs=[OGC:CRS84h] but got HTTP " + statusA3D + ".");
		}
	}

	// -------------------------------------------------------------------------
	// Private helpers
	// -------------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	private String findMapUrl(String landingPageUrl) {
		try {
			String base = landingPageUrl.endsWith("/") ? landingPageUrl.substring(0, landingPageUrl.length() - 1)
					: landingPageUrl;
			Map<String, Object> landingPage = fetchJson(base + "?f=json");
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

	/**
	 * Scans {@code /collections?f=json} for the first collection whose spatial extent
	 * bbox has six elements (3D). Returns a {@link ThreeDEntry} with its map URL and
	 * vertical bounds, or {@code null} if no such collection exists.
	 */
	@SuppressWarnings("unchecked")
	private ThreeDEntry discoverThreeDCollection(String landingPageUrl) {
		try {
			String base = landingPageUrl.endsWith("/") ? landingPageUrl.substring(0, landingPageUrl.length() - 1)
					: landingPageUrl;
			Map<String, Object> collectionsResponse = fetchJson(base + "/collections?f=json");
			if (collectionsResponse == null) {
				return null;
			}
			List<Map<String, Object>> collections = (List<Map<String, Object>>) collectionsResponse.get("collections");
			if (collections == null) {
				return null;
			}
			for (Map<String, Object> collection : collections) {
				ThreeDEntry entry = buildThreeDEntry(landingPageUrl, collection);
				if (entry != null) {
					return entry;
				}
			}
		}
		catch (Exception e) {
			// discovery failure — 3D test will be skipped
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private ThreeDEntry buildThreeDEntry(String landingPageUrl, Map<String, Object> collection) {
		try {
			Map<String, Object> extent = (Map<String, Object>) collection.get("extent");
			if (extent == null) {
				return null;
			}
			Map<String, Object> spatial = (Map<String, Object>) extent.get("spatial");
			if (spatial == null) {
				return null;
			}
			List<List<Number>> bboxArray = (List<List<Number>>) spatial.get("bbox");
			if (bboxArray == null || bboxArray.isEmpty()) {
				return null;
			}
			List<Number> bbox = bboxArray.get(0);
			if (bbox == null || bbox.size() != 6) {
				return null;
			}
			double minZ = bbox.get(2).doubleValue();
			double maxZ = bbox.get(5).doubleValue();

			String collectionId = (String) collection.get("id");
			List<Map<String, Object>> links = (List<Map<String, Object>>) collection.get("links");
			String collectionMapUrl = null;
			if (links != null) {
				for (Map<String, Object> link : links) {
					String rel = (String) link.get("rel");
					if (rel != null && matchesRelIgnoringScheme(rel, REL_MAP)) {
						String href = (String) link.get("href");
						if (href != null) {
							collectionMapUrl = resolveUrl(landingPageUrl, href);
							break;
						}
					}
				}
			}
			if (collectionMapUrl == null && collectionId != null) {
				String base = landingPageUrl.endsWith("/") ? landingPageUrl.substring(0, landingPageUrl.length() - 1)
						: landingPageUrl;
				collectionMapUrl = base + "/collections/" + collectionId + "/map";
			}
			if (collectionMapUrl == null) {
				return null;
			}
			return new ThreeDEntry(collectionMapUrl, minZ, maxZ);
		}
		catch (Exception e) {
			return null;
		}
	}

	private Map<String, Object> fetchJson(String urlString) {
		try {
			HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");
			conn.setConnectTimeout(10000);
			conn.setReadTimeout(10000);
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

	private int getStatusRaw(String urlString) {
		try {
			HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(10000);
			conn.setReadTimeout(10000);
			conn.setInstanceFollowRedirects(true);
			return conn.getResponseCode();
		}
		catch (Exception e) {
			System.err.println("[A.21] HTTP request failed for: " + urlString + " => " + e.getMessage());
			return -1;
		}
	}

	private String resolveUrl(String baseUrl, String url) {
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

	private boolean matchesRelIgnoringScheme(String actual, String expected) {
		return normalizeScheme(actual).equals(normalizeScheme(expected));
	}

	private String normalizeScheme(String uri) {
		if (uri != null && uri.startsWith("https://")) {
			return "http://" + uri.substring("https://".length());
		}
		return uri;
	}

	private boolean is4xx(int status) {
		return status >= 400 && status <= 499;
	}

	/**
	 * Formats a vertical coordinate value without scientific notation, preserving
	 * meaningful decimal places.
	 */
	private String formatZ(double value) {
		return new BigDecimal(value).stripTrailingZeros().toPlainString();
	}

}
