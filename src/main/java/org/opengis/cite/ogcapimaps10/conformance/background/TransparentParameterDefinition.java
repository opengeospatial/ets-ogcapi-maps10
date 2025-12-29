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
import java.util.List;
import java.util.Map;

public class TransparentParameterDefinition extends CommonFixture {

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
	 * Gets the base URL template for the first available /map resource, including f,
	 * bbox, width, and height parameters.
	 */
	private String getMapBaseUrlTemplate() throws Exception {
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
		if (collectionsList == null || collectionsList.isEmpty()) {
			throw new SkipException("Test Skipped: No collections found.");
		}

		for (Map<String, Object> collection : collectionsList) {
			List<Map<String, Object>> collectionLinks = (List<Map<String, Object>>) collection.get("links");
			Map<String, Object> relMap = findLinkByRel(collectionLinks, MAP_REL_TYPE);
			if (relMap != null && relMap.get("href") != null) {
				URI uri = URI.create(relMap.get("href").toString());
				return uri.toURL().toString() + "?f=" + DEFAULT_FORMAT + "&bbox=" + DEFAULT_BBOX + "&width="
						+ DEFAULT_WIDTH + "&height=" + DEFAULT_HEIGHT;
			}
		}

		throw new SkipException(
				"Test Skipped: No map resources found in the collections to verify. Cannot proceed with A.7.");
	}

	/**
	 * Sends a GET request and sets Accept
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

	// ==========================================================
	// Image helpers (Alpha/opacity checks)
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

		System.out.println(" [Alpha Check] corner(0,0) alpha=" + actualAlpha + " (expected " + expectedAlpha + ")");
		Assert.assertEquals(actualAlpha, expectedAlpha, assertionMessage);
	}

	/**
	 * <pre>
	 * Abstract test A.3.2
	 *
	 * Identifier: /conf/background/transparent-definition
	 * Requirement: Requirement 7: /req/background/transparent-definition
	 * Test purpose: Verify that the implementation supports the transparent parameter
	 *
	 * Test method
	 * Given: a map resource that conformed successfully to /conf/core
	 * When: retrieving a map for all combinations of (no transparent parameter, transparent=false, transparent=true)
	 *       and with and without bgcolor parameter
	 * Then:
	 * - assert that the server interprets transparent as a Boolean indicating whether the background of the map should be transparent,
	 * - assert that, if transparent is not specified and a bgcolor is not specified, the server assumes a value of true,
	 * - assert that, if transparent is not specified and a bgcolor is specified, the server assumes a value of false,
	 * - assert that, if transparent is true and a bgcolor is specified, the server uses 0 for the background’s opacity.
	 * </pre>
	 */
	@Test(description = "Implements A.3.2. Abstract Test for Requirement transparent parameter definition")
	public void verifyTransparentDefinition() throws Exception {

		System.out.println("--- Starting A.7 TRANSPARENT Tests ---");

		requireConformance("/conf/core", "/conf/background");

		String baseUrl = getMapBaseUrlTemplate();

		String testBgColor = "FF00FF"; // Magenta

		// ==========================================================
		// Case 1: transparent NOT specified + bgcolor NOT specified
		// Expectation: server assumes transparent=true
		// ==========================================================
		System.out.println("\n[Case 1] transparent (absent) + bgcolor (absent) => expect transparent=true (alpha=0)");

		HttpURLConnection connCase1 = sendMapRequest(baseUrl);
		Assert.assertEquals(connCase1.getResponseCode(), 200,
				"Case 1 Failed: Map request failed with HTTP status. Expected 200.");

		assertCornerAlpha(connCase1.getInputStream(), 0,
				"Assertion 1 Failed: If transparent is not specified and bgcolor is not specified, server must assume transparent=true (background opacity=0).");

		// ==========================================================
		// Case 2: transparent NOT specified + bgcolor specified
		// Expectation: server assumes transparent=false
		// ==========================================================
		System.out.println("\n[Case 2] transparent (absent) + bgcolor=" + testBgColor
				+ " => expect transparent=false (alpha=255)");

		String urlCase2 = baseUrl + "&bgcolor=" + testBgColor;
		HttpURLConnection connCase2 = sendMapRequest(urlCase2);
		Assert.assertEquals(connCase2.getResponseCode(), 200,
				"Case 2 Failed: Map request failed with HTTP status. Expected 200.");

		assertCornerAlpha(connCase2.getInputStream(), 255,
				"Assertion 2 Failed: If transparent is not specified and bgcolor is specified, server must assume transparent=false (background opacity=255).");

		// ==========================================================
		// Case 3: transparent=false (with and without bgcolor)
		// Expectation: server interprets transparent as Boolean and makes background
		// non-transparent
		// ==========================================================
		System.out.println("\n[Case 3] transparent=false (bgcolor absent) => expect alpha=255");

		String urlCase3a = baseUrl + "&transparent=false";
		HttpURLConnection connCase3a = sendMapRequest(urlCase3a);
		Assert.assertEquals(connCase3a.getResponseCode(), 200,
				"Case 3a Failed: Map request failed with HTTP status. Expected 200.");

		assertCornerAlpha(connCase3a.getInputStream(), 255,
				"Assertion 3 Failed: transparent=false must result in an opaque background (opacity=255).");

		System.out.println("\n[Case 3] transparent=false + bgcolor=" + testBgColor + " => expect alpha=255");

		String urlCase3b = baseUrl + "&transparent=false&bgcolor=" + testBgColor;
		HttpURLConnection connCase3b = sendMapRequest(urlCase3b);
		Assert.assertEquals(connCase3b.getResponseCode(), 200,
				"Case 3b Failed: Map request failed with HTTP status. Expected 200.");

		assertCornerAlpha(connCase3b.getInputStream(), 255,
				"Assertion 3 Failed: transparent=false must remain opaque even when bgcolor is specified (opacity=255).");

		// ==========================================================
		// Case 4: transparent=true + bgcolor NOT specified
		// Expectation: background is transparent (Boolean true)
		// ==========================================================
		System.out.println("\n[Case 4] transparent=true + bgcolor (absent) => expect alpha=0");

		String urlCase4 = baseUrl + "&transparent=true";
		HttpURLConnection connCase4 = sendMapRequest(urlCase4);
		Assert.assertEquals(connCase4.getResponseCode(), 200,
				"Case 4 Failed: Map request failed with HTTP status. Expected 200.");

		assertCornerAlpha(connCase4.getInputStream(), 0,
				"Assertion 4 Failed: transparent=true must result in a fully transparent background (opacity=0).");

		// ==========================================================
		// Case 5: transparent=true + bgcolor specified
		// Expectation: server uses 0 for background opacity
		// ==========================================================
		System.out.println(
				"\n[Case 5] transparent=true + bgcolor=" + testBgColor + " => expect alpha=0 (opacity forced to 0)");

		String urlCase5 = baseUrl + "&transparent=true&bgcolor=" + testBgColor;
		HttpURLConnection connCase5 = sendMapRequest(urlCase5);
		Assert.assertEquals(connCase5.getResponseCode(), 200,
				"Case 5 Failed: Map request failed with HTTP status. Expected 200.");

		assertCornerAlpha(connCase5.getInputStream(), 0,
				"Assertion 5 Failed: If transparent=true and bgcolor is specified, server must use opacity=0 for the background.");
	}

}
