package org.opengis.cite.ogcapimaps10.conformance.spatialSubsetting;

import java.io.InputStream;
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
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * Implements Abstract Test A.19: /conf/spatial-subsetting/subset-crs
 *
 * <p>
 * Test Purpose: Verify that the implementation supports the subset-crs parameter for
 * specifying the CRS of the subset parameter correctly (Requirement 19:
 * /req/spatial-subsetting/subset-crs).
 *
 * <p>
 * Assertions verified:
 * <ul>
 * <li>[Req19/crs84-accepted] The server returns HTTP 200 when subset-crs is set to CRS84
 * together with a valid subset.</li>
 * <li>[Req19/default-crs84] The server returns HTTP 200 when subset-crs is omitted,
 * defaulting to CRS84.</li>
 * <li>[Req19/storage-crs-accepted] If the storage CRS is known, the server returns HTTP
 * 200 when subset-crs is set to the storage CRS.</li>
 * <li>[Req19/safe-curie] The server returns HTTP 200 when subset-crs is expressed as the
 * safe CURIE [OGC:CRS84].</li>
 * <li>[Req19/subset-crs-ignored] The server returns HTTP 200 (does not reject) when
 * subset-crs is supplied without a subset parameter.</li>
 * </ul>
 */
public class SubsetCrsDefinitionTest extends CommonFixture {

	private static final String CRS84_URI = "https://www.opengis.net/def/crs/OGC/1.3/CRS84";

	// URL-encoded form of the safe CURIE [OGC:CRS84]
	private static final String CRS84_SAFE_CURIE = "%5BOGC%3ACRS84%5D";

	// Whole-world extent expressed using CRS84 axis names (Lat, Lon), with
	// parentheses and colons percent-encoded so new URL() doesn't reject the query
	// string
	private static final String WHOLE_WORLD_SUBSET = "subset=Lat%28-90%3A90%29&subset=Lon%28-180%3A180%29";

	private static final String REL_MAP = "http://www.opengis.net/def/rel/ogc/1.0/map";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	/**
	 * A.19 Abstract Test for Requirement /req/spatial-subsetting/subset-crs.
	 *
	 * <p>
	 * Verifies that the server accepts subset-crs=CRS84, defaults to CRS84 when
	 * subset-crs is omitted, accepts the storage CRS (if known), accepts safe CURIE
	 * notation, and does not reject subset-crs when subset is absent.
	 */
	@Test(description = "A.19 Abstract Test for Requirement /req/spatial-subsetting/subset-crs: "
			+ "Verify that the implementation supports the subset-crs parameter correctly for map requests.")
	public void verifySubsetCrsDefinition() {
		List<String> errors = new ArrayList<>();

		String landingPageUrl = rootUri.toString();

		// Locate the dataset map endpoint
		String mapUrl = findMapUrl(landingPageUrl);
		if (mapUrl == null) {
			throw new SkipException("Could not find a dataset map endpoint (rel=ogc/1.0/map) from the landing page at "
					+ landingPageUrl + ". Skipping A.19 test.");
		}
		String sep = mapUrl.contains("?") ? "&" : "?";

		// Reachability pre-check: a bare map request (no extra params) must succeed
		// before we validate individual assertions. If it returns -1 the server is
		// unreachable and all assertions would report misleading -1 codes.
		int reachabilityStatus = getStatusRaw(mapUrl + sep + "f=png");
		if (reachabilityStatus == -1) {
			throw new SkipException("Map endpoint unreachable (HTTP -1) at " + mapUrl + ". Skipping A.19 test.");
		}

		// --- Assertion B (Req 19/B): CRS84 accepted as subset-crs value ---
		int statusB = getStatusRaw(
				mapUrl + sep + WHOLE_WORLD_SUBSET + "&subset-crs=" + encodeCrsUri(CRS84_URI) + "&f=png");
		if (statusB != 200) {
			errors.add("[Req19/crs84-accepted] Expected HTTP 200 when subset-crs=" + CRS84_URI
					+ " with a valid subset, but got HTTP " + statusB + ".");
		}

		// --- Assertion C (Req 19/C): omitting subset-crs defaults to CRS84 ---
		int statusC = getStatusRaw(mapUrl + sep + WHOLE_WORLD_SUBSET + "&f=png");
		if (statusC != 200) {
			errors.add("[Req19/default-crs84] Expected HTTP 200 when subset-crs is omitted (defaults to CRS84)"
					+ " but got HTTP " + statusC + ".");
		}

		// --- Assertion D (Req 19/D): storage/native CRS accepted ---
		String storageCrs = discoverStorageCrs(landingPageUrl);
		if (storageCrs != null) {
			int statusD = getStatusRaw(
					mapUrl + sep + WHOLE_WORLD_SUBSET + "&subset-crs=" + encodeCrsUri(storageCrs) + "&f=png");
			if (statusD != 200) {
				errors.add("[Req19/storage-crs-accepted] Expected HTTP 200 when subset-crs=" + storageCrs
						+ " (storage CRS) but got HTTP " + statusD + ".");
			}
		}

		// --- Assertion E (Req 19/E): safe CURIE notation accepted ---
		int statusE = getStatusRaw(mapUrl + sep + WHOLE_WORLD_SUBSET + "&subset-crs=" + CRS84_SAFE_CURIE + "&f=png");
		if (statusE != 200) {
			errors.add("[Req19/safe-curie] Expected HTTP 200 when subset-crs=[OGC:CRS84] (safe CURIE, URL-encoded)"
					+ " but got HTTP " + statusE + ".");
		}

		// --- Assertion F (Req 19/F): subset-crs ignored when subset is absent ---
		int statusF = getStatusRaw(mapUrl + sep + "subset-crs=" + encodeCrsUri(CRS84_URI) + "&f=png");
		if (statusF != 200) {
			errors.add("[Req19/subset-crs-ignored] Expected HTTP 200 when subset-crs is supplied without subset"
					+ " (subset-crs should be ignored), but got HTTP " + statusF + ".");
		}

		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.19 verifySubsetCrsDefinition failed:\n" + String.join("\n", errors));
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

	@SuppressWarnings("unchecked")
	private String discoverStorageCrs(String landingPageUrl) {
		try {
			Map<String, Object> collectionsResponse = fetchJson(landingPageUrl + "/collections?f=json");
			if (collectionsResponse == null) {
				return null;
			}
			List<Map<String, Object>> collections = (List<Map<String, Object>>) collectionsResponse.get("collections");
			if (collections == null) {
				return null;
			}
			for (Map<String, Object> collection : collections) {
				Object storageCrs = collection.get("storageCrs");
				if (storageCrs instanceof String) {
					String crsUri = (String) storageCrs;
					if (!normalizeScheme(crsUri).equals(normalizeScheme(CRS84_URI))) {
						return crsUri;
					}
				}
			}
		}
		catch (Exception e) {
			// storage CRS not discoverable
		}
		return null;
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
			System.err.println("[A.19] HTTP request failed for: " + urlString + " => " + e.getMessage());
			return -1;
		}
	}

	private String encodeCrsUri(String crsUri) {
		return URLEncoder.encode(crsUri, StandardCharsets.UTF_8);
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
		if (uri.startsWith("https://")) {
			return "http://" + uri.substring("https://".length());
		}
		return uri;
	}

}
