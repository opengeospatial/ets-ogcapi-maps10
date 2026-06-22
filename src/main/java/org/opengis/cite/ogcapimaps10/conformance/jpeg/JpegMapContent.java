package org.opengis.cite.ogcapimaps10.conformance.jpeg;

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
import org.opengis.cite.ogcapimaps10.domain.JpegInteractiveTestResult;
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import io.restassured.response.Response;

/**
 * A.17.1. Abstract Test for Requirement JPEG Map Content
 *
 * <pre>
 * Abstract test A.56
 *
 * Identifier:    /conf/jpeg/content
 * Requirement:   /req/jpeg/content
 * Test purpose:  Verify that the implementation supports retrieving maps
 *                negotiating for JPEG content
 *
 * Test method:
 * Given: a map resource that conformed successfully to /conf/core
 * When:  retrieving a JPEG (image/jpeg) representation of a map resource
 *        through HTTP content negotiation
 * Then:
 * - assert that every 200-response of the server with the media type
 *   image/jpeg is a JPEG document representing only one map,
 * - assert that the colors of the JPEG represent geospatial features
 *   and/or coverage values in the map,
 * - assert that all maps representing parts of the same resource or
 *   resources and using the same style follow the same portrayal rules.
 * </pre>
 */
public class JpegMapContent extends CommonFixture {

	private static final String MEDIA_TYPE_JPEG = "image/jpeg";

	private static final String REL_MAP = "https://www.opengis.net/def/rel/ogc/1.0/map";

	/**
	 * JPEG file signature: first 2 bytes are FF D8 (SOI - Start of Image).
	 */
	private static final byte[] JPEG_SIGNATURE = new byte[] { (byte) 0xFF, (byte) 0xD8 };

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
	 * Abstract test A.56
	 *
	 * Identifier: /conf/jpeg/content
	 * Requirement: /req/jpeg/content
	 * Test purpose: Verify that the implementation supports retrieving maps
	 *               negotiating for JPEG content
	 *
	 * Test method:
	 * Given: a map resource that conformed successfully to /conf/core
	 * When: retrieving a JPEG (image/jpeg) representation of a map resource through HTTP content negotiation
	 * Then:
	 * - assert that every 200-response of the server with the media type image/jpeg
	 *   is a JPEG document representing only one map
	 * - assert that the colors of the JPEG represent geospatial features
	 *   and/or coverage values in the map
	 * - assert that all maps representing parts of the same resource or resources
	 *   and using the same style follow the same portrayal rules
	 * </pre>
	 */
	@Test(description = "Implements A.17.1. Abstract Test for Requirement JPEG content (Requirement /req/jpeg/content)")
	public void verifyJpegContent() {
		List<String> errors = new ArrayList<>();

		// Request JPEG through HTTP content negotiation
		Response response;
		try {
			response = init().accept(MEDIA_TYPE_JPEG).when().get(mapUrl);
		}
		catch (Exception e) {
			throw new AssertionError("[JPEG] Failed to request map as JPEG from " + mapUrl + ": " + e.getMessage());
		}

		int statusCode = response.getStatusCode();

		// ============================================================
		// Part A: Every 200-response with media type image/jpeg SHALL be
		// a JPEG document representing only one map
		// ============================================================
		if (statusCode != 200) {
			errors.add(String.format("[Part A] Expected HTTP 200 but got %d for JPEG map request to %s", statusCode,
					mapUrl));
		}
		else {
			String contentType = response.getContentType();
			if (contentType == null || !contentType.contains(MEDIA_TYPE_JPEG)) {
				errors.add(String.format("[Part A] Expected Content-Type containing '%s' but got '%s'", MEDIA_TYPE_JPEG,
						contentType));
			}

			byte[] imageBytes = response.asByteArray();

			// Verify JPEG file signature
			if (!hasJpegSignature(imageBytes)) {
				errors.add("[Part A] Response body does not have a valid JPEG file signature");
			}

			// Verify it is a readable JPEG image
			BufferedImage image = readImage(imageBytes);
			if (image == null) {
				errors.add("[Part A] Response body could not be decoded as a valid JPEG image");
			}
			else {
				if (image.getWidth() <= 0 || image.getHeight() <= 0) {
					errors.add(String.format("[Part A] JPEG image has invalid dimensions: %dx%d", image.getWidth(),
							image.getHeight()));
				}

				// ============================================================
				// Part B: The colors of the JPEG SHALL represent geospatial
				// features and/or coverage values in the map
				// ============================================================

				// Automated check: verify the image is not completely blank
				if (isCompletelyBlank(image)) {
					errors.add("[Part B] JPEG image appears to be completely blank "
							+ "(all pixels are the same color). The colors should represent "
							+ "geospatial features and/or coverage values");
				}
			}
		}

		// ============================================================
		// Part B & C: Interactive Verification
		// ============================================================
		// Part B: Visual confirmation that colors represent geospatial
		// features and/or coverage values
		// Part C: All maps representing parts of the same resource and
		// using the same style SHALL follow the same portrayal
		// rules (requires comparing multiple map responses)

		JpegInteractiveTestResult interactiveResult = null;
		try {
			interactiveResult = getJpegInteractiveTestResult(testContext);
		}
		catch (SkipException e) {
			// Interactive tests not enabled, skip these checks
		}

		if (interactiveResult != null && interactiveResult.isEnabled()) {
			// Part B: Interactive color verification
			if (!interactiveResult.isColorsRepresentFeatures()) {
				errors.add("[Part B] Interactive verification failed: "
						+ "JPEG map colors do not correctly represent geospatial features " + "and/or coverage values");
			}

			// Part C: Interactive portrayal consistency verification
			if (!interactiveResult.isPortrayalConsistent()) {
				errors.add("[Part C] Interactive verification failed: "
						+ "Maps representing parts of the same resource do not follow " + "the same portrayal rules");
			}
		}

		// Clear binary image data from response logging before assertion.
		// Binary content in the response stream causes EARL report XML
		// to be truncated.
		clearMessages();

		if (!errors.isEmpty()) {
			StringBuilder message = new StringBuilder();
			message.append("JPEG content verification failed with ").append(errors.size()).append(" error(s):\n");
			for (int i = 0; i < errors.size(); i++) {
				message.append("  ").append(i + 1).append(". ").append(errors.get(i)).append("\n");
			}
			throw new AssertionError(message.toString());
		}
	}

	/**
	 * Retrieves the JpegInteractiveTestResult from the test context.
	 * @param context The test context.
	 * @return The JpegInteractiveTestResult containing interactive test results.
	 * @throws SkipException if the context or result is null.
	 */
	private JpegInteractiveTestResult getJpegInteractiveTestResult(ITestContext context) {
		if (context == null) {
			throw new SkipException("Test context is null!");
		}
		Object attribute = context.getSuite().getAttribute(SuiteAttribute.JPEG_INTERACTIVE_TEST_RESULT.getName());
		if (attribute == null) {
			throw new SkipException("JPEG interactive test result is missing!");
		}
		return (JpegInteractiveTestResult) attribute;
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
	 * Checks if the byte array starts with the JPEG file signature (SOI marker: FF D8).
	 * @param data The byte array to check.
	 * @return true if the data starts with the JPEG signature.
	 */
	private boolean hasJpegSignature(byte[] data) {
		if (data == null || data.length < JPEG_SIGNATURE.length) {
			return false;
		}
		for (int i = 0; i < JPEG_SIGNATURE.length; i++) {
			if (data[i] != JPEG_SIGNATURE[i]) {
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
