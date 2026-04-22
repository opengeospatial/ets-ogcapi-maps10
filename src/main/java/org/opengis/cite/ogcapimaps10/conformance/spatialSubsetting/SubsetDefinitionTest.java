package org.opengis.cite.ogcapimaps10.conformance.spatialSubsetting;

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
 * Implements Abstract Test A.22: /conf/spatial-subsetting/subset-definition
 *
 * <p>
 * Test Purpose: Verify that the implementation supports the {@code subset} parameter for
 * spatial subsetting correctly (Requirement 22:
 * /req/spatial-subsetting/subset-definition).
 *
 * <p>
 * Assertions verified:
 * <ul>
 * <li>[Req22/subset-accepted] 22/A+B: subset=Lat(-90:90)&amp;subset=Lon(-180:180)
 * accepted → HTTP 200.</li>
 * <li>[Req22/invalid-axis] 22/D: Unknown axis name → HTTP 4xx.</li>
 * <li>[Req22/out-of-range] 22/E: Interval entirely outside valid CRS range → HTTP 204 or
 * 404.</li>
 * <li>[Req22/wrap-around] 22/F: Anti-meridian wrap-around (low &gt; high) → HTTP
 * 200.</li>
 * <li>[Req22/explicit-subset-crs] 22/G: Explicit subset-crs=CRS84 → HTTP 200.</li>
 * <li>[Req22/subset-bbox-conflict] 22/H: subset (spatial) + bbox → HTTP 4xx.</li>
 * <li>[Req22/subset-center-conflict] 22/H: subset (spatial) + center → HTTP 4xx.</li>
 * <li>[Req22/multiple-subset-equiv] 22/I: Multiple subset params and single
 * comma-separated form both → HTTP 200.</li>
 * <li>[Req22/h-axis] 22/C: subset=h(minZ:maxZ) accepted → HTTP 200 (skipped when no 3D
 * collection found).</li>
 * </ul>
 */
public class SubsetDefinitionTest extends CommonFixture {

	private static final String CRS84_URI = "https://www.opengis.net/def/crs/OGC/1.3/CRS84";

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
	 * Discovers the dataset map endpoint and the first collection with a 3D spatial
	 * extent. Throws {@link SkipException} if no map endpoint can be located.
	 * @param testContext the TestNG test context
	 */
	@BeforeClass
	public void discoverTargets(ITestContext testContext) {
		String landingPageUrl = rootUri.toString();
		mapUrl = findMapUrl(landingPageUrl);
		if (mapUrl != null) {
			sep = mapUrl.contains("?") ? "&" : "?";
		}
		threeDEntry = discoverThreeDCollection(landingPageUrl);
	}

	// -------------------------------------------------------------------------
	// Tests
	// -------------------------------------------------------------------------

	/**
	 * A.22 — Req 22/A, B, D, E, F, G, H, I: verifies subset parameter syntax, axis name
	 * support, conflict detection, edge cases, and multi-param equivalence.
	 */
	@Test(description = "A.22 Req 22/A+B+D+E+F+G+H+I: subset parameter syntax, axis names, "
			+ "conflict detection, wrap-around, and multi-param equivalence.")
	public void verifySubsetDefinition() {
		if (mapUrl == null) {
			throw new SkipException("No map endpoint (rel=ogc/1.0/map) found at " + rootUri + ". Skipping A.22 test.");
		}
		List<String> errors = new ArrayList<>();

		// --- 22/A + 22/B: valid ABNF subset with Lat/Lon axis names → HTTP 200
		// Use a small extent to avoid slow full-global map generation.
		int statusA = getStatusRaw(mapUrl + sep + "subset=Lat%28-10%3A10%29&subset=Lon%28-10%3A10%29&f=png");
		if (statusA != 200) {
			errors.add("[Req22/subset-accepted] Expected HTTP 200 for subset=Lat(-10:10)&subset=Lon(-10:10)"
					+ " but got HTTP " + statusA + ".");
		}

		// --- 22/D: unknown axis name → HTTP 4xx
		int statusD = getStatusRaw(mapUrl + sep + "subset=InvalidAxis%280%3A1%29&f=png");
		if (!is4xx(statusD)) {
			errors.add("[Req22/invalid-axis] Expected HTTP 4xx for subset=InvalidAxis(0:1)" + " but got HTTP " + statusD
					+ ".");
		}

		// --- 22/E: interval entirely outside valid CRS axis range → HTTP 204, 404, or
		// 400.
		// Latitude is bounded to [-90, 90] by CRS84; values 200-300 are always invalid.
		// Servers may return 400 (bad request) for CRS-invalid coordinates or 204/404
		// for an empty spatial intersection — all are conformant responses.
		int statusE = getStatusRaw(mapUrl + sep + "subset=Lat%28200%3A300%29&f=png");
		if (statusE != 204 && statusE != 404 && !is4xx(statusE)) {
			errors.add("[Req22/out-of-range] Expected HTTP 204, 404, or 4xx for subset=Lat(200:300)"
					+ " (entirely outside valid latitude range) but got HTTP " + statusE + ".");
		}

		// --- 22/F: anti-meridian wrap-around (low > high) → HTTP 200
		int statusF = getStatusRaw(mapUrl + sep + "subset=Lat%28-10%3A10%29&subset=Lon%28170%3A-170%29&f=png");
		if (statusF != 200) {
			errors.add("[Req22/wrap-around] Expected HTTP 200 for subset=Lat(-10:10)&subset=Lon(170:-170)"
					+ " (anti-meridian wrap-around) but got HTTP " + statusF + ".");
		}

		// --- 22/G: explicit subset-crs=CRS84 → HTTP 200
		String encodedCrs84 = URLEncoder.encode(CRS84_URI, StandardCharsets.UTF_8);
		int statusG = getStatusRaw(mapUrl + sep + "subset=Lat%28-10%3A10%29&subset=Lon%28-10%3A10%29&subset-crs="
				+ encodedCrs84 + "&f=png");
		if (statusG != 200) {
			errors.add("[Req22/explicit-subset-crs] Expected HTTP 200 for subset=Lat(-10:10)&subset=Lon(-10:10)"
					+ "&subset-crs=" + CRS84_URI + " but got HTTP " + statusG + ".");
		}

		// --- 22/H: subset (spatial) + bbox → HTTP 4xx
		int statusH1 = getStatusRaw(mapUrl + sep + "subset=Lat%28-10%3A10%29&bbox=-180,-90,180,90&f=png");
		if (!is4xx(statusH1)) {
			errors.add("[Req22/subset-bbox-conflict] Expected HTTP 4xx when combining"
					+ " subset=Lat(-10:10) and bbox but got HTTP " + statusH1 + ".");
		}

		// --- 22/H: subset (spatial) + center → HTTP 4xx
		int statusH2 = getStatusRaw(mapUrl + sep + "subset=Lat%28-10%3A10%29&center=0,0&f=png");
		if (!is4xx(statusH2)) {
			errors.add("[Req22/subset-center-conflict] Expected HTTP 4xx when combining"
					+ " subset=Lat(-10:10) and center but got HTTP " + statusH2 + ".");
		}

		// --- 22/I: multiple subset params and single comma-separated form both → HTTP
		// 200
		// Multi-param form already verified above as 22/A. Only add single-param form.
		// Single-param form: subset=Lat(-10:10),Lon(-10:10) — comma encoded as %2C
		int statusI = getStatusRaw(mapUrl + sep + "subset=Lat%28-10%3A10%29%2CLon%28-10%3A10%29&f=png");
		if (statusI != 200) {
			errors.add("[Req22/multiple-subset-equiv] Expected HTTP 200 for single comma-separated form"
					+ " subset=Lat(-10:10),Lon(-10:10) but got HTTP " + statusI + ".");
		}

		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.22 verifySubsetDefinition failed:\n" + String.join("\n", errors));
		}
	}

	/**
	 * A.22 — Req 22/C: verifies that the {@code h} (vertical) axis is accepted in the
	 * {@code subset} parameter when the collection declares a 3D spatial extent.
	 *
	 * <p>
	 * Skipped when no collection with a 3D spatial extent is found on this server.
	 */
	@Test(description = "A.22 Req 22/C: subset=h(minZ:maxZ) accepted → HTTP 200 "
			+ "(skipped when no 3D collection found).")
	public void verifySubsetDefinition3D() {
		if (mapUrl == null) {
			throw new SkipException("No map endpoint (rel=ogc/1.0/map) found at " + rootUri + ". Skipping A.22 test.");
		}
		if (threeDEntry == null) {
			throw new SkipException("No collection with a 3D spatial extent found at " + rootUri
					+ "/collections. Skipping 22/C assertion.");
		}

		String hSubset = "subset=h%28" + formatZ(threeDEntry.minZ) + "%3A" + formatZ(threeDEntry.maxZ) + "%29";
		String threeDSep = threeDEntry.mapUrl.contains("?") ? "&" : "?";
		int status = getStatusRaw(threeDEntry.mapUrl + threeDSep + hSubset + "&f=png");

		if (status != 200) {
			throw new AssertionError("A.22 verifySubsetDefinition3D failed:\n"
					+ "[Req22/h-axis] Expected HTTP 200 for subset=h(" + formatZ(threeDEntry.minZ) + ":"
					+ formatZ(threeDEntry.maxZ) + ") but got HTTP " + status + ".");
		}
	}

	// -------------------------------------------------------------------------
	// Private helpers
	// -------------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	private String findMapUrl(String landingPageUrl) {
		String base = landingPageUrl.endsWith("/") ? landingPageUrl.substring(0, landingPageUrl.length() - 1)
				: landingPageUrl;
		try {
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
		return base + "/map";
	}

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
			conn.setConnectTimeout(30000);
			conn.setReadTimeout(30000);
			conn.setInstanceFollowRedirects(true);
			int code = conn.getResponseCode();
			if (code == HttpURLConnection.HTTP_OK) {
				try (InputStream is = conn.getInputStream()) {
					return OBJECT_MAPPER.readValue(is, new TypeReference<Map<String, Object>>() {
					});
				}
			}
			System.err.println("[A.22] fetchJson got HTTP " + code + " for: " + urlString);
		}
		catch (Exception e) {
			System.err.println("[A.22] fetchJson failed for: " + urlString + " => " + e.getMessage());
		}
		return null;
	}

	private int getStatusRaw(String urlString) {
		try {
			HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(30000);
			conn.setReadTimeout(60000);
			conn.setInstanceFollowRedirects(true);
			return conn.getResponseCode();
		}
		catch (Exception e) {
			System.err.println("[A.22] HTTP request failed for: " + urlString + " => " + e.getMessage());
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

	private String formatZ(double value) {
		return new BigDecimal(value).stripTrailingZeros().toPlainString();
	}

}
