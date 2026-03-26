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
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * Implements Abstract Test A.18: /conf/spatial-subsetting/bbox-crs
 *
 * <p>
 * Test Purpose: Verify that the implementation supports the bbox-crs parameter for
 * specifying the CRS of the bbox parameter correctly (Requirement 18:
 * /req/spatial-subsetting/bbox-crs).
 *
 * <p>
 * Assertions verified:
 * <ul>
 * <li>[Req18/crs84-accepted] The server returns HTTP 200 when bbox-crs is set to CRS84
 * together with a valid bbox.</li>
 * <li>[Req18/default-crs84] The server returns HTTP 200 when bbox-crs is omitted,
 * defaulting to CRS84.</li>
 * <li>[Req18/storage-crs-accepted] If the storage CRS is known, the server returns HTTP
 * 200 when bbox-crs is set to the storage CRS.</li>
 * <li>[Req18/safe-curie] The server returns HTTP 200 when bbox-crs is expressed as the
 * safe CURIE [OGC:CRS84].</li>
 * <li>[Req18/bbox-crs-ignored] The server returns HTTP 200 (does not reject) when
 * bbox-crs is supplied without a bbox parameter.</li>
 * </ul>
 */
public class BBoxCrsDefinitionTest extends CommonFixture {

	private static final String CRS84_URI = "https://www.opengis.net/def/crs/OGC/1.3/CRS84";

	private static final String CRS84_SAFE_CURIE = "%5BOGC%3ACRS84%5D";

	private static final String WHOLE_WORLD_BBOX = "bbox=-180,-90,180,90";

	private static final String REL_MAP = "http://www.opengis.net/def/rel/ogc/1.0/map";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	/**
	 * A.18 Abstract Test for Requirement /req/spatial-subsetting/bbox-crs.
	 *
	 * <p>
	 * Verifies that the server accepts bbox-crs=CRS84, defaults to CRS84 when bbox-crs is
	 * omitted, accepts the storage CRS (if known), accepts safe CURIE notation, and does
	 * not reject bbox-crs when bbox is absent.
	 * @param context the TestNG test context, used to access suite attributes
	 */
	@Test(description = "A.18 Abstract Test for Requirement /req/spatial-subsetting/bbox-crs: "
			+ "Verify that the implementation supports the bbox-crs parameter correctly for map requests.")
	public void verifyBBoxCrsDefinition(ITestContext context) {
		List<String> errors = new ArrayList<>();

		String landingPageUrl = rootUri.toString();

		// Locate the dataset map endpoint
		String mapUrl = findMapUrl(landingPageUrl);
		if (mapUrl == null) {
			throw new SkipException("Could not find a dataset map endpoint (rel=ogc/1.0/map) from the landing page at "
					+ landingPageUrl + ". Skipping A.18 test.");
		}
		String sep = mapUrl.contains("?") ? "&" : "?";

		// --- Assertion B (Req 18/B): CRS84 accepted as bbox-crs value ---
		int statusB = getStatusRaw(mapUrl + sep + WHOLE_WORLD_BBOX + "&bbox-crs=" + encodeCrsUri(CRS84_URI) + "&f=png");
		if (statusB != 200) {
			errors.add("[Req18/crs84-accepted] Expected HTTP 200 when bbox-crs=" + CRS84_URI
					+ " with a valid bbox, but got HTTP " + statusB + ".");
		}

		// --- Assertion C (Req 18/C): omitting bbox-crs defaults to CRS84 ---
		int statusC = getStatusRaw(mapUrl + sep + WHOLE_WORLD_BBOX + "&f=png");
		if (statusC != 200) {
			errors.add("[Req18/default-crs84] Expected HTTP 200 when bbox-crs is omitted (defaults to CRS84)"
					+ " but got HTTP " + statusC + ".");
		}

		// --- Assertion D (Req 18/D): storage/native CRS accepted ---
		String storageCrs = discoverStorageCrs(landingPageUrl);
		if (storageCrs != null) {
			int statusD = getStatusRaw(
					mapUrl + sep + WHOLE_WORLD_BBOX + "&bbox-crs=" + encodeCrsUri(storageCrs) + "&f=png");
			if (statusD != 200) {
				errors.add("[Req18/storage-crs-accepted] Expected HTTP 200 when bbox-crs=" + storageCrs
						+ " (storage CRS) but got HTTP " + statusD + ".");
			}
		}

		// --- Assertion E (Req 18/E): safe CURIE notation accepted ---
		int statusE = getStatusRaw(mapUrl + sep + WHOLE_WORLD_BBOX + "&bbox-crs=" + CRS84_SAFE_CURIE + "&f=png");
		if (statusE != 200) {
			errors.add("[Req18/safe-curie] Expected HTTP 200 when bbox-crs=[OGC:CRS84] (safe CURIE, URL-encoded)"
					+ " but got HTTP " + statusE + ".");
		}

		// --- Assertion F (Req 18/F): bbox-crs ignored when bbox is absent ---
		int statusF = getStatusRaw(mapUrl + sep + "bbox-crs=" + encodeCrsUri(CRS84_URI) + "&f=png");
		if (statusF != 200) {
			errors.add("[Req18/bbox-crs-ignored] Expected HTTP 200 when bbox-crs is supplied without bbox"
					+ " (bbox-crs should be ignored), but got HTTP " + statusF + ".");
		}

		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.18 verifyBBoxCrsDefinition failed:\n" + String.join("\n", errors));
		}
	}

	// -------------------------------------------------------------------------
	// Private helpers
	// -------------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	private String findMapUrl(String landingPageUrl) {
		try {
			Map<String, Object> landingPage = fetchJson(landingPageUrl + "?f=json");
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

	private int getStatusRaw(String urlString) {
		try {
			URL url = new URL(urlString);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(10000);
			conn.setReadTimeout(10000);
			conn.setInstanceFollowRedirects(true);
			return conn.getResponseCode();
		}
		catch (Exception e) {
			System.err.println("[A.18] HTTP request failed for: " + urlString + " => " + e.getMessage());
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
