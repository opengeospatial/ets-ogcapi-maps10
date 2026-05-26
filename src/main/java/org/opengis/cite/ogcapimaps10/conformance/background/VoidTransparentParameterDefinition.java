package org.opengis.cite.ogcapimaps10.conformance.background;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opengis.cite.ogcapimaps10.conformance.CommonFixture;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class VoidTransparentParameterDefinition extends CommonFixture {

	private static final int DEFAULT_WIDTH = 400;

	private static final int DEFAULT_HEIGHT = 400;

	private static final String DEFAULT_FORMAT = "image/png";

	private static final String MAP_REL_TYPE = "http://www.opengis.net/def/rel/ogc/1.0/map";

	/**
	 * Global extent bbox in CRS84. For interrupted projection CRSs (e.g. Goode
	 * Homolosine), void areas appear naturally within this extent.
	 */
	private static final String GLOBAL_BBOX = "-180,-90,180,90";

	/**
	 * Known interrupted projection CRSs that produce void areas (parts of the map outside
	 * the valid area of the projection). Void areas only exist for interrupted
	 * projections where the map is intentionally "torn"; most standard CRSs (CRS84,
	 * Mercator, UTM, Lambert, etc.) have a fully valid ℝ² and thus no void.
	 */
	private static final List<String> KNOWN_VOID_CRS_IDENTIFIERS = Arrays.asList(
			// Goode Homolosine (OGC)
			"[OGC:1534]", "[OGC:153456]",
			// Goode Homolosine (ESRI)
			"ESRI:54052", "ESRI:54053",
			// Interrupted Mollweide / Sinusoidal (ESRI)
			"ESRI:54011", "ESRI:54048",
			// HTTP URI variants for OGC CRSs
			"http://www.opengis.net/def/crs/OGC/1.3/1534", "http://www.opengis.net/def/crs/OGC/1.3/153456",
			"https://www.opengis.net/def/crs/OGC/1.3/1534", "https://www.opengis.net/def/crs/OGC/1.3/153456");

	private void requireConformance(String... requiredClassSuffixes) throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		String conformanceUrl = rootUri.toString() + "/conformance";
		HttpURLConnection connection = (HttpURLConnection) new URL(conformanceUrl).openConnection();
		connection.setInstanceFollowRedirects(true);
		connection.setRequestMethod("GET");
		connection.setRequestProperty("Accept", "application/json");

		if (connection.getResponseCode() != 200) {
			throw new SkipException("Failed to retrieve conformance: HTTP " + connection.getResponseCode());
		}

		Map<String, Object> data = objectMapper.readValue(connection.getInputStream(),
				new TypeReference<Map<String, Object>>() {
				});

		List<String> conformsTo = objectMapper.convertValue(data.get("conformsTo"), new TypeReference<List<String>>() {
		});
		if (conformsTo == null) {
			throw new SkipException("No 'conformsTo' array found in conformance response.");
		}

		for (String suffix : requiredClassSuffixes) {
			boolean ok = conformsTo.stream().anyMatch(s -> s != null && s.endsWith(suffix));
			if (!ok) {
				throw new SkipException("Test Skipped: SUT does not declare conformance to: " + suffix);
			}
		}
	}

	private Map<String, Object> findLinkByRel(List<Map<String, Object>> links, String rel) {
		if (links == null)
			return null;
		for (Map<String, Object> link : links) {
			Object relVal = link.get("rel");
			if (relVal != null && matchesRelIgnoringScheme(relVal.toString(), rel)) {
				return link;
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
	 * Checks whether the server declares conformance to the CRS requirements class
	 * (/conf/crs).
	 */
	private boolean checkConformanceCrs() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		String conformanceUrl = rootUri.toString() + "/conformance";

		HttpURLConnection connection = (HttpURLConnection) new URL(conformanceUrl).openConnection();
		connection.setInstanceFollowRedirects(true);
		connection.setRequestMethod("GET");
		connection.setRequestProperty("Accept", "application/json");

		if (connection.getResponseCode() != 200)
			return false;

		Map<String, Object> data = objectMapper.readValue(connection.getInputStream(),
				new TypeReference<Map<String, Object>>() {
				});

		List<String> conformsTo = objectMapper.convertValue(data.get("conformsTo"), new TypeReference<List<String>>() {
		});
		if (conformsTo == null)
			return false;

		return conformsTo.stream().anyMatch(s -> s != null && s.endsWith("/conf/crs"));
	}

	/**
	 * Checks if a CRS URI matches any known interrupted projection CRS with void areas.
	 * Returns the original CRS URI if matched, null otherwise.
	 */
	private static String findMatchingVoidCrs(String crsUri) {
		if (crsUri == null)
			return null;
		for (String known : KNOWN_VOID_CRS_IDENTIFIERS) {
			if (crsUri.equalsIgnoreCase(known)) {
				return crsUri;
			}
		}
		return null;
	}

	/**
	 * Returns a base URL template for requesting a map in a CRS that has void areas.
	 * Discovery flow: check storageCrs, then collection "crs" array if /conf/crs is
	 * supported. Skips if no void-capable CRS found.
	 */
	private String getMapBaseUrlTemplate() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		boolean supportsCrsClass = checkConformanceCrs();

		System.out.println("  [CRS Discovery] Server supports /conf/crs: " + supportsCrsClass);

		String apiUrl = rootUri.toString() + "/collections?f=json";

		HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
		connection.setRequestMethod("GET");
		connection.setRequestProperty("Accept", "application/json");

		if (connection.getResponseCode() != 200) {
			throw new SkipException("Failed to retrieve collections: HTTP " + connection.getResponseCode());
		}

		Map<String, Object> data = objectMapper.readValue(connection.getInputStream(),
				new TypeReference<Map<String, Object>>() {
				});

		List<Map<String, Object>> collectionsList = objectMapper.convertValue(data.get("collections"),
				new TypeReference<List<Map<String, Object>>>() {
				});
		if (collectionsList == null || collectionsList.isEmpty()) {
			throw new SkipException("Test Skipped: No collections found.");
		}

		for (Map<String, Object> collection : collectionsList) {
			List<Map<String, Object>> collectionLinks = objectMapper.convertValue(collection.get("links"),
					new TypeReference<List<Map<String, Object>>>() {
					});
			Map<String, Object> relMap = findLinkByRel(collectionLinks, MAP_REL_TYPE);
			if (relMap == null || relMap.get("href") == null)
				continue;

			String voidCrs = null;
			boolean needsCrsParam = false;

			// Check storageCrs first (native CRS — no crs query param needed)
			Object storageCrsObj = collection.get("storageCrs");
			if (storageCrsObj != null) {
				voidCrs = findMatchingVoidCrs(storageCrsObj.toString());
			}

			// If native CRS has no void, check the crs array (requires crs query param)
			if (voidCrs == null && supportsCrsClass) {
				List<String> crsList = objectMapper.convertValue(collection.get("crs"),
						new TypeReference<List<String>>() {
						});
				if (crsList != null) {
					for (String crs : crsList) {
						voidCrs = findMatchingVoidCrs(crs);
						if (voidCrs != null) {
							needsCrsParam = true;
							break;
						}
					}
				}
			}

			if (voidCrs == null)
				continue;

			// Found a collection with a void-capable CRS — build URL template
			String mapUrl = relMap.get("href").toString();
			URI uri = new URI(mapUrl);
			if (!uri.isAbsolute()) {
				uri = rootUri.resolve(uri);
			}

			String baseUrl = uri.toURL().toString() + "?f=" + DEFAULT_FORMAT + "&bbox=" + GLOBAL_BBOX + "&width="
					+ DEFAULT_WIDTH + "&height=" + DEFAULT_HEIGHT;

			if (needsCrsParam) {
				baseUrl += "&crs=" + URLEncoder.encode(voidCrs, "UTF-8");
			}

			System.out.println("  [CRS Discovery] Using collection: " + collection.get("id"));
			System.out.println(
					"  [CRS Discovery] Void-capable CRS: " + voidCrs + " (crs param needed: " + needsCrsParam + ")");
			return baseUrl;
		}

		throw new SkipException("Test Skipped: No collection supports a CRS with void areas "
				+ "(interrupted projections such as Goode Homolosine). "
				+ "Void-transparent tests require a CRS where parts of the map fall outside "
				+ "the valid area of the projection. Cannot proceed with A.9.");
	}

	private HttpURLConnection sendMapRequest(String urlString) throws Exception {
		URL url = new URL(urlString);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setConnectTimeout(30000);
		connection.setReadTimeout(60000);
		connection.setRequestMethod("GET");
		connection.setRequestProperty("Accept", DEFAULT_FORMAT);
		connection.connect();
		return connection;
	}

	/**
	 * Sends a quick pre-check request with shorter timeout to verify parameter support.
	 */
	private HttpURLConnection sendPreCheckRequest(String urlString) throws Exception {
		URL url = new URL(urlString);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setConnectTimeout(5000);
		connection.setReadTimeout(10000);
		connection.setRequestMethod("GET");
		connection.setRequestProperty("Accept", DEFAULT_FORMAT);
		connection.connect();
		return connection;
	}

	// ==========================================================
	// Image helpers (Alpha checks on void areas)
	// ==========================================================
	private static BufferedImage readImage(InputStream is) throws Exception {
		BufferedImage img = ImageIO.read(is);
		if (img == null) {
			throw new IllegalStateException("Response could not be decoded as an image (ImageIO.read returned null).");
		}
		return img;
	}

	private static int getAlphaAt(BufferedImage img, int x, int y) {
		int argb = img.getRGB(x, y);
		return (argb >>> 24) & 0xFF;
	}

	private static void assertCornerAlpha(InputStream imageStream, int expectedAlpha, String assertionMessage)
			throws Exception {
		BufferedImage img = readImage(imageStream);
		int actualAlpha = getAlphaAt(img, 0, 0);

		System.out.println("    [Alpha Check] corner(0,0) alpha=" + actualAlpha + " (expected " + expectedAlpha + ")");
		Assert.assertEquals(actualAlpha, expectedAlpha, assertionMessage);
	}

	/**
	 * <pre>
	 * Abstract test A.3.4
	 *
	 * Identifier: /conf/background/void-transparent-definition
	 * Requirement: Requirement 9: /req/background/void-transparent-definition
	 * Test purpose: Verify that the implementation supports the void-transparent parameter
	 *
	 * Test method
	 * Given: a map resource that conformed successfully to /conf/core
	 * When: retrieving a map for all combinations of (no void-transparent parameter, void-transparent=false, void-transparent=true)
	 *       and with and without void-color parameter
	 * Then:
	 * - assert that the server interprets void-transparent as a Boolean indicating whether the parts of the map outside of the
	 *   valid areas of the projection / CRS should be transparent,
	 * - assert that, if void-transparent is not specified, the server assumes the same value as for transparent (specified or default).
	 * </pre>
	 */
	@Test(description = "Implements A.3.4. Abstract Test for Requirement void-transparent parameter definition")
	public void verifyVoidTransparentDefinition() throws Exception {

		System.out.println("--- Starting A.9 VOID-TRANSPARENT Tests ---");

		requireConformance("/conf/core", "/conf/background");

		String baseUrl = getMapBaseUrlTemplate();

		// ----------------------------------------------------------
		// Pre-check: Verify server supports void-transparent parameter
		// ----------------------------------------------------------
		System.out.println("\n[Pre-check] Testing if server supports void-transparent parameter...");
		try {
			String preCheckUrl = baseUrl + "&void-transparent=true";
			HttpURLConnection preCheckConn = sendPreCheckRequest(preCheckUrl);
			int preCheckCode = preCheckConn.getResponseCode();
			if (preCheckCode == 400 || preCheckCode == 501) {
				throw new SkipException("Test Skipped: Server does not support void-transparent parameter (HTTP "
						+ preCheckCode + ").");
			}
			System.out.println("  [Pre-check] Server accepts void-transparent parameter (HTTP " + preCheckCode + ")");
		}
		catch (SkipException e) {
			throw e;
		}
		catch (Exception e) {
			throw new SkipException("Test Skipped: Server does not support void-transparent parameter or timed out ("
					+ e.getMessage() + ").");
		}

		String voidColorHex = "FF00FF"; // Magenta

		// ==========================================================
		// Group A: void-transparent NOT specified => must assume same value as
		// transparent (specified or default)
		// ==========================================================

		// Case A1: transparent absent (default depends on bgcolor, but here no bgcolor)
		// => transparent default true (A.7)
		System.out.println(
				"\n[Case A1] void-transparent (absent) + transparent (absent) + void-color (absent) => expect alpha=0 (void-transparent defaults to transparent default)");

		HttpURLConnection connA1 = sendMapRequest(baseUrl);
		Assert.assertEquals(connA1.getResponseCode(), 200,
				"Failed: Map request failed with HTTP status. Expected 200.");

		assertCornerAlpha(connA1.getInputStream(), 0,
				"Failed: If void-transparent is not specified, it must assume the same value as transparent (default true when transparent/bgcolor are absent).");

		// Case A1: transparent absent + bgcolor specified => transparent default false
		System.out.println(
				"\n[Case A1] void-transparent (absent) + transparent (absent) + bgcolor=red + void-color (absent) => expect alpha=255 (void-transparent defaults to transparent default=false when bgcolor is specified)");

		String urlA1b = baseUrl + "&bgcolor=red";
		HttpURLConnection connA1b = sendMapRequest(urlA1b);
		Assert.assertEquals(connA1b.getResponseCode(), 200,
				"Failed: Map request failed with HTTP status. Expected 200.");

		assertCornerAlpha(connA1b.getInputStream(), 255,
				"Failed: If void-transparent is not specified, it must assume the same value as transparent (default false when bgcolor is specified and transparent is absent).");

		// Case A2: transparent=false explicitly => void-transparent absent must match =>
		// alpha=255
		System.out.println("\n[Case A2] void-transparent (absent) + transparent=false + void-color=" + voidColorHex
				+ " => expect alpha=255 (match transparent=false)");

		String urlA2 = baseUrl + "&transparent=false" + "&void-color=" + voidColorHex;
		HttpURLConnection connA2 = sendMapRequest(urlA2);
		Assert.assertEquals(connA2.getResponseCode(), 200,
				"Failed: Map request failed with HTTP status. Expected 200.");

		assertCornerAlpha(connA2.getInputStream(), 255,
				"Failed: If void-transparent is not specified, it must assume the same value as transparent (transparent=false => void areas opaque).");

		// Case A3: transparent=true explicitly => void-transparent absent must match =>
		// alpha=0
		System.out.println(
				"\n[Case A3] void-transparent (absent) + transparent=true + void-color (absent) => expect alpha=0 (match transparent=true)");

		String urlA3 = baseUrl + "&transparent=true";
		HttpURLConnection connA3 = sendMapRequest(urlA3);
		Assert.assertEquals(connA3.getResponseCode(), 200,
				"Failed: Map request failed with HTTP status. Expected 200.");

		assertCornerAlpha(connA3.getInputStream(), 0,
				"Failed: If void-transparent is not specified, it must assume the same value as transparent (transparent=true => void areas transparent).");

		// ==========================================================
		// Group B: void-transparent explicitly set => must be interpreted as Boolean for
		// void areas
		// ==========================================================

		// Case B1: transparent=true but void-transparent=false => void areas must be
		// opaque (alpha=255)
		System.out.println(
				"\n[Case B1] transparent=true + void-transparent=false + void-color (absent) => expect alpha=255 (void areas opaque)");

		String urlB1 = baseUrl + "&transparent=true" + "&void-transparent=false";
		HttpURLConnection connB1 = sendMapRequest(urlB1);
		Assert.assertEquals(connB1.getResponseCode(), 200,
				"Failed: Map request failed with HTTP status. Expected 200.");

		assertCornerAlpha(connB1.getInputStream(), 255,
				"Failed: Server must interpret void-transparent=false as Boolean and make void areas opaque.");

		// Case B2: transparent=false but void-transparent=true => void areas must be
		// transparent (alpha=0)
		System.out.println("\n[Case B2] transparent=false + void-transparent=true + void-color=" + voidColorHex
				+ " => expect alpha=0 (void areas transparent)");

		String urlB2 = baseUrl + "&transparent=false" + "&void-transparent=true" + "&void-color=" + voidColorHex;
		HttpURLConnection connB2 = sendMapRequest(urlB2);
		Assert.assertEquals(connB2.getResponseCode(), 200,
				"Failed: Map request failed with HTTP status. Expected 200.");

		assertCornerAlpha(connB2.getInputStream(), 0,
				"Failed: Server must interpret void-transparent=true as Boolean and make void areas transparent (alpha=0) even when transparent=false.");

		// Case B3: void-transparent=false + void-color=present => void areas opaque
		System.out.println("\n[Case B3] void-transparent=false + void-color=" + voidColorHex
				+ " => expect alpha=255 (void areas opaque)");

		String urlB3 = baseUrl + "&void-transparent=false" + "&void-color=" + voidColorHex;
		HttpURLConnection connB3 = sendMapRequest(urlB3);
		Assert.assertEquals(connB3.getResponseCode(), 200,
				"Failed: Map request failed with HTTP status. Expected 200.");

		assertCornerAlpha(connB3.getInputStream(), 255,
				"Failed: Server must interpret void-transparent=false as Boolean and make void areas opaque.");

		// Case B4: void-transparent=true + void-color=absent => void areas transparent
		System.out.println(
				"\n[Case B4] void-transparent=true + void-color (absent) => expect alpha=0 (void areas transparent)");

		String urlB4 = baseUrl + "&void-transparent=true";
		HttpURLConnection connB4 = sendMapRequest(urlB4);
		Assert.assertEquals(connB4.getResponseCode(), 200,
				"Failed: Map request failed with HTTP status. Expected 200.");

		assertCornerAlpha(connB4.getInputStream(), 0,
				"Failed: Server must interpret void-transparent=true as Boolean and make void areas transparent.");
	}

}
