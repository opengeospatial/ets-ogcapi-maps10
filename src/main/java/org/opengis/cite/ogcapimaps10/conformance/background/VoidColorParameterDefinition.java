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

public class VoidColorParameterDefinition extends CommonFixture {

	private static final int DEFAULT_WIDTH = 400;

	private static final int DEFAULT_HEIGHT = 400;

	private static final String DEFAULT_FORMAT = "image/png";

	private static final String MAP_REL_TYPE = "http://www.opengis.net/def/rel/ogc/1.0/map";

	private static final String VOID_BBOX = "-180,-120,180,120";

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
		connection.setRequestMethod("GET");
		connection.setRequestProperty("Accept", "application/json");

		if (connection.getResponseCode() != 200) {
			throw new SkipException("Failed to retrieve conformance: HTTP " + connection.getResponseCode());
		}

		Map<String, Object> data = objectMapper.readValue(connection.getInputStream(),
				new TypeReference<Map<String, Object>>() {
				});

		List<String> conformsTo = (List<String>) data.get("conformsTo");
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
	 * Returns a base URL template for requesting a map from the first collection
	 * providing a map link. Includes f, bbox, width and height as required parameters for
	 * subsequent appends.
	 */
	private String getMapBaseUrlTemplate() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
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

		List<Map<String, Object>> collections = (List<Map<String, Object>>) data.get("collections");
		if (collections == null || collections.isEmpty()) {
			throw new SkipException("Test Skipped: No collections found.");
		}

		for (Map<String, Object> collection : collections) {
			List<Map<String, Object>> links = (List<Map<String, Object>>) collection.get("links");
			Map<String, Object> mapLink = findLinkByRel(links, MAP_REL_TYPE);
			if (mapLink != null && mapLink.get("href") != null) {
				URI uri = URI.create(mapLink.get("href").toString());
				return uri.toURL().toString() + "?f=" + DEFAULT_FORMAT + "&bbox=" + VOID_BBOX + "&width="
						+ DEFAULT_WIDTH + "&height=" + DEFAULT_HEIGHT;
			}
		}

		throw new SkipException(
				"Test Skipped: No map resources found in the collections to verify. Cannot proceed with A.8.");
	}

	/**
	 * Sends a GET request to the provided map URL. (This was missing in your previous
	 * copy; A.8 requires this helper like A.6/A.7.)
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
		// Case 1: void-color using a hexadecimal value
		// ----------------------------------------------------------
		String hexVoid = "00FF00"; // bright green
		int expectedHexRgb24 = parseExpectedColor(hexVoid);

		System.out.println("\n[Case 1] void-color (hex) = " + hexVoid);

		String urlHex = baseUrl + "&void-color=" + hexVoid;
		HttpURLConnection connHex = sendMapRequest(urlHex);
		Assert.assertEquals(connHex.getResponseCode(), 200,
				"Case 1 Failed: Map request failed (HTTP " + connHex.getResponseCode() + ").");

		assertAnyCornerMatchesVoidColor(connHex.getInputStream(), expectedHexRgb24,
				"Assertion 1 Failed: Server must support void-color as 6-digit hexadecimal RGB and apply it to void areas.");

		// ----------------------------------------------------------
		// Case 2: void-color using a W3C Web Color name (case-insensitive)
		// ----------------------------------------------------------
		String w3cName = "nAvY";
		int expectedW3cRgb24 = parseExpectedColor(w3cName);

		System.out.println("\n[Case 2] void-color (W3C name, case-insensitive) = " + w3cName);

		String urlW3c = baseUrl + "&void-color=" + w3cName;
		HttpURLConnection connW3c = sendMapRequest(urlW3c);
		Assert.assertEquals(connW3c.getResponseCode(), 200,
				"Case 2 Failed: Map request failed (HTTP " + connW3c.getResponseCode() + ").");

		assertAnyCornerMatchesVoidColor(connW3c.getInputStream(), expectedW3cRgb24,
				"Assertion 2 Failed: Server must support void-color as a case-insensitive W3C web color name and apply it to void areas.");

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
				"Case 3 Failed: Map request failed (HTTP " + connNoVoid.getResponseCode() + ").");

		assertAnyCornerMatchesVoidColor(connNoVoid.getInputStream(), expectedBgRgb24,
				"Assertion 3 Failed: If void-color is not specified, void areas must use the same color value as bgcolor (specified or default).");
	}

}
