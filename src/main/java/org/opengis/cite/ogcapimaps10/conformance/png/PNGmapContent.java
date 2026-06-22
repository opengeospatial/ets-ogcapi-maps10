package org.opengis.cite.ogcapimaps10.conformance.png;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.opengis.cite.ogcapimaps10.conformance.CommonFixture;
import org.opengis.cite.ogcapimaps10.conformance.SuiteAttribute;
import org.opengis.cite.ogcapimaps10.domain.PngInteractiveTestResult;
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import io.restassured.response.Response;

/**
 * A.16.1. Abstract Test for Requirement PNG Map Content
 *
 * <pre>
 * Abstract test A.55
 *
 * Identifier:    /conf/png/content
 * Requirement:   /req/png/content
 * Test purpose:  Verify that the implementation supports retrieving maps
 *                negotiating for PNG content
 *
 * Test method:
 * Given: a map resource that conformed successfully to /conf/core
 * When:  retrieving a PNG (image/png) representation of a map resource
 *        through HTTP content negotiation
 * Then:
 * - assert that every 200-response of the server with the media type
 *   image/png is a PNG document representing only one map,
 * - assert that the colors of the PNG represent the geospatial features
 *   or coverage values in the map,
 * - assert that the alpha channel of the PNG is used when partial
 *   transparency is required,
 * - assert that all maps representing parts of the same resource or
 *   resources and using the same style follow the same portrayal rules.
 * </pre>
 */
public class PNGmapContent extends CommonFixture {

	private static final String MEDIA_TYPE_PNG = "image/png";

	private static final String REL_MAP = "https://www.opengis.net/def/rel/ogc/1.0/map";

	/**
	 * PNG file signature: 8 bytes (137 80 78 71 13 10 26 10).
	 */
	private static final byte[] PNG_SIGNATURE = new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A };

	private String mapUrl;

	private ITestContext testContext;

	/**
	 * Discovers the map resource URL from the landing page or collections. The map URL is
	 * found by looking for a link with rel {@code [ogc-rel:map]} (i.e.
	 * {@code https://www.opengis.net/def/rel/ogc/1.0/map}).
	 * @param testContext The test context containing suite attributes.
	 */
	@BeforeClass
	public void discoverMapUrl(ITestContext testContext) {
		this.testContext = testContext;
		this.mapUrl = findMapUrl();
		if (this.mapUrl == null) {
			throw new SkipException("No map resource URL found. The server must provide a link with "
					+ "rel='[ogc-rel:map]' in the landing page or collection descriptions.");
		}
	}

	/**
	 * <pre>
	 * Abstract test A.55
	 *
	 * Identifier: /conf/png/content
	 * Requirement: /req/png/content
	 * Test purpose: Verify that the implementation supports retrieving maps
	 *               negotiating for PNG content
	 *
	 * Test method:
	 * Given: a map resource that conformed successfully to /conf/core
	 * When: retrieving a PNG (image/png) representation of a map resource through HTTP content negotiation
	 * Then:
	 * - assert that every 200-response of the server with the media type image/png
	 *   is a PNG document representing only one map
	 * - assert that the colors of the PNG represent the geospatial features
	 *   or coverage values in the map
	 * - assert that the alpha channel of the PNG is used when partial transparency
	 *   is required
	 * - assert that all maps representing parts of the same resource or resources
	 *   and using the same style follow the same portrayal rules
	 * </pre>
	 */
	@Test(description = "Implements A.16.1. Abstract Test for Requirement PNG content (Requirement /req/png/content)")
	public void verifyPngContent() {
		List<String> errors = new ArrayList<>();

		// Request PNG through HTTP content negotiation
		Response response;
		try {
			response = init().accept(MEDIA_TYPE_PNG).when().get(mapUrl);
		}
		catch (Exception e) {
			throw new AssertionError("[PNG] Failed to request map as PNG from " + mapUrl + ": " + e.getMessage());
		}

		int statusCode = response.getStatusCode();

		// ============================================================
		// Part A: Every 200-response with media type image/png SHALL be
		// a PNG document representing only one map
		// ============================================================
		if (statusCode != 200) {
			errors.add(String.format("[Part A] Expected HTTP 200 but got %d for PNG map request to %s", statusCode,
					mapUrl));
		}
		else {
			String contentType = response.getContentType();
			if (contentType == null || !contentType.contains(MEDIA_TYPE_PNG)) {
				errors.add(String.format("[Part A] Expected Content-Type containing '%s' but got '%s'", MEDIA_TYPE_PNG,
						contentType));
			}

			byte[] imageBytes = response.asByteArray();

			// Verify PNG file signature
			if (!hasPngSignature(imageBytes)) {
				errors.add("[Part A] Response body does not have a valid PNG file signature");
			}

			// Verify it is a readable PNG image
			BufferedImage image = readImage(imageBytes);
			if (image == null) {
				errors.add("[Part A] Response body could not be decoded as a valid PNG image");
			}
			else {
				if (image.getWidth() <= 0 || image.getHeight() <= 0) {
					errors.add(String.format("[Part A] PNG image has invalid dimensions: %dx%d", image.getWidth(),
							image.getHeight()));
				}

				// ============================================================
				// Part B: The colors of the PNG SHALL represent the geospatial
				// features or coverage values in the map
				// ============================================================

				// Automated check: verify the image is not completely blank
				if (isCompletelyBlank(image)) {
					errors.add("[Part B] PNG image appears to be completely blank "
							+ "(all pixels are the same color). The colors should represent "
							+ "geospatial features or coverage values");
				}

				// ============================================================
				// Part C: The alpha channel of the PNG SHALL be used when
				// partial transparency is required
				// ============================================================
				if (!image.getColorModel().hasAlpha()) {
					errors.add("[Part C] PNG image does not support alpha channel. "
							+ "The PNG format SHALL support alpha channel for partial " + "transparency when required");
				}
			}
		}

		// ============================================================
		// Part B & D: Interactive Verification
		// ============================================================
		// Part B: Visual confirmation that colors represent geospatial
		// features or coverage values
		// Part D: All maps representing parts of the same resource and
		// using the same style SHALL follow the same portrayal
		// rules (requires comparing multiple map responses)

		PngInteractiveTestResult interactiveResult = null;
		try {
			interactiveResult = getPngInteractiveTestResult(testContext);
		}
		catch (SkipException e) {
			// Interactive tests not enabled, skip these checks
		}

		if (interactiveResult != null && interactiveResult.isEnabled()) {
			// Part B: Interactive color verification
			if (!interactiveResult.isColorsRepresentFeatures()) {
				errors.add("[Part B] Interactive verification failed: "
						+ "PNG map colors do not correctly represent geospatial features " + "or coverage values");
			}

			// Part D: Interactive portrayal consistency verification
			if (!interactiveResult.isPortrayalConsistent()) {
				errors.add("[Part D] Interactive verification failed: "
						+ "Maps representing parts of the same resource do not follow " + "the same portrayal rules");
			}
		}

		// Clear binary image data from response logging before assertion.
		// Binary content in the response stream causes EARL report XML
		// to be truncated.
		clearMessages();

		if (!errors.isEmpty()) {
			StringBuilder message = new StringBuilder();
			message.append("PNG content verification failed with ").append(errors.size()).append(" error(s):\n");
			for (int i = 0; i < errors.size(); i++) {
				message.append("  ").append(i + 1).append(". ").append(errors.get(i)).append("\n");
			}
			throw new AssertionError(message.toString());
		}
	}

	/**
	 * Retrieves the PngInteractiveTestResult from the test context.
	 * @param context The test context.
	 * @return The PngInteractiveTestResult containing interactive test results.
	 * @throws SkipException if the context or result is null.
	 */
	private PngInteractiveTestResult getPngInteractiveTestResult(ITestContext context) {
		if (context == null) {
			throw new SkipException("Test context is null!");
		}
		Object attribute = context.getSuite().getAttribute(SuiteAttribute.PNG_INTERACTIVE_TEST_RESULT.getName());
		if (attribute == null) {
			throw new SkipException("PNG interactive test result is missing!");
		}
		return (PngInteractiveTestResult) attribute;
	}

	/**
	 * Finds the map resource URL by searching the landing page and collections for a link
	 * with rel {@code [ogc-rel:map]}.
	 * @return The map URL, or null if not found.
	 */
	private String findMapUrl() {
		String baseUrl = rootUri.toString();
		if (baseUrl.endsWith("/")) {
			baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
		}

		// 1. Try dataset-level: landing page [ogc-rel:map] link -> /map
		String url = findMapUrlFromLandingPage(baseUrl);
		if (url != null) {
			return url;
		}

		// 2. Try collection-level: collection [ogc-rel:map] link ->
		// /collections/{id}/map
		return findMapUrlFromCollections(baseUrl + "/collections");
	}

	/**
	 * Finds a map URL from the landing page links.
	 * @param landingPageUrl The landing page URL.
	 * @return The map URL, or null if not found.
	 */
	private String findMapUrlFromLandingPage(String landingPageUrl) {
		try {
			Response response = init().accept("application/json").when().get(landingPageUrl);
			if (response.getStatusCode() == 200) {
				List<Map<String, Object>> links = response.jsonPath().getList("links");
				String href = findLinkHrefByRel(links, REL_MAP);
				if (href != null) {
					return resolveUrl(landingPageUrl, href);
				}
			}
		}
		catch (Exception e) {
			// Landing page check failed, continue to collections
		}
		return null;
	}

	/**
	 * Finds a map URL from collection descriptions.
	 * @param collectionsUrl The collections endpoint URL.
	 * @return The map URL, or null if not found.
	 */
	private String findMapUrlFromCollections(String collectionsUrl) {
		try {
			Response response = init().accept("application/json").when().get(collectionsUrl);
			if (response.getStatusCode() == 200) {
				List<Map<String, Object>> collections = response.jsonPath().getList("collections");
				if (collections != null) {
					for (Map<String, Object> collection : collections) {
						List<Map<String, Object>> links = toMapList(collection.get("links"));
						String href = findLinkHrefByRel(links, REL_MAP);
						if (href != null) {
							return resolveUrl(collectionsUrl, href);
						}
					}
				}
			}
		}
		catch (Exception e) {
			// Collections check failed
		}
		return null;
	}

	/**
	 * Finds the href of a link matching the given rel value, with HTTP/HTTPS scheme
	 * normalization.
	 * @param links The list of link objects.
	 * @param rel The expected rel value.
	 * @return The href string, or null if not found.
	 */
	private String findLinkHrefByRel(List<Map<String, Object>> links, String rel) {
		if (links == null) {
			return null;
		}
		for (Map<String, Object> link : links) {
			Object linkRel = link.get("rel");
			if (linkRel instanceof String && matchesRelIgnoringScheme((String) linkRel, rel)) {
				return (String) link.get("href");
			}
		}
		return null;
	}

	/**
	 * Resolves a potentially relative URL against a base URL.
	 * @param baseUrl The base URL.
	 * @param url The URL to resolve (may be relative or absolute).
	 * @return The resolved absolute URL.
	 */
	private String resolveUrl(String baseUrl, String url) {
		if (url == null) {
			return null;
		}
		if (url.startsWith("http://") || url.startsWith("https://")) {
			return url;
		}
		try {
			java.net.URI base = java.net.URI.create(baseUrl);
			return base.resolve(url).toString();
		}
		catch (Exception e) {
			if (url.startsWith("/")) {
				try {
					java.net.URI base = java.net.URI.create(baseUrl);
					return base.getScheme() + "://" + base.getAuthority() + url;
				}
				catch (Exception ex) {
					return url;
				}
			}
			return url;
		}
	}

	/**
	 * Reads an image from a byte array.
	 * @param imageBytes The image bytes.
	 * @return The BufferedImage, or null if reading fails.
	 */
	private BufferedImage readImage(byte[] imageBytes) {
		try {
			return ImageIO.read(new ByteArrayInputStream(imageBytes));
		}
		catch (IOException e) {
			return null;
		}
	}

	/**
	 * Checks if the byte array starts with the PNG file signature.
	 * @param data The byte array to check.
	 * @return true if the data starts with the PNG signature.
	 */
	private boolean hasPngSignature(byte[] data) {
		if (data == null || data.length < PNG_SIGNATURE.length) {
			return false;
		}
		for (int i = 0; i < PNG_SIGNATURE.length; i++) {
			if (data[i] != PNG_SIGNATURE[i]) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Checks if an image is completely blank (all pixels are the same color).
	 * @param image The image to check.
	 * @return true if all pixels have the same RGB value.
	 */
	private boolean isCompletelyBlank(BufferedImage image) {
		if (image.getWidth() == 0 || image.getHeight() == 0) {
			return true;
		}
		int firstPixel = image.getRGB(0, 0);
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				if (image.getRGB(x, y) != firstPixel) {
					return false;
				}
			}
		}
		return true;
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
	 * Safely converts an Object to a List of Maps.
	 * @param obj The object to convert.
	 * @return The list of maps, or null if conversion fails.
	 */
	private List<Map<String, Object>> toMapList(Object obj) {
		if (!(obj instanceof List)) {
			return null;
		}
		List<?> list = (List<?>) obj;
		List<Map<String, Object>> result = new ArrayList<>();
		for (Object item : list) {
			if (item instanceof Map) {
				Map<?, ?> map = (Map<?, ?>) item;
				Map<String, Object> typedMap = new HashMap<>();
				for (Map.Entry<?, ?> entry : map.entrySet()) {
					if (entry.getKey() instanceof String) {
						typedMap.put((String) entry.getKey(), entry.getValue());
					}
				}
				result.add(typedMap);
			}
		}
		return result;
	}

}
