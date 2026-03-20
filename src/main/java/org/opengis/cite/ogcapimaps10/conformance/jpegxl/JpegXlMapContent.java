package org.opengis.cite.ogcapimaps10.conformance.jpegxl;

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
import org.opengis.cite.ogcapimaps10.domain.JpegXlInteractiveTestResult;
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import io.restassured.response.Response;

/**
 * A.18.1. Abstract Test for Requirement JPEG XL Map Content
 *
 * <pre>
 * Abstract test A.57
 *
 * Identifier:    /conf/jpegxl/content
 * Requirement:   /req/jpegxl/content
 * Test purpose:  Verify that the implementation supports retrieving maps
 *                negotiating for JPEG XL content
 *
 * Test method:
 * Given: a map resource that conformed successfully to /conf/core
 * When:  retrieving a JPEG XL (image/jxl) representation of a map resource
 *        through HTTP content negotiation
 * Then:
 * - assert that every 200-response of the server with the media type
 *   image/jxl is a JPEG XL file representing only one map,
 * - assert that the JPEG XL is a color image representing the geospatial
 *   features or coverage values in the map,
 * - assert that all maps representing parts of the same resource or
 *   resources and using the same style follow the same portrayal rules.
 * </pre>
 */
public class JpegXlMapContent extends CommonFixture {

	private static final String MEDIA_TYPE_JXL = "image/jxl";

	private static final String REL_MAP = "https://www.opengis.net/def/rel/ogc/1.0/map";

	/**
	 * JPEG XL file signature: first 2 bytes are FF 0A (naked codestream) or first 12
	 * bytes are 00 00 00 0C 4A 58 4C 20 0D 0A 87 0A (ISOBMFF container).
	 */
	private static final byte[] JXL_CODESTREAM_SIGNATURE = new byte[] { (byte) 0xFF, (byte) 0x0A };

	private static final byte[] JXL_CONTAINER_SIGNATURE = new byte[] { 0x00, 0x00, 0x00, 0x0C, 0x4A, 0x58, 0x4C, 0x20,
			0x0D, 0x0A, (byte) 0x87, 0x0A };

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
	 * Abstract test A.57
	 *
	 * Identifier: /conf/jpegxl/content
	 * Requirement: /req/jpegxl/content
	 * Test purpose: Verify that the implementation supports retrieving maps
	 *               negotiating for JPEG XL content
	 *
	 * Test method:
	 * Given: a map resource that conformed successfully to /conf/core
	 * When: retrieving a JPEG XL (image/jxl) representation of a map resource through HTTP content negotiation
	 * Then:
	 * - assert that every 200-response of the server with the media type image/jxl
	 *   is a JPEG XL file representing only one map
	 * - assert that the JPEG XL is a color image representing the geospatial
	 *   features or coverage values in the map
	 * - assert that all maps representing parts of the same resource or resources
	 *   and using the same style follow the same portrayal rules
	 * </pre>
	 */
	@Test(description = "Implements A.18.1. Abstract Test for Requirement JPEG XL content (Requirement /req/jpegxl/content)")
	public void verifyJpegXlContent() {
		List<String> errors = new ArrayList<>();

		// Request JPEG XL through HTTP content negotiation
		Response response;
		try {
			response = init().accept(MEDIA_TYPE_JXL).when().get(mapUrl);
		}
		catch (Exception e) {
			throw new AssertionError(
					"[JPEG XL] Failed to request map as JPEG XL from " + mapUrl + ": " + e.getMessage());
		}

		int statusCode = response.getStatusCode();

		// ============================================================
		// Part A: Every 200-response with media type image/jxl SHALL be
		// a JPEG XL file representing only one map
		// ============================================================
		if (statusCode != 200) {
			errors.add(String.format("[Part A] Expected HTTP 200 but got %d for JPEG XL map request to %s", statusCode,
					mapUrl));
		}
		else {
			String contentType = response.getContentType();
			if (contentType == null || !contentType.contains(MEDIA_TYPE_JXL)) {
				errors.add(String.format("[Part A] Expected Content-Type containing '%s' but got '%s'", MEDIA_TYPE_JXL,
						contentType));
			}

			byte[] imageBytes = response.asByteArray();

			// Verify JPEG XL file signature
			if (!hasJpegXlSignature(imageBytes)) {
				errors.add("[Part A] Response body does not have a valid JPEG XL file signature "
						+ "(expected naked codestream FF 0A or ISOBMFF container signature)");
			}

			// ============================================================
			// Part B: The JPEG XL SHALL be a color image representing the
			// geospatial features or coverage values in the map
			// ============================================================

			// Automated check: verify the response has non-trivial content
			if (imageBytes.length == 0) {
				errors.add("[Part B] JPEG XL response body is empty. "
						+ "The image should represent geospatial features or coverage values");
			}
		}

		// ============================================================
		// Part B & C: Interactive Verification
		// ============================================================
		// Part B: Visual confirmation that the JPEG XL is a color image
		// representing geospatial features or coverage values
		// Part C: All maps representing parts of the same resource and
		// using the same style SHALL follow the same portrayal
		// rules (requires comparing multiple map responses)

		JpegXlInteractiveTestResult interactiveResult = null;
		try {
			interactiveResult = getJpegXlInteractiveTestResult(testContext);
		}
		catch (SkipException e) {
			// Interactive tests not enabled, skip these checks
		}

		if (interactiveResult != null && interactiveResult.isEnabled()) {
			// Part B: Interactive color verification
			if (!interactiveResult.isColorsRepresentFeatures()) {
				errors.add("[Part B] Interactive verification failed: "
						+ "JPEG XL map is not a color image correctly representing geospatial features "
						+ "or coverage values");
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
			message.append("JPEG XL content verification failed with ").append(errors.size()).append(" error(s):\n");
			for (int i = 0; i < errors.size(); i++) {
				message.append("  ").append(i + 1).append(". ").append(errors.get(i)).append("\n");
			}
			throw new AssertionError(message.toString());
		}
	}

	/**
	 * Retrieves the JpegXlInteractiveTestResult from the test context.
	 * @param context The test context.
	 * @return The JpegXlInteractiveTestResult containing interactive test results.
	 * @throws SkipException if the context or result is null.
	 */
	private JpegXlInteractiveTestResult getJpegXlInteractiveTestResult(ITestContext context) {
		if (context == null) {
			throw new SkipException("Test context is null!");
		}
		Object attribute = context.getSuite().getAttribute(SuiteAttribute.JPEGXL_INTERACTIVE_TEST_RESULT.getName());
		if (attribute == null) {
			throw new SkipException("JPEG XL interactive test result is missing!");
		}
		return (JpegXlInteractiveTestResult) attribute;
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
	 * Checks if the byte array starts with a valid JPEG XL file signature. JPEG XL has
	 * two valid signatures:
	 * <ul>
	 * <li>Naked codestream: FF 0A</li>
	 * <li>ISOBMFF container: 00 00 00 0C 4A 58 4C 20 0D 0A 87 0A</li>
	 * </ul>
	 * @param data The byte array to check.
	 * @return true if the data starts with a valid JPEG XL signature.
	 */
	private boolean hasJpegXlSignature(byte[] data) {
		if (data == null || data.length < JXL_CODESTREAM_SIGNATURE.length) {
			return false;
		}
		// Check naked codestream signature (FF 0A)
		if (matchesSignature(data, JXL_CODESTREAM_SIGNATURE)) {
			return true;
		}
		// Check ISOBMFF container signature
		if (data.length >= JXL_CONTAINER_SIGNATURE.length && matchesSignature(data, JXL_CONTAINER_SIGNATURE)) {
			return true;
		}
		return false;
	}

	/**
	 * Checks if the data starts with the given signature bytes.
	 * @param data The byte array to check.
	 * @param signature The expected signature bytes.
	 * @return true if the data starts with the signature.
	 */
	private boolean matchesSignature(byte[] data, byte[] signature) {
		for (int i = 0; i < signature.length; i++) {
			if (data[i] != signature[i]) {
				return false;
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
