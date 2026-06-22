package org.opengis.cite.ogcapimaps10.conformance.styledmap;

import static io.restassured.http.Method.GET;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opengis.cite.ogcapimaps10.conformance.CommonFixture;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

/**
 * A.15.2. Abstract Test for Requirement styled map operation.
 *
 * <pre>
 * Abstract test A.54
 *
 * Identifier: /conf/styled-map/map-operation
 * Requirement: Requirement 54: /req/styled-map/map-operation
 * Test purpose: Verify that the implementation supports retrieving maps from
 *               OGC API — Styles style resources.
 * </pre>
 */
public class StyledMapOperation extends CommonFixture {

	private static final String MAP_REL_HTTPS = "https://www.opengis.net/def/rel/ogc/1.0/map";

	private static final String MAP_REL_HTTP = "http://www.opengis.net/def/rel/ogc/1.0/map";

	private static final String MAP_REL_COMPACT = "ogc-rel:map";

	/**
	 * <pre>
	 * Abstract test A.54
	 *
	 * Identifier: /conf/styled-map/map-operation
	 * Requirement: Requirement 54: /req/styled-map/map-operation
	 * Test purpose: Verify that the implementation supports retrieving maps from
	 *               OGC API — Styles style resources.
	 *
	 * Test method:
	 * Given: a style correctly linking to a map resource as per /conf/styled-map/desc-links
	 * When: retrieving a map for that style as per /conf/core
	 * Then: assert that every resource for which a styled map is available supports an
	 *       HTTP GET operation to a .../styles/{styleId}/map URL to retrieve a map for
	 *       a particular style (e.g., /collections/{collectionId}/styles/{styleId}/map
	 *       for a styled collection map or /styles/{styleId}/map for a styled dataset map).
	 * </pre>
	 */
	@Test(description = "Implements A.15.2. Abstract Test for Requirement styled map operation "
			+ "(Requirement /req/styled-map/map-operation)")
	public void verifyStyledMapOperation() {
		List<StylesList> stylesLists = discoverStylesLists();
		if (stylesLists.isEmpty()) {
			throw new SkipException("No dataset or collection styles list was found.");
		}

		boolean anyMapLinkFound = false;
		List<String> errors = new ArrayList<>();

		for (StylesList stylesList : stylesLists) {
			List<Map<String, Object>> styles = stylesList.json.getList("styles");
			if (styles == null || styles.isEmpty()) {
				continue;
			}
			for (Map<String, Object> style : styles) {
				String href = findMapLinkHref(style);
				if (href == null) {
					continue;
				}
				anyMapLinkFound = true;
				String mapUrl = resolveHref(URI.create(stylesList.url), href);
				validateMapResponse(mapUrl, style, stylesList, errors);
			}
		}

		if (!anyMapLinkFound) {
			throw new SkipException("No styled map links were found in any style resource.");
		}

		if (!errors.isEmpty()) {
			StringBuilder message = new StringBuilder();
			message.append("Styled map operation verification failed with ")
				.append(errors.size())
				.append(" error(s):\n");
			for (int i = 0; i < errors.size(); i++) {
				message.append("  ").append(i + 1).append(". ").append(errors.get(i)).append("\n");
			}
			Assert.fail(message.toString());
		}
	}

	private void validateMapResponse(String mapUrl, Map<String, Object> style, StylesList stylesList,
			List<String> errors) {
		String styleId = valueAsString(style.get("id"));
		Response response = init().baseUri(mapUrl).accept("image/png,image/jpeg,image/*").when().request(GET);

		int statusCode = response.statusCode();
		if (statusCode < 200 || statusCode >= 300) {
			errors.add("HTTP GET to styled map URL '" + mapUrl + "' (style '" + styleId + "' in '" + stylesList.url
					+ "') returned status " + statusCode + " instead of 2xx.");
			return;
		}

		String contentType = response.contentType();
		if (contentType == null || !contentType.startsWith("image/")) {
			errors.add("HTTP GET to styled map URL '" + mapUrl + "' (style '" + styleId + "' in '" + stylesList.url
					+ "') returned Content-Type '" + contentType + "', expected image/*.");
		}
	}

	private List<StylesList> discoverStylesLists() {
		List<StylesList> stylesLists = new ArrayList<>();
		Set<String> seen = new LinkedHashSet<>();

		addStylesList(stylesLists, seen, resolvePath("/styles"), null);
		for (String collectionId : readCollectionIds()) {
			addStylesList(stylesLists, seen, resolvePath("/collections/" + encodePathSegment(collectionId) + "/styles"),
					collectionId);
		}

		return stylesLists;
	}

	private void addStylesList(List<StylesList> stylesLists, Set<String> seen, String stylesListUrl,
			String collectionId) {
		if (!seen.add(stylesListUrl)) {
			return;
		}
		Response response = init().baseUri(stylesListUrl).accept("application/json").when().request(GET);
		if (response.statusCode() < 200 || response.statusCode() >= 300) {
			return;
		}
		try {
			JsonPath json = response.jsonPath();
			if (json.getList("styles") != null) {
				stylesLists.add(new StylesList(stylesListUrl, collectionId, json));
			}
		}
		catch (RuntimeException e) {
			// not a valid JSON styles list
		}
	}

	private List<String> readCollectionIds() {
		Response response = init().baseUri(resolvePath("/collections")).accept("application/json").when().request(GET);
		if (response.statusCode() < 200 || response.statusCode() >= 300) {
			return Collections.emptyList();
		}
		List<Map<String, Object>> collections;
		try {
			collections = response.jsonPath().getList("collections");
		}
		catch (RuntimeException e) {
			return Collections.emptyList();
		}
		if (collections == null) {
			return Collections.emptyList();
		}

		List<String> collectionIds = new ArrayList<>();
		for (Map<String, Object> collection : collections) {
			Object id = collection.get("id");
			if (id instanceof String) {
				collectionIds.add((String) id);
			}
		}
		return collectionIds;
	}

	private String findMapLinkHref(Map<String, Object> style) {
		Object linksObject = style.get("links");
		if (!(linksObject instanceof List<?>)) {
			return null;
		}
		for (Object linkObject : (List<?>) linksObject) {
			if (!(linkObject instanceof Map<?, ?>)) {
				continue;
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> link = (Map<String, Object>) linkObject;
			String rel = valueAsString(link.get("rel"));
			if (MAP_REL_HTTPS.equals(rel) || MAP_REL_HTTP.equals(rel) || MAP_REL_COMPACT.equals(rel)) {
				return valueAsString(link.get("href"));
			}
		}
		return null;
	}

	private String resolveHref(URI base, String href) {
		return base.resolve(href).toString();
	}

	private String resolvePath(String path) {
		String base = rootUri.toString();
		if (!base.endsWith("/")) {
			base = base + "/";
		}
		String relativePath = path.startsWith("/") ? path.substring(1) : path;
		return URI.create(base).resolve(relativePath).toString();
	}

	private static String encodePathSegment(String value) {
		try {
			return URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20");
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Could not encode URI path segment: " + value, e);
		}
	}

	private static String valueAsString(Object value) {
		return value instanceof String ? (String) value : null;
	}

	private static final class StylesList {

		private final String url;

		private final String collectionId;

		private final JsonPath json;

		private StylesList(String url, String collectionId, JsonPath json) {
			this.url = url;
			this.collectionId = collectionId;
			this.json = json;
		}

	}

}
