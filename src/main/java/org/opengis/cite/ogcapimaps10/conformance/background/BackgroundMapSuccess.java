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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BackgroundMapSuccess extends CommonFixture {

	private static final String VALID_BBOX = "-180,-90,180,90";

	private static final String VOID_BBOX = "-180,-120,180,120";

	private static final int DEFAULT_WIDTH = 400;

	private static final int DEFAULT_HEIGHT = 400;

	private static final String DEFAULT_FORMAT = "image/png";

	private static final String MAP_REL_TYPE = "http://www.opengis.net/def/rel/ogc/1.0/map";

	private static final String TEST_BG_HEX = "CC00CC";

	private static final String TEST_BG_W3C = "NaVy"; // case-insensitive

	private static final String TEST_VOID_HEX = "00FF00"; // green-ish

	private static final String TEST_VOID_W3C = "ReD"; // case-insensitive

	private static final Map<String, Integer> W3C_COLORS = new HashMap<>();
	static {
		W3C_COLORS.put("NAVY", 0x000080);
		W3C_COLORS.put("RED", 0xFF0000);
		W3C_COLORS.put("GREEN", 0x008000);
		W3C_COLORS.put("LIME", 0x00FF00);
		W3C_COLORS.put("BLUE", 0x0000FF);
		W3C_COLORS.put("BLACK", 0x000000);
		W3C_COLORS.put("WHITE", 0xFFFFFF);
		W3C_COLORS.put("YELLOW", 0xFFFF00);
		W3C_COLORS.put("CYAN", 0x00FFFF);
		W3C_COLORS.put("AQUA", 0x00FFFF);
		W3C_COLORS.put("MAGENTA", 0xFF00FF);
		W3C_COLORS.put("FUCHSIA", 0xFF00FF);
		W3C_COLORS.put("GRAY", 0x808080);
		W3C_COLORS.put("SILVER", 0xC0C0C0);
		W3C_COLORS.put("MAROON", 0x800000);
		W3C_COLORS.put("OLIVE", 0x808000);
		W3C_COLORS.put("PURPLE", 0x800080);
		W3C_COLORS.put("TEAL", 0x008080);
	}

	private void requireConformance(String... requiredClassSuffixes) throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		String conformanceUrl = rootUri.toString() + "/conformance";
		HttpURLConnection connection = (HttpURLConnection) new URL(conformanceUrl).openConnection();
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
			if (relVal != null && rel.equals(relVal.toString())) {
				return link;
			}
		}
		return null;
	}

	/**
	 * Gets base URL template for /map of the first available collection using
	 * bbox/width/height/f=image/png. Uses convertValue to avoid unchecked-cast warnings
	 * (no @SuppressWarnings needed).
	 */
	private String getMapBaseUrlTemplateWithBbox(String bbox) throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
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
			if (relMap != null && relMap.get("href") != null) {
				String mapUrl = relMap.get("href").toString();
				URI uri = new URI(mapUrl);
				if (!uri.isAbsolute()) {
					uri = rootUri.resolve(uri);
				}
				return uri.toURL().toString() + "?f=" + DEFAULT_FORMAT + "&bbox=" + bbox + "&width=" + DEFAULT_WIDTH
						+ "&height=" + DEFAULT_HEIGHT;
			}
		}

		throw new SkipException(
				"Test Skipped: No map resources found in the collections to verify. Cannot proceed with A.10.");
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

	/**
	 * Pre-check if the server supports a specific parameter. Returns true if supported.
	 */
	private boolean isParameterSupported(String baseUrl, String paramName, String paramValue) {
		try {
			String testUrl = baseUrl + "&" + paramName + "=" + paramValue;
			HttpURLConnection conn = sendPreCheckRequest(testUrl);
			int code = conn.getResponseCode();
			return code != 400 && code != 501;
		}
		catch (Exception e) {
			return false;
		}
	}

	// ==========================================================
	// Color / image helpers (RGB + alpha checks)
	// ==========================================================

	private static int parseExpectedColor(String color) {
		if (color == null)
			throw new IllegalArgumentException("Color must not be null.");
		String c = color.trim();
		if (c.matches("(?i)^[0-9a-f]{6}$")) {
			return Integer.parseInt(c, 16) & 0xFFFFFF;
		}
		Integer rgb = W3C_COLORS.get(c.toUpperCase());
		if (rgb == null) {
			throw new IllegalArgumentException("Unsupported W3C color name in test: " + color);
		}
		return rgb & 0xFFFFFF;
	}

	private static BufferedImage readImage(InputStream is) throws Exception {
		BufferedImage img = ImageIO.read(is);
		if (img == null) {
			throw new IllegalStateException("Response could not be decoded as an image (ImageIO.read returned null).");
		}
		return img;
	}

	private static int rgb24At(BufferedImage img, int x, int y) {
		return img.getRGB(x, y) & 0xFFFFFF;
	}

	private static int alphaAt(BufferedImage img, int x, int y) {
		int argb = img.getRGB(x, y);
		return (argb >>> 24) & 0xFF;
	}

	/**
	 * Samples corners and returns the first match. This reduces fragility when some
	 * corners contain data due to styling or rendering.
	 */
	private static boolean anyCornerMatches(BufferedImage img, int expectedRgb24, Integer expectedAlphaOrNull) {
		int w = img.getWidth();
		int h = img.getHeight();

		int[][] pts = new int[][] { { 0, 0 }, { w - 1, 0 }, { 0, h - 1 }, { w - 1, h - 1 } };

		for (int[] p : pts) {
			int x = p[0], y = p[1];
			int rgb = rgb24At(img, x, y);
			int a = alphaAt(img, x, y);

			boolean rgbOk = (rgb == (expectedRgb24 & 0xFFFFFF));
			boolean aOk = (expectedAlphaOrNull == null) || (a == expectedAlphaOrNull.intValue());

			if (rgbOk && aOk)
				return true;
		}
		return false;
	}

	/**
	 * Decide default transparent value (A.7 rules): - if transparent absent and bgcolor
	 * absent => default true - if transparent absent and bgcolor present => default false
	 */
	private static boolean resolveTransparent(Boolean transparentParam, boolean hasBgcolorParam) {
		if (transparentParam != null)
			return transparentParam.booleanValue();
		return !hasBgcolorParam; // absent bgcolor => true, present bgcolor => false
	}

	/**
	 * Decide default void-transparent value (A.9 rules): - if void-transparent absent =>
	 * same as resolved transparent
	 */
	private static boolean resolveVoidTransparent(Boolean voidTransparentParam, boolean resolvedTransparent) {
		if (voidTransparentParam != null)
			return voidTransparentParam.booleanValue();
		return resolvedTransparent;
	}

	/**
	 * <pre>
	 * Abstract test A.3.5
	 *
	 * Identifier: /conf/background/map-success
	 * Requirement: Requirement 10: /req/background/map-success
	 * Test purpose:
	 * Verify that the implementation's response for the map retrieval operation with a background color
	 * and/or transparent parameter is correct.
	 *
	 * Test method
	 * Given: a map resource that conformed successfully to /conf/core
	 * When: for all combinations of (no transparent parameter, transparent=false, transparent=true) and
	 *       (without bgcolor parameter, with bgcolor using a hexadecimal value and with bgcolor using a W3C Web Color name)
	 * Then:
	 * - assert that the color of the map in the areas with no data is exactly the one specified in the bgcolor,
	 * - assert that the color in parts of the map outside of the valid areas of the projection / CRS is the one specified by
	 *   void-color, or otherwise default to the same as the background color (whether specified by bgcolor or default),
	 * - assert that the transparency setting in parts of the map outside of the valid areas of the projection / CRS is the one
	 *   specified by void-transparent, or otherwise default to the same as the background transparency setting (whether specified
	 *   by transparent or default),
	 * - assert that, in case the output format allows it and in the absence of the transparent parameter (or if it is false),
	 *   the opacity (alpha value) of the map in the areas with no data is exactly 100%, if transparent is false or 0% if transparent
	 *   is true (if the renderer supports anti-aliasing, at the edges between data and no-data areas, the opacity is allowed to have
	 *   a value between 0% and 100%).
	 *   </pre>
	 */
	@Test(description = "Implements A.3.5. Abstract Test for Requirement Background Map Success")
	public void verifyBackgroundMapSuccess() throws Exception {

		System.out.println("--- Starting A.10 BACKGROUND MAP SUCCESS Tests ---");

		requireConformance("/conf/core", "/conf/background");

		String baseValid = getMapBaseUrlTemplateWithBbox(VALID_BBOX);
		String baseVoid = getMapBaseUrlTemplateWithBbox(VOID_BBOX);

		// Pre-check void parameter support
		System.out.println("\n[Pre-check] Testing if server supports void parameters...");
		boolean supportsVoidColor = isParameterSupported(baseVoid, "void-color", TEST_VOID_HEX);
		boolean supportsVoidTransparent = isParameterSupported(baseVoid, "void-transparent", "true");

		System.out.println("  [Pre-check] void-color supported: " + supportsVoidColor);
		System.out.println("  [Pre-check] void-transparent supported: " + supportsVoidTransparent);

		// Test 9 combinations: transparent (absent/false/true) × bgcolor (absent/hex/W3C)
		Boolean[] transparentVariants = new Boolean[] { null, Boolean.FALSE, Boolean.TRUE };
		String[] bgcolorVariants = new String[] { null, TEST_BG_HEX, TEST_BG_W3C };

		int caseNum = 0;

		for (Boolean transparentParam : transparentVariants) {
			for (String bgcolorParam : bgcolorVariants) {

				boolean hasBg = (bgcolorParam != null);
				boolean resolvedTransparent = resolveTransparent(transparentParam, hasBg);
				int expectedBgRgb = hasBg ? parseExpectedColor(bgcolorParam) : 0xFFFFFF;
				int expectedBgAlpha = resolvedTransparent ? 0 : 255;

				caseNum++;
				System.out.println("\n[Case " + caseNum + "] transparent="
						+ (transparentParam == null ? "(absent)" : transparentParam.toString()) + ", bgcolor="
						+ (bgcolorParam == null ? "(absent)" : bgcolorParam));

				// Build request URL with current parameter combination
				String urlValid = baseValid;
				if (transparentParam != null) {
					urlValid += "&transparent=" + transparentParam.toString().toLowerCase();
				}
				if (bgcolorParam != null) {
					urlValid += "&bgcolor=" + bgcolorParam;
				}

				HttpURLConnection connValid = sendMapRequest(urlValid);
				Assert.assertEquals(connValid.getResponseCode(), 200,
						"Failed: Valid bbox map request failed. Expected HTTP 200.");

				BufferedImage imgValid = readImage(connValid.getInputStream());

				// Verify background alpha matches transparent setting
				boolean alphaOkValid = anyCornerMatches(imgValid, rgb24At(imgValid, 0, 0), expectedBgAlpha)
						|| anyCornerMatches(imgValid, rgb24At(imgValid, imgValid.getWidth() - 1, 0), expectedBgAlpha)
						|| anyCornerMatches(imgValid, rgb24At(imgValid, 0, imgValid.getHeight() - 1), expectedBgAlpha)
						|| anyCornerMatches(imgValid,
								rgb24At(imgValid, imgValid.getWidth() - 1, imgValid.getHeight() - 1), expectedBgAlpha);

				System.out.println("    [Valid bbox] expected background alpha=" + expectedBgAlpha
						+ " (resolved transparent=" + resolvedTransparent + ")");
				Assert.assertTrue(alphaOkValid,
						"Failed: Expected background/no-data alpha to match resolved transparent setting.");

				// Verify background color matches bgcolor (skip if fully transparent)
				if (bgcolorParam != null || !resolvedTransparent) {
					boolean rgbOkValid = anyCornerMatches(imgValid, expectedBgRgb, null);
					System.out
						.println("    [Valid bbox] expected background rgb=#" + String.format("%06X", expectedBgRgb));
					Assert.assertTrue(rgbOkValid,
							"Failed: Expected background/no-data RGB to match bgcolor (or default when bgcolor absent & background opaque).");
				}
				else {
					System.out.println(
							"    [Valid bbox] transparent background expected; skipping strict RGB assert when bgcolor is absent.");
				}
			}
		}

		// Void parameter validation - only run if server supports void parameters
		if (supportsVoidColor || supportsVoidTransparent) {
			System.out.println("\n--- Starting Void Parameter Validation ---");

			// Verify void-color defaults to bgcolor when not specified
			if (supportsVoidColor) {
				System.out.println("\n[Void Color Check] Testing void-color defaults to bgcolor...");

				String urlVoidNoBgcolor = baseVoid + "&bgcolor=" + TEST_BG_HEX + "&transparent=false";
				HttpURLConnection connVoidNoBg = sendMapRequest(urlVoidNoBgcolor);
				Assert.assertEquals(connVoidNoBg.getResponseCode(), 200,
						"Failed: Void bbox map request failed. Expected HTTP 200.");

				BufferedImage imgVoidNoBg = readImage(connVoidNoBg.getInputStream());
				int expectedBgRgbForVoid = parseExpectedColor(TEST_BG_HEX);

				boolean voidDefaultOk = anyCornerMatches(imgVoidNoBg, expectedBgRgbForVoid, null);
				System.out.println("    [Void Color Check] expected void rgb (default to bgcolor)=#"
						+ String.format("%06X", expectedBgRgbForVoid));
				Assert.assertTrue(voidDefaultOk,
						"Failed: When void-color is not specified, void areas must use the same color as bgcolor.");

				// Verify explicit void-color is applied
				System.out.println("\n[Void Color Check] Testing explicit void-color parameter...");
				String urlVoidWithColor = baseVoid + "&bgcolor=" + TEST_BG_HEX + "&void-color=" + TEST_VOID_HEX
						+ "&transparent=false";
				HttpURLConnection connVoidWithColor = sendMapRequest(urlVoidWithColor);
				Assert.assertEquals(connVoidWithColor.getResponseCode(), 200,
						"Failed: Void bbox map request failed. Expected HTTP 200.");

				BufferedImage imgVoidWithColor = readImage(connVoidWithColor.getInputStream());
				int expectedVoidRgb = parseExpectedColor(TEST_VOID_HEX);

				boolean voidColorOk = anyCornerMatches(imgVoidWithColor, expectedVoidRgb, null);
				System.out
					.println("    [Void Color Check] expected void rgb=#" + String.format("%06X", expectedVoidRgb));
				Assert.assertTrue(voidColorOk,
						"Failed: When void-color is specified, void areas must use the specified void-color.");
			}

			// Verify void-transparent defaults to transparent when not specified
			if (supportsVoidTransparent) {
				System.out.println("\n[Void Transparent Check] Testing void-transparent defaults to transparent...");

				String urlVoidNoTransp = baseVoid + "&transparent=true";
				HttpURLConnection connVoidNoTransp = sendMapRequest(urlVoidNoTransp);
				Assert.assertEquals(connVoidNoTransp.getResponseCode(), 200,
						"Failed: Void bbox map request failed. Expected HTTP 200.");

				BufferedImage imgVoidNoTransp = readImage(connVoidNoTransp.getInputStream());
				boolean voidTranspDefaultOk = anyCornerMatches(imgVoidNoTransp, rgb24At(imgVoidNoTransp, 0, 0), 0);
				System.out.println("    [Void Transparent Check] expected void alpha (default to transparent)=0");
				Assert.assertTrue(voidTranspDefaultOk,
						"Failed: When void-transparent is not specified, void areas must use the same transparency as transparent.");

				// Verify explicit void-transparent=false makes void areas opaque
				System.out.println("\n[Void Transparent Check] Testing explicit void-transparent=false...");
				String urlVoidWithTransp = baseVoid + "&transparent=true&void-transparent=false";
				HttpURLConnection connVoidWithTransp = sendMapRequest(urlVoidWithTransp);
				Assert.assertEquals(connVoidWithTransp.getResponseCode(), 200,
						"Failed: Void bbox map request failed. Expected HTTP 200.");

				BufferedImage imgVoidWithTransp = readImage(connVoidWithTransp.getInputStream());
				boolean voidTranspOk = anyCornerMatches(imgVoidWithTransp, rgb24At(imgVoidWithTransp, 0, 0), 255);
				System.out.println("    [Void Transparent Check] expected void alpha (explicit false)=255");
				Assert.assertTrue(voidTranspOk,
						"Failed: When void-transparent=false is specified, void areas must be opaque.");
			}
		}
		else {
			System.out.println(
					"\n[Void Parameter Validation] Skipped: Server does not support void-color or void-transparent parameters.");
		}

		System.out.println("\n--- A.10 BACKGROUND MAP SUCCESS Tests Completed ---");
	}

}
