package org.opengis.cite.ogcapimaps10.conformance.html;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opengis.cite.ogcapimaps10.conformance.CommonFixture;
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import io.restassured.response.Response;

/**
 * A.21.1. Abstract Test for Requirement HTML Map Content
 *
 * <pre>
 * Abstract test A.60
 *
 * Identifier: /conf/html/content
 * Requirement: Requirement 60: /req/html/content
 * Test purpose: Verify that the implementation supports retrieving maps
 *                negotiating for HTML content
 *
 * Test method:
 * Given: a map resource that conformed successfully to /conf/core
 * When: retrieving an (text/html) HTML representation of a map resource
 *        HTTP content negotiation
 * Then:
 * - assert that every 200-response of the server with the media type
 *   text/html is an HTML document representing the geospatial data as maps.
 * </pre>
 */
public class HTMLmapContent extends CommonFixture {

	private static final String MEDIA_TYPE_HTML = "text/html";

	private static final String REL_MAP = "https://www.opengis.net/def/rel/ogc/1.0/map";

	private static final String CONF_HTML_HTTP = "http://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/html";

	private static final String CONF_HTML_HTTPS = "https://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/html";

	private String mapUrl;

	/**
	 * Checks that the server declares conformance to the HTML conformance class, then
	 * discovers the map resource URL. If the server does not declare HTML support, the
	 * test is skipped.
	 * @param testContext The test context containing suite attributes.
	 */
	@BeforeClass
	public void checkConformanceAndDiscoverMapUrl(ITestContext testContext) {
		if (!serverDeclaresHtmlConformance()) {
			throw new SkipException("Conformance class HTML (" + CONF_HTML_HTTPS
					+ ") is not declared by the server. HTML tests will be skipped.");
		}
		this.mapUrl = findMapUrl();
		if (this.mapUrl == null) {
			throw new SkipException("No map resource URL found. The server must provide a link with "
					+ "rel='[ogc-rel:map]' in the landing page or collection descriptions.");
		}
	}

	/**
	 * Checks whether the server's /conformance endpoint declares the HTML conformance
	 * class.
	 * @return true if the server declares HTML conformance, false otherwise.
	 */
	private boolean serverDeclaresHtmlConformance() {
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
						if (CONF_HTML_HTTP.equals(uri) || CONF_HTML_HTTPS.equals(uri)) {
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
	 * Abstract test A.60
	 *
	 * Identifier: /conf/html/content
	 * Requirement: Requirement 60: /req/html/content
	 * Test purpose: Verify that the implementation supports retrieving maps
	 *               negotiating for HTML content
	 *
	 * Test method:
	 * Given: a map resource that conformed successfully to /conf/core
	 * When: retrieving an (text/html) HTML representation of a map resource
	 *       HTTP content negotiation
	 * Then:
	 * - assert that every 200-response of the server with the media type
	 *   text/html is an HTML document representing the geospatial data as maps
	 * </pre>
	 */
	@Test(description = "Implements A.21.1. Abstract Test for Requirement HTML content (Requirement /req/html/content)")
	public void verifyHtmlContent() {
		List<String> errors = new ArrayList<>();

		// Request HTML through HTTP content negotiation
		Response response;
		try {
			response = init().accept(MEDIA_TYPE_HTML).when().get(mapUrl);
		}
		catch (Exception e) {
			throw new AssertionError("[HTML] Failed to request map as HTML from " + mapUrl + ": " + e.getMessage());
		}

		int statusCode = response.getStatusCode();

		// ============================================================
		// Part A: Every 200-response with media type text/html SHALL be
		// an HTML document representing the geospatial data as maps
		// ============================================================
		if (statusCode != 200) {
			errors.add(String.format("[Part A] Expected HTTP 200 but got %d for HTML map request to %s", statusCode,
					mapUrl));
		}
		else {
			String contentType = response.getContentType();
			if (contentType == null || !contentType.contains(MEDIA_TYPE_HTML)) {
				errors.add(String.format("[Part A] Expected Content-Type containing '%s' but got '%s'", MEDIA_TYPE_HTML,
						contentType));
			}

			String htmlBody = response.asString();

			// Verify the response body is not empty
			if (htmlBody == null || htmlBody.trim().isEmpty()) {
				errors.add("[Part A] Response body is empty");
			}
			else {
				// Verify it contains basic HTML structure
				String lowerBody = htmlBody.toLowerCase();

				if (!lowerBody.contains("<html") && !lowerBody.contains("<!doctype html")) {
					errors.add("[Part A] Response body does not appear to be a valid HTML document "
							+ "(missing <html> element or <!DOCTYPE html> declaration)");
				}
			}
		}

		// Clear response data before assertion to avoid EARL report truncation
		clearMessages();

		if (!errors.isEmpty()) {
			StringBuilder message = new StringBuilder();
			message.append("HTML content verification failed with ").append(errors.size()).append(" error(s):\n");
			for (int i = 0; i < errors.size(); i++) {
				message.append("  ").append(i + 1).append(". ").append(errors.get(i)).append("\n");
			}
			throw new AssertionError(message.toString());
		}
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
