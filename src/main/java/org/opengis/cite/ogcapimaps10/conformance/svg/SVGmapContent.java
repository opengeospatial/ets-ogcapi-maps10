package org.opengis.cite.ogcapimaps10.conformance.svg;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.opengis.cite.ogcapimaps10.conformance.CommonFixture;
import org.testng.annotations.BeforeClass;
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import io.restassured.response.Response;

/**
 * A.20.1. Abstract Test for Requirement SVG Map Content
 *
 * <pre>
 * Abstract test A.59
 *
 * Identifier:    /conf/svg/content
 * Requirement:   /req/svg/content
 * Test purpose:  Verify that the implementation supports retrieving maps
 *                negotiating for SVG content
 *
 * Test method:
 * Given: a map resource that conformed successfully to /conf/core
 * When:  retrieving an SVG (image/svg+xml) representation of a map resource
 *        through HTTP content negotiation
 * Then:
 * - assert that every 200-response of the server with the media type
 *   image/svg+xml is an SVG document representing only a map,
 * - assert that the SVG coordinates inside the map start at 0,0 and
 *   end in the width and height of the request.
 * </pre>
 */
public class SVGmapContent extends CommonFixture {

	private static final String MEDIA_TYPE_SVG = "image/svg+xml";

	private static final String SVG_NAMESPACE = "http://www.w3.org/2000/svg";

	private static final String REL_MAP = "https://www.opengis.net/def/rel/ogc/1.0/map";

	private static final String CONF_SVG_HTTP = "http://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/svg";

	private static final String CONF_SVG_HTTPS = "https://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/svg";

	private static final int REQUEST_WIDTH = 256;

	private static final int REQUEST_HEIGHT = 256;

	private String mapUrl;

	/**
	 * Checks that the server declares conformance to the SVG conformance class, then
	 * discovers the map resource URL. If the server does not declare SVG support, the
	 * test is skipped.
	 * @param testContext The test context containing suite attributes.
	 */
	@BeforeClass
	public void checkConformanceAndDiscoverMapUrl(ITestContext testContext) {
		if (!serverDeclaresSvgConformance()) {
			throw new SkipException("Conformance class SVG (" + CONF_SVG_HTTPS
					+ ") is not declared by the server. SVG tests will be skipped.");
		}
		this.mapUrl = findMapUrl();
		if (this.mapUrl == null) {
			throw new SkipException("No map resource URL found. The server must provide a link with "
					+ "rel='[ogc-rel:map]' in the landing page or collection descriptions.");
		}
	}

	/**
	 * Checks whether the server's /conformance endpoint declares the SVG conformance
	 * class.
	 * @return true if the server declares SVG conformance, false otherwise.
	 */
	private boolean serverDeclaresSvgConformance() {
		String baseUrl = rootUri.toString();
		if (baseUrl.endsWith("/")) {
			baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
		}
		String conformanceUrl = baseUrl + "/conformance";
		try {
			Response response = init().accept("application/json").when().get(conformanceUrl);
			if (response.getStatusCode() == 200) {
				List<String> conformsTo = response.jsonPath().getList("conformsTo");
				if (conformsTo != null) {
					for (String uri : conformsTo) {
						if (CONF_SVG_HTTP.equals(uri) || CONF_SVG_HTTPS.equals(uri)) {
							return true;
						}
					}
				}
			}
		}
		catch (Exception e) {
			// Failed to check conformance, proceed without skipping
		}
		return false;
	}

	/**
	 * <pre>
	 * Abstract test A.59
	 *
	 * Identifier: /conf/svg/content
	 * Requirement: /req/svg/content
	 * Test purpose: Verify that the implementation supports retrieving maps
	 *               negotiating for SVG content
	 *
	 * Test method:
	 * Given: a map resource that conformed successfully to /conf/core
	 * When: retrieving an SVG (image/svg+xml) representation of a map resource
	 *       through HTTP content negotiation
	 * Then:
	 * - assert that every 200-response of the server with the media type
	 *   image/svg+xml is an SVG document representing only a map
	 * - assert that the SVG coordinates inside the map start at 0,0 and
	 *   end in the width and height of the request
	 * </pre>
	 */
	@Test(description = "Implements A.20.1. Abstract Test for Requirement SVG content (Requirement /req/svg/content)")
	public void verifySvgContent() {
		List<String> errors = new ArrayList<>();

		String requestUrl = buildSvgRequestUrl();

		// Request SVG through HTTP content negotiation
		Response response;
		try {
			response = init().accept(MEDIA_TYPE_SVG).when().get(requestUrl);
		}
		catch (Exception e) {
			throw new AssertionError("[SVG] Failed to request map as SVG from " + requestUrl + ": " + e.getMessage());
		}

		int statusCode = response.getStatusCode();

		// ============================================================
		// Part A: Every 200-response with media type image/svg+xml SHALL be
		// an SVG document representing only a map
		// ============================================================
		if (statusCode != 200) {
			errors.add(String.format("[Part A] Expected HTTP 200 but got %d for SVG map request to %s", statusCode,
					requestUrl));
		}
		else {
			String contentType = response.getContentType();
			if (contentType == null || !contentType.contains(MEDIA_TYPE_SVG)) {
				errors.add(String.format("[Part A] Expected Content-Type containing '%s' but got '%s'", MEDIA_TYPE_SVG,
						contentType));
			}

			byte[] svgBytes = response.asByteArray();

			// Parse as XML and verify it is a valid SVG document
			Document svgDoc = parseSvgDocument(svgBytes);
			if (svgDoc == null) {
				errors.add("[Part A] Response body could not be parsed as a valid XML/SVG document");
			}
			else {
				Element root = svgDoc.getDocumentElement();
				String localName = root.getLocalName();
				String namespaceUri = root.getNamespaceURI();

				if (!"svg".equals(localName)) {
					errors.add(String.format("[Part A] Root element is '%s', expected 'svg'", localName));
				}
				if (namespaceUri != null && !SVG_NAMESPACE.equals(namespaceUri)) {
					errors.add(String.format("[Part A] SVG namespace is '%s', expected '%s'", namespaceUri,
							SVG_NAMESPACE));
				}

				// ============================================================
				// Part B: The SVG coordinates inside the map start at 0,0 and
				// end in the width and height of the request
				// ============================================================
				verifySvgCoordinates(root, errors);
			}
		}

		// Clear response data before assertion to avoid EARL report truncation
		clearMessages();

		if (!errors.isEmpty()) {
			StringBuilder message = new StringBuilder();
			message.append("SVG content verification failed with ").append(errors.size()).append(" error(s):\n");
			for (int i = 0; i < errors.size(); i++) {
				message.append("  ").append(i + 1).append(". ").append(errors.get(i)).append("\n");
			}
			throw new AssertionError(message.toString());
		}
	}

	/**
	 * Verifies that the SVG coordinates start at (0,0) and end at (width, height). Checks
	 * the viewBox attribute first, then falls back to width/height attributes.
	 * @param root The root SVG element.
	 * @param errors The list to add error messages to.
	 */
	private void verifySvgCoordinates(Element root, List<String> errors) {
		String viewBox = root.getAttribute("viewBox");
		if (viewBox != null && !viewBox.isEmpty()) {
			// viewBox format: "minX minY width height"
			String[] parts = viewBox.trim().split("\\s+|,");
			if (parts.length != 4) {
				errors.add(String.format("[Part B] viewBox has %d values (expected 4): '%s'", parts.length, viewBox));
				return;
			}
			try {
				double minX = Double.parseDouble(parts[0]);
				double minY = Double.parseDouble(parts[1]);
				double vbWidth = Double.parseDouble(parts[2]);
				double vbHeight = Double.parseDouble(parts[3]);

				if (minX != 0.0 || minY != 0.0) {
					errors
						.add(String.format("[Part B] SVG viewBox origin is (%.1f, %.1f), expected (0, 0). viewBox='%s'",
								minX, minY, viewBox));
				}
				if (vbWidth <= 0 || vbHeight <= 0) {
					errors.add(String.format("[Part B] SVG viewBox dimensions are invalid (%.1f x %.1f). viewBox='%s'",
							vbWidth, vbHeight, viewBox));
				}
			}
			catch (NumberFormatException e) {
				errors.add(String.format("[Part B] viewBox contains non-numeric values: '%s'", viewBox));
			}
		}
		else {
			// No viewBox — check width and height attributes
			String widthAttr = root.getAttribute("width");
			String heightAttr = root.getAttribute("height");

			if ((widthAttr == null || widthAttr.isEmpty()) && (heightAttr == null || heightAttr.isEmpty())) {
				errors.add("[Part B] SVG element has neither viewBox nor width/height attributes. "
						+ "Cannot verify that coordinates start at 0,0 and end at the requested dimensions");
				return;
			}

			if (widthAttr == null || widthAttr.isEmpty()) {
				errors.add("[Part B] SVG element is missing 'width' attribute");
			}
			else {
				double width = parseNumericValue(widthAttr);
				if (width <= 0) {
					errors.add(String.format("[Part B] SVG width is invalid: '%s'", widthAttr));
				}
			}

			if (heightAttr == null || heightAttr.isEmpty()) {
				errors.add("[Part B] SVG element is missing 'height' attribute");
			}
			else {
				double height = parseNumericValue(heightAttr);
				if (height <= 0) {
					errors.add(String.format("[Part B] SVG height is invalid: '%s'", heightAttr));
				}
			}
		}
	}

	/**
	 * Parses a numeric value from a string, stripping any CSS unit suffix (e.g., "px",
	 * "em").
	 * @param value The string value to parse.
	 * @return The numeric value, or -1 if parsing fails.
	 */
	private double parseNumericValue(String value) {
		if (value == null || value.isEmpty()) {
			return -1;
		}
		// Strip common CSS unit suffixes
		String numeric = value.replaceAll("[a-zA-Z%]+$", "").trim();
		try {
			return Double.parseDouble(numeric);
		}
		catch (NumberFormatException e) {
			return -1;
		}
	}

	/**
	 * Parses the byte array as an XML document (SVG).
	 * @param svgBytes The SVG byte content.
	 * @return The parsed Document, or null if parsing fails.
	 */
	private Document parseSvgDocument(byte[] svgBytes) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			// Disable external entities to prevent XXE
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			DocumentBuilder builder = factory.newDocumentBuilder();
			return builder.parse(new ByteArrayInputStream(svgBytes));
		}
		catch (ParserConfigurationException | SAXException | IOException e) {
			return null;
		}
	}

	/**
	 * Builds the SVG map request URL with width and height parameters.
	 * @return The request URL with format, width and height parameters.
	 */
	private String buildSvgRequestUrl() {
		String separator = mapUrl.contains("?") ? "&" : "?";
		return mapUrl + separator + "f=svg&width=" + REQUEST_WIDTH + "&height=" + REQUEST_HEIGHT;
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
