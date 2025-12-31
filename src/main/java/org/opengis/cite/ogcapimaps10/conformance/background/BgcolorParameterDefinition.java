package org.opengis.cite.ogcapimaps10.conformance.background;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opengis.cite.ogcapimaps10.conformance.CommonFixture;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.SkipException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

interface TestImageValidator {

	static final Map<String, Integer> W3C_COLORS = Collections.unmodifiableMap(new HashMap<String, Integer>() {
		{
			put("WHITE", 0xFFFFFF); // Default white
			put("NAVY", 0x000080); // Case 2 example
			put("BLUE", 0x0000FF);
			put("RED", 0xFF0000);
			put("LIME", 0x00FF00);
		}
	});

	/**
	 * Converts a Hex string (e.g., "0xFF00FF", "FF00FF", "#FF00FF") or a W3C Web Color
	 * name (e.g., "NAVY") to an integer RGB value.
	 */
	static int parseExpectedColor(String expectedColor) {
		if (expectedColor == null) {
			throw new IllegalArgumentException("expectedColor is null");
		}

		String raw = expectedColor.trim();
		if (raw.isEmpty()) {
			throw new IllegalArgumentException("expectedColor is empty");
		}

		String colorUpper = raw.toUpperCase();

		// 1) Handle W3C Color Names (case-insensitive)
		Integer named = W3C_COLORS.get(colorUpper);
		if (named != null) {
			return named;
		}

		// 2) Normalize Hex prefixes: "#RRGGBB" / "0xRRGGBB" / "RRGGBB"
		if (colorUpper.startsWith("#")) {
			colorUpper = colorUpper.substring(1);
		}
		if (colorUpper.startsWith("0X")) {
			colorUpper = colorUpper.substring(2);
		}

		// 3) Accept plain 6-digit hex: RRGGBB
		if (!colorUpper.matches("^[0-9A-F]{6}$")) {
			throw new IllegalArgumentException("Unsupported color format or W3C color name: " + expectedColor);
		}

		try {
			return (int) Long.parseLong(colorUpper, 16);
		}
		catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid hexadecimal color format: " + expectedColor);
		}
	}

	/**
	 * Checks if the pixel color in the non-data area of the map image matches the
	 * expected background color.
	 * @param imageStream The InputStream of the image (from the HTTP response body).
	 * @param expectedColor The expected color value (Hex e.g., "0xFF0000" or W3C name
	 * e.g., "BLUE").
	 * @return True if the background color matches.
	 */
	static boolean checkPixelColor(InputStream imageStream, String expectedColor) {
		BufferedImage image;
		int expectedRGB;
		int expectedColorValue;
		final int SAMPLING_OFFSET = 10; // Pixel distance from the edge for sampling

		// ------------------------------------
		// Step 1: Color Conversion and Validation
		// ------------------------------------
		try {
			expectedRGB = parseExpectedColor(expectedColor);
			expectedRGB = new Color(expectedRGB).getRGB();
			expectedColorValue = expectedRGB & 0x00FFFFFF;

		}
		catch (IllegalArgumentException e) {
			System.err.println("Error in expected color: " + e.getMessage());
			return false;
		}

		// ------------------------------------
		// Step 2: Image Reading and Decoding
		// ------------------------------------
		try {
			image = ImageIO.read(imageStream);
			if (image == null) {
				System.err.println("Failed to decode image stream. Not a recognized format.");
				return false;
			}
		}
		catch (IOException e) {
			System.err.println("IO Error reading image stream: " + e.getMessage());
			return false;
		}

		// ------------------------------------
		// Step 3 & 4: Sampling, Comparison, and Matching
		// ------------------------------------
		int width = image.getWidth();
		int height = image.getHeight();

		// Sample points: four corners
		int[] sampleX = { SAMPLING_OFFSET, width - SAMPLING_OFFSET, SAMPLING_OFFSET, width - SAMPLING_OFFSET };
		int[] sampleY = { SAMPLING_OFFSET, SAMPLING_OFFSET, height - SAMPLING_OFFSET, height - SAMPLING_OFFSET };

		for (int i = 0; i < sampleX.length; i++) {
			int x = sampleX[i];
			int y = sampleY[i];

			if (x < 0 || x >= width || y < 0 || y >= height) {
				System.err.println("Sampling point is outside image boundary. Check SAMPLING_OFFSET.");
				continue;
			}

			int actualRGB = image.getRGB(x, y);
			int actualColor = actualRGB & 0x00FFFFFF;

			if (actualColor != expectedColorValue) {
				System.err.printf("Assertion Failed: Pixel color mismatch at (%d, %d). Expected: 0x%X, Found: 0x%X%n",
						x, y, expectedColorValue, actualColor);
				return false;
			}
		}

		// Final Result
		System.out.printf("\t[PASS VALIDATION] Successfully confirmed image color matches: %s (0x%X)%n", expectedColor,
				expectedColorValue);
		return true;
	}

}

/**
 * A.3.1. Abstract Test for Requirement bgcolor parameter definition Implements the
 * verification for the three URL constructions and four assertions required by the WHEN
 * clause.
 */
public class BgcolorParameterDefinition extends CommonFixture {

	// --- Standard parameters required for testing ---
	private static final String DEFAULT_BBOX = "-180,-90,180,90";

	private static final int DEFAULT_WIDTH = 400;

	private static final int DEFAULT_HEIGHT = 400;

	private static final String DEFAULT_FORMAT = "image/png";

	private static final String MAP_REL_TYPE = "http://www.opengis.net/def/rel/ogc/1.0/map";

	/**
	 * Ensures the SUT declares conformance to the required requirement classes. If a
	 * requirement class is not declared, the test must be skipped (not failed).
	 */
	private void requireConformance(String... requiredClassSuffixes) throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		String conformanceUrl = rootUri.toString() + "/conformance";
		HttpURLConnection connection = (HttpURLConnection) new URL(conformanceUrl).openConnection();
		connection.setRequestMethod("GET");
		connection.setRequestProperty("Accept", "application/json");

		if (connection.getResponseCode() != 200) {
			throw new SkipException("Failed to retrieve conformance declaration: HTTP " + connection.getResponseCode());
		}

		Map<String, Object> data = objectMapper.readValue(connection.getInputStream(),
				new TypeReference<Map<String, Object>>() {
				});
		List<String> conformsTo = (List<String>) data.getOrDefault("conformsTo", Collections.emptyList());

		for (String requiredSuffix : requiredClassSuffixes) {
			boolean ok = false;
			for (String uri : conformsTo) {
				if (uri != null && uri.toLowerCase().contains(requiredSuffix.toLowerCase())) {
					ok = true;
					break;
				}
			}
			if (!ok) {
				throw new SkipException("Test Skipped: SUT does not declare conformance to " + requiredSuffix);
			}
		}
	}

	/**
	 * Finds a specific link object from a list based on the relationship type (rel).
	 */
	public static Map<String, Object> findLinkByRel(List<Map<String, Object>> links, String expectedRel) {
		if (links == null) {
			return null;
		}
		for (Map<String, Object> link : links) {
			Object rel = link.get("rel");
			if (expectedRel.equals(rel)) {
				return link;
			}
		}
		return null;
	}

	/**
	 * Helper class to hold map resource information including collection ID and base URL.
	 */
	private static class MapResourceInfo {

		final String collectionId;

		final String baseUrl;

		MapResourceInfo(String collectionId, String baseUrl) {
			this.collectionId = collectionId;
			this.baseUrl = baseUrl;
		}

	}

	/**
	 * Gets the base URL template for the first available /map resource, including f,
	 * bbox, width, and height parameters. Also returns the collection ID for style
	 * lookup.
	 */
	private MapResourceInfo getMapResourceInfo() throws Exception {
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

		List<Map<String, Object>> collectionsList = (List<Map<String, Object>>) data.get("collections");

		for (Map<String, Object> collection : collectionsList) {
			List<Map<String, Object>> collectionLinks = (List<Map<String, Object>>) collection.get("links");

			Map<String, Object> relMap = findLinkByRel(collectionLinks, MAP_REL_TYPE);

			if (relMap != null && relMap.containsKey("href")) {
				String collectionId = (String) collection.get("id");
				String mapUrl = (String) relMap.get("href");
				URI uri = new URI(mapUrl);

				if (!uri.isAbsolute()) {
					uri = rootUri.resolve(uri);
				}

				String baseUrl = uri.toURL().toString() + "?f=" + DEFAULT_FORMAT + "&bbox=" + DEFAULT_BBOX + "&width="
						+ DEFAULT_WIDTH + "&height=" + DEFAULT_HEIGHT;

				return new MapResourceInfo(collectionId, baseUrl);
			}
		}
		throw new SkipException(
				"Test Skipped: No map resources found in the collections to verify. Cannot proceed with A.6.");
	}

	/**
	 * Gets the first available style ID from the collection's styles endpoint.
	 * @param collectionId The collection ID to query styles for.
	 * @return The first style ID, or null if styles endpoint is not available or empty.
	 */
	private String getFirstStyleId(String collectionId) {
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			String stylesUrl = rootUri.toString() + "/collections/" + collectionId + "/styles?f=json";

			HttpURLConnection connection = (HttpURLConnection) new URL(stylesUrl).openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Accept", "application/json");
			connection.setConnectTimeout(10000);
			connection.setReadTimeout(10000);

			if (connection.getResponseCode() != 200) {
				System.out.println("  [Style Discovery] /styles endpoint returned HTTP " + connection.getResponseCode()
						+ ", skipping style test.");
				return null;
			}

			Map<String, Object> data = objectMapper.readValue(connection.getInputStream(),
					new TypeReference<Map<String, Object>>() {
					});

			List<Map<String, Object>> styles = (List<Map<String, Object>>) data.get("styles");
			if (styles == null || styles.isEmpty()) {
				System.out.println("  [Style Discovery] No styles found in the response.");
				return null;
			}

			String styleId = (String) styles.get(0).get("id");
			System.out.println("  [Style Discovery] Found style: " + styleId);
			return styleId;
		}
		catch (Exception e) {
			System.out.println("  [Style Discovery] Failed to retrieve styles: " + e.getMessage());
			return null;
		}
	}

	/**
	 * Gets the background color defined in a style.
	 * @param collectionId The collection ID.
	 * @param styleId The style ID to query.
	 * @return The background color in hex format (e.g., "AADDCC"), or null if not
	 * defined.
	 */
	private String getStyleBackgroundColor(String collectionId, String styleId) {
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			String styleUrl = rootUri.toString() + "/collections/" + collectionId + "/styles/" + styleId + "?f=json";

			HttpURLConnection connection = (HttpURLConnection) new URL(styleUrl).openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Accept", "application/json");
			connection.setConnectTimeout(10000);
			connection.setReadTimeout(10000);

			if (connection.getResponseCode() != 200) {
				System.out
					.println("  [Style Detail] Failed to retrieve style detail: HTTP " + connection.getResponseCode());
				return null;
			}

			Map<String, Object> styleData = objectMapper.readValue(connection.getInputStream(),
					new TypeReference<Map<String, Object>>() {
					});

			// Parse MapBox GL style format - look for background layer
			List<Map<String, Object>> layers = (List<Map<String, Object>>) styleData.get("layers");
			if (layers != null) {
				for (Map<String, Object> layer : layers) {
					String type = (String) layer.get("type");
					if ("background".equals(type)) {
						Map<String, Object> paint = (Map<String, Object>) layer.get("paint");
						if (paint != null) {
							Object bgColor = paint.get("background-color");
							if (bgColor != null) {
								String colorStr = bgColor.toString();
								// Convert #RRGGBB to RRGGBB
								if (colorStr.startsWith("#")) {
									colorStr = colorStr.substring(1);
								}
								System.out.println("  [Style Detail] Found background-color: " + colorStr);
								return colorStr;
							}
						}
					}
				}
			}

			System.out.println("  [Style Detail] No background-color defined in style.");
			return null;
		}
		catch (Exception e) {
			System.out.println("  [Style Detail] Failed to parse style: " + e.getMessage());
			return null;
		}
	}

	/**
	 * Sends a GET request and sets Content-Type
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
	 * <pre>
	 * Abstract test A.3.1
	 *
	 * Identifier: /conf/background/bgcolor-definition
	 * Requirement: Requirement 6: /req/background/bgcolor-definition
	 * Test purpose: Verify that the implementation supports the bgcolor parameter.
	 *
	 * Test method:
	 * Given: a map resource that conformed successfully to /conf/core
	 * When: retrieving a map without bgcolor parameter, with bgcolor using a hexadecimal value and with bgcolor using a W3C Web Color name
	 * Then:
	 * - assert that the map operation supports a bgcolor parameter in hexadecimal red-green-blue color value (from 00 to FF, FF representing 255)
	 *   for the background color of the map. For a six-digit hexadecimal value, the first and second digits specify the intensity of red.
	 *   The third and fourth digits specify the intensity of green. The fifth and sixth digits specify the intensity of blue,
	 * - assert that the map operation supports a bgcolor parameter in case-insensitive W3C web color name for the background color of the map,
	 * - assert that if bgcolor is not specified, and either transparent is set to false or the output format cannot encode transparency,
	 *   and there is an style defined the server uses the background color specified by the requested style,
	 * - assert that if bgcolor is not specified, and either transparent is set to false or the output format cannot encode transparency,
	 *   and there is no style used or the style do not specify a background color, the background color is set to 0xFFFFFF.
	 * </pre>
	 */
	@Test(description = "Implements A.3.1. Abstract Test for Requirement bgcolor parameter definition")
	public void verifyBgcolorDefinition() throws Exception {

		System.out.println("--- Starting A.6 BGCOLOR Tests ---");

		requireConformance("/conf/core", "/conf/background");

		MapResourceInfo mapInfo = getMapResourceInfo();
		String baseUrl = mapInfo.baseUrl;
		String collectionId = mapInfo.collectionId;

		System.out.println("  [Info] Testing collection: " + collectionId);

		// ----------------------------------------------------
		// Case 1: Verify that the bgcolor parameter supports a 6-digit hexadecimal RGB
		// value (RRGGBB) as the map background color.
		// ----------------------------------------------------
		// Validate Hexadecimal Color Value
		String hexColor = "CC00CC"; // Purple

		String testUrlHex = baseUrl + "&bgcolor=" + hexColor + "&transparent=false";
		System.out.println("\n[Case 1] Testing Hex Color: " + hexColor);

		HttpURLConnection connHex = sendMapRequest(testUrlHex);
		Assert.assertEquals(connHex.getResponseCode(), 200,
				"Failed: Hex BGCOLOR request failed with HTTP status. Expected 200.");

		Assert.assertTrue(TestImageValidator.checkPixelColor(connHex.getInputStream(), hexColor),
				"Failed: Map background color must be the specified hex value " + hexColor);

		// ----------------------------------------------------
		// Case 2: Verify that the bgcolor parameter supports W3C web color names and is
		// case-insensitive.
		// ----------------------------------------------------
		System.out.println("\n[Case 2] Testing W3C Web Color Names (case-insensitive)");

		String[] w3cColors = { "NAVY", // baseline
				"NaVy", // mixed case: used to verify case-insensitive color name parsing
				"WHITE", "ReD", "LiMe", "BLUE" };

		for (String colorName : w3cColors) {

			String testUrlWeb = baseUrl + "&bgcolor=" + colorName + "&transparent=false";
			System.out.println("  - Testing W3C color: " + colorName);

			HttpURLConnection connWeb = sendMapRequest(testUrlWeb);

			Assert.assertEquals(connWeb.getResponseCode(), 200,
					"Failed: W3C color request failed for '" + colorName + "'");

			Assert.assertTrue(TestImageValidator.checkPixelColor(connWeb.getInputStream(), colorName),
					"Failed: Map background color must match W3C color name (case-insensitive): " + colorName);
		}

		// ----------------------------------------------------
		// Case 3 & 4: Test style background color or default white
		// Try to automatically discover style and its background color
		// ----------------------------------------------------
		System.out.println("\n[Case 3/4] Testing Style Background Color or Default White");

		String styleId = getFirstStyleId(collectionId);
		String styleBackgroundColor = null;

		if (styleId != null) {
			styleBackgroundColor = getStyleBackgroundColor(collectionId, styleId);
		}

		if (styleId != null && styleBackgroundColor != null) {
			// ----------------------------------------------------
			// Assertion 3: Style has background color defined - use it
			// ----------------------------------------------------
			String testUrlStyle = baseUrl + "&style=" + styleId + "&transparent=false";
			System.out.println("\n[Assertion 3] Testing Style Priority: style=" + styleId + " (Expected: "
					+ styleBackgroundColor + ")");

			HttpURLConnection connStyle = sendMapRequest(testUrlStyle);
			Assert.assertEquals(connStyle.getResponseCode(), 200,
					"Failed: Style Priority request failed. Expected HTTP 200.");

			Assert.assertTrue(TestImageValidator.checkPixelColor(connStyle.getInputStream(), styleBackgroundColor),
					"Failed: Background color must match the color defined by the requested style: "
							+ styleBackgroundColor);
		}
		else {
			// ----------------------------------------------------
			// Assertion 4: No style or style has no background color - default to white
			// ----------------------------------------------------
			String defaultColorHex = "FFFFFF"; // Default White

			String testUrlDefault = baseUrl + "&transparent=false";
			System.out.println("\n[Assertion 4] Testing Default Color (White): " + defaultColorHex);
			System.out.println("  (No style with background-color found, testing default white behavior)");

			HttpURLConnection connDefault = sendMapRequest(testUrlDefault);
			Assert.assertEquals(connDefault.getResponseCode(), 200,
					"Failed: Default White BGCOLOR request failed with HTTP status. Expected 200.");

			Assert.assertTrue(TestImageValidator.checkPixelColor(connDefault.getInputStream(), defaultColorHex),
					"Failed: When BGCOLOR is absent and no style background is defined, background must default to "
							+ defaultColorHex);
		}

	}

}