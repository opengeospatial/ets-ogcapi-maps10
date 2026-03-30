package org.opengis.cite.ogcapimaps10.conformance.tiff;

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
import org.opengis.cite.ogcapimaps10.domain.TiffInteractiveTestResult;
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import io.restassured.response.Response;

/**
 * A.19.1. Abstract Test for Requirement TIFF Map Content
 *
 * <pre>
 * Abstract test A.58
 *
 * Identifier:    /conf/tiff/content
 * Requirement:   /req/tiff/content
 * Test purpose:  Verify that the implementation supports retrieving maps
 *                negotiating for TIFF and/or GeoTIFF content
 *
 * Test method:
 * Given: a map resource that conformed successfully to /conf/core
 * When:  retrieving a TIFF (image/tiff) and GeoTIFF
 *        (image/tiff; application=geotiff) representation of a map resource
 *        through HTTP content negotiation
 * Then:
 * - assert that every 200-response of the server with the media type
 *   image/tiff is a TIFF document representing only one map,
 * - assert that the TIFF file represents colors by using an image palette
 *   or RGB combination,
 * - assert that all maps representing parts of the same resource or
 *   resources and using the same style follow the same portrayal rules
 *   or represent data with the same reference and units of measure.
 * </pre>
 */
public class TIFFmapContent extends CommonFixture {

	private static final String MEDIA_TYPE_TIFF = "image/tiff";

	private static final String REL_MAP = "https://www.opengis.net/def/rel/ogc/1.0/map";

	/**
	 * TIFF file signature: first two bytes are either "II" (0x49 0x49, little-endian) or
	 * "MM" (0x4D 0x4D, big-endian), followed by the magic number 42 (0x002A).
	 */
	private static final byte[] TIFF_SIGNATURE_LE = new byte[] { 0x49, 0x49, 0x2A, 0x00 };

	private static final byte[] TIFF_SIGNATURE_BE = new byte[] { 0x4D, 0x4D, 0x00, 0x2A };

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
	 * Abstract test A.58
	 *
	 * Identifier: /conf/tiff/content
	 * Requirement: /req/tiff/content
	 * Test purpose: Verify that the implementation supports retrieving maps
	 *               negotiating for TIFF and/or GeoTIFF content
	 *
	 * Test method:
	 * Given: a map resource that conformed successfully to /conf/core
	 * When: retrieving a TIFF (image/tiff) and GeoTIFF (image/tiff; application=geotiff)
	 *       representation of a map resource through HTTP content negotiation
	 * Then:
	 * - assert that every 200-response of the server with the media type image/tiff
	 *   is a TIFF document representing only one map
	 * - assert that the TIFF file represents colors by using an image palette
	 *   or RGB combination
	 * - assert that all maps representing parts of the same resource or resources
	 *   and using the same style follow the same portrayal rules or represent data
	 *   with the same reference and units of measure
	 * </pre>
	 */
	@Test(description = "Implements A.19.1. Abstract Test for Requirement TIFF content (Requirement /req/tiff/content)")
	public void verifyTiffContent() {
		List<String> errors = new ArrayList<>();

		// Request TIFF through HTTP content negotiation
		Response response;
		try {
			response = init().accept(MEDIA_TYPE_TIFF).when().get(mapUrl);
		}
		catch (Exception e) {
			throw new AssertionError("[TIFF] Failed to request map as TIFF from " + mapUrl + ": " + e.getMessage());
		}

		int statusCode = response.getStatusCode();

		// ============================================================
		// Part A: Every 200-response with media type image/tiff SHALL be
		// a TIFF document representing only one map
		// ============================================================
		if (statusCode != 200) {
			errors.add(String.format("[Part A] Expected HTTP 200 but got %d for TIFF map request to %s", statusCode,
					mapUrl));
		}
		else {
			String contentType = response.getContentType();
			if (contentType == null || !contentType.contains(MEDIA_TYPE_TIFF)) {
				errors.add(String.format("[Part A] Expected Content-Type containing '%s' but got '%s'", MEDIA_TYPE_TIFF,
						contentType));
			}

			byte[] imageBytes = response.asByteArray();

			// Verify TIFF file signature
			if (!hasTiffSignature(imageBytes)) {
				errors.add("[Part A] Response body does not have a valid TIFF file signature");
			}

			// Verify it is a readable TIFF image
			BufferedImage image = readImage(imageBytes);
			if (image == null) {
				errors.add("[Part A] Response body could not be decoded as a valid TIFF image");
			}
			else {
				if (image.getWidth() <= 0 || image.getHeight() <= 0) {
					errors.add(String.format("[Part A] TIFF image has invalid dimensions: %dx%d", image.getWidth(),
							image.getHeight()));
				}

				// ============================================================
				// Part B: The TIFF file SHALL represent colors by using an
				// image palette or RGB combination
				// ============================================================
				if (!usesIndexedOrRgbColorModel(image)) {
					errors.add("[Part B] TIFF image does not use an image palette or RGB color model. "
							+ "The TIFF file SHALL represent colors by using an image palette " + "or RGB combination");
				}
			}
		}

		// ============================================================
		// Part C: Interactive Verification
		// ============================================================
		// All maps representing parts of the same resource and
		// using the same style SHALL follow the same portrayal
		// rules or represent data with the same reference and
		// units of measure

		TiffInteractiveTestResult interactiveResult = null;
		try {
			interactiveResult = getTiffInteractiveTestResult(testContext);
		}
		catch (SkipException e) {
			// Interactive tests not enabled, skip these checks
		}

		if (interactiveResult != null && interactiveResult.isEnabled()) {
			// Part C: Interactive portrayal consistency verification
			if (!interactiveResult.isPortrayalConsistent()) {
				errors.add("[Part C] Interactive verification failed: "
						+ "Maps representing parts of the same resource do not follow "
						+ "the same portrayal rules or represent data with the same "
						+ "reference and units of measure");
			}
		}

		// Clear binary image data from response logging before assertion.
		// Binary content in the response stream causes EARL report XML
		// to be truncated.
		clearMessages();

		if (!errors.isEmpty()) {
			StringBuilder message = new StringBuilder();
			message.append("TIFF content verification failed with ").append(errors.size()).append(" error(s):\n");
			for (int i = 0; i < errors.size(); i++) {
				message.append("  ").append(i + 1).append(". ").append(errors.get(i)).append("\n");
			}
			throw new AssertionError(message.toString());
		}
	}

	/**
	 * Retrieves the TiffInteractiveTestResult from the test context.
	 * @param context The test context.
	 * @return The TiffInteractiveTestResult containing interactive test results.
	 * @throws SkipException if the context or result is null.
	 */
	private TiffInteractiveTestResult getTiffInteractiveTestResult(ITestContext context) {
		if (context == null) {
			throw new SkipException("Test context is null!");
		}
		Object attribute = context.getSuite().getAttribute(SuiteAttribute.TIFF_INTERACTIVE_TEST_RESULT.getName());
		if (attribute == null) {
			throw new SkipException("TIFF interactive test result is missing!");
		}
		return (TiffInteractiveTestResult) attribute;
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
	 * Checks if the byte array starts with a valid TIFF file signature. TIFF files begin
	 * with either "II" (little-endian) or "MM" (big-endian) followed by 42.
	 * @param data The byte array to check.
	 * @return true if the data starts with a valid TIFF signature.
	 */
	private boolean hasTiffSignature(byte[] data) {
		if (data == null || data.length < 4) {
			return false;
		}
		return matchesSignature(data, TIFF_SIGNATURE_LE) || matchesSignature(data, TIFF_SIGNATURE_BE);
	}

	private boolean matchesSignature(byte[] data, byte[] signature) {
		for (int i = 0; i < signature.length; i++) {
			if (data[i] != signature[i]) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Checks if the image uses an indexed (palette) or RGB color model.
	 * @param image The image to check.
	 * @return true if the image uses IndexColorModel or has at least 3 RGB components.
	 */
	private boolean usesIndexedOrRgbColorModel(BufferedImage image) {
		int imageType = image.getType();
		// Indexed/palette types
		if (imageType == BufferedImage.TYPE_BYTE_INDEXED || imageType == BufferedImage.TYPE_BYTE_BINARY) {
			return true;
		}
		// RGB types
		if (imageType == BufferedImage.TYPE_INT_RGB || imageType == BufferedImage.TYPE_INT_ARGB
				|| imageType == BufferedImage.TYPE_INT_ARGB_PRE || imageType == BufferedImage.TYPE_INT_BGR
				|| imageType == BufferedImage.TYPE_3BYTE_BGR || imageType == BufferedImage.TYPE_4BYTE_ABGR
				|| imageType == BufferedImage.TYPE_4BYTE_ABGR_PRE) {
			return true;
		}
		// Fallback: check color model components
		int numComponents = image.getColorModel().getNumColorComponents();
		return numComponents >= 3 || image.getColorModel() instanceof java.awt.image.IndexColorModel;
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
