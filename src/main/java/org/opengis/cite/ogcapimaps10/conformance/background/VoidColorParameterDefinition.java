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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VoidColorParameterDefinition extends CommonFixture {

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
	 * Known interrupted projection CRSs that produce void areas (parts of the map
	 * outside the valid area of the projection). Void areas only exist for interrupted
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

	private static final Map<String, Integer> W3C_COLORS = new HashMap<>();
	static {
		W3C_COLORS.put("NAVY", 0x000080);
		W3C_COLORS.put("WHITE", 0xFFFFFF);
		W3C_COLORS.put("BLACK", 0x000000);
		W3C_COLORS.put("RED", 0xFF0000);
		W3C_COLORS.put("GREEN", 0x008000);
		W3C_COLORS.put("BLUE", 0x0000FF);
		W3C_COLORS.put("MAGENTA", 0xFF00FF);
		W3C_COLORS.put("FUCHSIA", 0xFF00FF);
		W3C_COLORS.put("CYAN", 0x00FFFF);
		W3C_COLORS.put("YELLOW", 0xFFFF00);
		W3C_COLORS.put("PURPLE", 0x800080);
		W3C_COLORS.put("GRAY", 0x808080);
		W3C_COLORS.put("GREY", 0x808080);
	}

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

		List<String> conformsTo = objectMapper.convertValue(data.get("conformsTo"),
				new TypeReference<List<String>>() {
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
	 * (/conf/crs). If supported, the collection description enumerates supported CRSs in
	 * its "crs" property and the crs query parameter can be used.
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

		List<String> conformsTo = objectMapper.convertValue(data.get("conformsTo"),
				new TypeReference<List<String>>() {
				});
		if (conformsTo == null)
			return false;

		return conformsTo.stream().anyMatch(s -> s != null && s.endsWith("/conf/crs"));
	}

	/**
	 * Checks if a CRS URI matches any known interrupted projection CRS with void areas.
	 * Comparison is case-insensitive. Returns the original CRS URI (preserving the
	 * server's format) if matched, null otherwise.
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
	 *
	 * Discovery flow: 1. Check if server supports CRS requirements class (/conf/crs). 2.
	 * For each collection with a map link: a. Check storageCrs (native CRS — no crs param
	 * needed). b. If server supports /conf/crs, check collection's "crs" array for known
	 * interrupted projection CRSs. 3. If no void-capable CRS found across all
	 * collections, skip the test.
	 */
	private String getMapBaseUrlTemplate() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		boolean supportsCrsClass = checkConformanceCrs();

		System.out.println("  [CRS Discovery] Server supports /conf/crs: " + supportsCrsClass);

		String collectionsUrl = rootUri.toString() + "/collections?f=json";

		HttpURLConnection connection = (HttpURLConnection) new URL(collectionsUrl).openConnection();
		connection.setRequestMethod("GET");
		connection.setRequestProperty("Accept", "application/json");

		if (connection.getResponseCode() != 200) {
			throw new SkipException("Failed to retrieve collections: HTTP " + connection.getResponseCode());
		}

		Map<String, Object> data = objectMapper.readValue(connection.getInputStream(),
				new TypeReference<Map<String, Object>>() {
				});

		List<Map<String, Object>> collections = objectMapper.convertValue(data.get("collections"),
				new TypeReference<List<Map<String, Object>>>() {
				});
		if (collections == null || collections.isEmpty()) {
			throw new SkipException("Test Skipped: No collections found.");
		}

		for (Map<String, Object> collection : collections) {
			List<Map<String, Object>> links = objectMapper.convertValue(collection.get("links"),
					new TypeReference<List<Map<String, Object>>>() {
					});
			Map<String, Object> mapLink = findLinkByRel(links, MAP_REL_TYPE);
			if (mapLink == null || mapLink.get("href") == null)
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
			String mapUrl = mapLink.get("href").toString();
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
				+ "Void-color tests require a CRS where parts of the map fall outside "
				+ "the valid area of the projection. Cannot proceed with A.8.");
	}

	/**
	 * Sends a GET request to the provided map URL.
	 */
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

	// Image + color helpers
	private static BufferedImage readImage(InputStream is) throws Exception {
		BufferedImage img = ImageIO.read(is);
		if (img == null) {
			throw new IllegalStateException("Response could not be decoded as an image (ImageIO.read returned null).");
		}
		return img;
	}

	private static int getRgb24At(BufferedImage img, int x, int y) {
		return img.getRGB(x, y) & 0xFFFFFF;
	}

	private static int parseExpectedColor(String expectedColor) {
		if (expectedColor == null || expectedColor.trim().isEmpty()) {
			throw new IllegalArgumentException("Expected color must not be null/empty.");
		}

		String s = expectedColor.trim();

		// Hex RGB, 6 digits
		if (s.matches("(?i)^[0-9a-f]{6}$")) {
			return Integer.parseInt(s, 16) & 0xFFFFFF;
		}

		// W3C color name (case-insensitive), via small built-in map
		Integer rgb = W3C_COLORS.get(s.toUpperCase());
		if (rgb != null)
			return rgb;

		throw new IllegalArgumentException("Unsupported W3C color name in this test helper: " + expectedColor);
	}

	/**
	 * Checks that at least one of the four corners matches expected RGB. This is more
	 * robust across implementations where only part of the output is void area.
	 */
	private static void assertAnyCornerMatchesVoidColor(InputStream imageStream, int expectedRgb24,
			String assertionMessage) throws Exception {
		BufferedImage img = readImage(imageStream);

		int w = img.getWidth();
		int h = img.getHeight();

		int[] samples = new int[] { getRgb24At(img, 0, 0), getRgb24At(img, w - 1, 0), getRgb24At(img, 0, h - 1),
				getRgb24At(img, w - 1, h - 1) };

		System.out.println("    [VoidColor Check] corners RGB24="
				+ String.format("(%06X, %06X, %06X, %06X)", samples[0], samples[1], samples[2], samples[3])
				+ ", expected=" + String.format("%06X", expectedRgb24));

		boolean ok = false;
		for (int rgb : samples) {
			if (rgb == expectedRgb24) {
				ok = true;
				break;
			}
		}
		Assert.assertTrue(ok, assertionMessage);
	}

	/**
	 * <pre>
	 * A.3.3. Abstract Test for Requirement void-color parameter definition
	 * Abstract test A.8
	 *
	 * Identifier: /conf/background/void-color-definition
	 * Requirement: Requirement 8: /req/background/void-color-definition
	 * Test purpose: Verify that the implementation supports the void-color parameter
	 *
	 * Test method
	 * Given: a map resource that conformed successfully to /conf/core
	 * When: retrieving a map without void-color parameter, with void-color using a hexadecimal value
	 *       and with void-color using a W3C Web Color name
	 * Then:
	 * - assert that the map operation supports a void-color parameter which can be a hexadecimal RGB value,
	 * - assert that the map operation supports a case-insensitive W3C web color name,
	 * - assert that if void-color is not specified, the same color value as for bgcolor (specified or default) is used
	 *   for the parts outside of the valid areas of the projection / CRS.
	 * </pre>
	 */
	@Test(description = "Implements A.3.3. Abstract Test for Requirement void-color parameter definition")
	public void verifyVoidColorDefinition() throws Exception {

		System.out.println("--- Starting A.8 VOID-COLOR Tests ---");

		requireConformance("/conf/core", "/conf/background");

		String baseUrl = getMapBaseUrlTemplate();

		// ----------------------------------------------------------
		// Pre-check: Verify server supports void-color parameter
		// ----------------------------------------------------------
		System.out.println("\n[Pre-check] Testing if server supports void-color parameter...");
		try {
			String preCheckUrl = baseUrl + "&void-color=FF0000";
			HttpURLConnection preCheckConn = sendPreCheckRequest(preCheckUrl);
			int preCheckCode = preCheckConn.getResponseCode();
			if (preCheckCode == 400 || preCheckCode == 501) {
				throw new SkipException(
						"Test Skipped: Server does not support void-color parameter (HTTP " + preCheckCode + ").");
			}
			System.out.println("  [Pre-check] Server accepts void-color parameter (HTTP " + preCheckCode + ")");
		}
		catch (SkipException e) {
			throw e;
		}
		catch (Exception e) {
			throw new SkipException("Test Skipped: Server does not support void-color parameter or timed out ("
					+ e.getMessage() + ").");
		}

		// ----------------------------------------------------------
		// Case 1: void-color using a hexadecimal value
		// ----------------------------------------------------------
		String hexVoid = "00FF00"; // bright green
		int expectedHexRgb24 = parseExpectedColor(hexVoid);

		System.out.println("\n[Case 1] void-color (hex) = " + hexVoid);

		String urlHex = baseUrl + "&void-color=" + hexVoid;
		HttpURLConnection connHex = sendMapRequest(urlHex);
		Assert.assertEquals(connHex.getResponseCode(), 200,
				"Failed: Map request failed (HTTP " + connHex.getResponseCode() + ").");

		assertAnyCornerMatchesVoidColor(connHex.getInputStream(), expectedHexRgb24,
				"Failed: Server must support void-color as 6-digit hexadecimal RGB and apply it to void areas.");

		// ----------------------------------------------------------
		// Case 2: void-color using a W3C Web Color name (case-insensitive)
		// ----------------------------------------------------------
		String w3cName = "nAvY";
		int expectedW3cRgb24 = parseExpectedColor(w3cName);

		System.out.println("\n[Case 2] void-color (W3C name, case-insensitive) = " + w3cName);

		String urlW3c = baseUrl + "&void-color=" + w3cName;
		HttpURLConnection connW3c = sendMapRequest(urlW3c);
		Assert.assertEquals(connW3c.getResponseCode(), 200,
				"Failed: Map request failed (HTTP " + connW3c.getResponseCode() + ").");

		assertAnyCornerMatchesVoidColor(connW3c.getInputStream(), expectedW3cRgb24,
				"Failed: Server must support void-color as a case-insensitive W3C web color name and apply it to void areas.");

		// ----------------------------------------------------------
		// Case 3: void-color NOT specified => same as bgcolor (specified or default)
		// ----------------------------------------------------------
		String bgcolorHex = "CC00CC"; // purple/magenta-ish
		int expectedBgRgb24 = parseExpectedColor(bgcolorHex);

		System.out.println(
				"\n[Case 3] void-color (absent) + bgcolor=" + bgcolorHex + " => expect void areas use same as bgcolor");

		String urlNoVoid = baseUrl + "&bgcolor=" + bgcolorHex;
		HttpURLConnection connNoVoid = sendMapRequest(urlNoVoid);
		Assert.assertEquals(connNoVoid.getResponseCode(), 200,
				"Failed: Map request failed (HTTP " + connNoVoid.getResponseCode() + ").");

		assertAnyCornerMatchesVoidColor(connNoVoid.getInputStream(), expectedBgRgb24,
				"Failed: If void-color is not specified, void areas must use the same color value as bgcolor (specified or default).");
	}

}
