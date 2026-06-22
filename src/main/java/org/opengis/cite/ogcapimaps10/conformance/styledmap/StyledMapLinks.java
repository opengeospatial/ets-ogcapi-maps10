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
 * A.15.1. Abstract Test for Requirement styled map links.
 *
 * <pre>
 * Abstract test A.53
 *
 * Identifier: /conf/styled-map/desc-links
 * Requirement: Requirement 53: /req/styled-map/desc-links
 * Test purpose: Verify that the implementation links correctly from a style
 *               resource to a map resource.
 * </pre>
 */
public class StyledMapLinks extends CommonFixture {

	private static final String MAP_REL_HTTPS = "https://www.opengis.net/def/rel/ogc/1.0/map";

	private static final String MAP_REL_HTTP = "http://www.opengis.net/def/rel/ogc/1.0/map";

	private static final String MAP_REL_COMPACT = "ogc-rel:map";

	/**
	 * <pre>
	 * Abstract test A.53
	 *
	 * Identifier: /conf/styled-map/desc-links
	 * Requirement: Requirement 53: /req/styled-map/desc-links
	 * Test purpose: Verify that the implementation links correctly from a style
	 *               resource to a map resource.
	 * </pre>
	 */
	@Test(description = "Implements A.15.1. Abstract Test for Requirement styled map links "
			+ "(Requirement /req/styled-map/desc-links)")
	public void verifyStyledMapLinks() {
		List<StylesList> stylesLists = discoverStylesLists();
		if (stylesLists.isEmpty()) {
			throw new SkipException("No dataset or collection styles list was found.");
		}

		List<String> errors = new ArrayList<>();
		for (StylesList stylesList : stylesLists) {
			validateStylesList(stylesList, errors);
		}

		if (!errors.isEmpty()) {
			StringBuilder message = new StringBuilder();
			message.append("Styled map link verification failed with ").append(errors.size()).append(" error(s):\n");
			for (int i = 0; i < errors.size(); i++) {
				message.append("  ").append(i + 1).append(". ").append(errors.get(i)).append("\n");
			}
			Assert.fail(message.toString());
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
			// The endpoint exists but did not provide a JSON styles list.
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

	private void validateStylesList(StylesList stylesList, List<String> errors) {
		List<Map<String, Object>> styles = stylesList.json.getList("styles");
		if (styles == null || styles.isEmpty()) {
			return;
		}
		for (Map<String, Object> style : styles) {
			validateStyle(URI.create(stylesList.url), stylesList.collectionId, style, errors);
		}
	}

	static void validateStyle(URI stylesListUri, String collectionId, Map<String, Object> style, List<String> errors) {
		String styleId = valueAsString(style.get("id"));
		if (styleId == null || styleId.isEmpty()) {
			errors.add("Style resource in '" + stylesListUri + "' does not provide an id.");
			return;
		}

		Object linksObject = style.get("links");
		if (!(linksObject instanceof List<?>)) {
			errors.add("Style resource '" + styleId + "' in '" + stylesListUri + "' does not provide links.");
			return;
		}

		Map<String, Object> mapLink = findMapLink((List<?>) linksObject);
		if (mapLink == null) {
			errors.add("Style resource '" + styleId + "' in '" + stylesListUri
					+ "' does not include a map link with rel '" + MAP_REL_HTTPS + "'.");
			return;
		}

		String href = valueAsString(mapLink.get("href"));
		if (href == null || href.isEmpty()) {
			errors.add("Style resource '" + styleId + "' in '" + stylesListUri + "' has a map link without href.");
			return;
		}

		if (!pointsToStyledMap(stylesListUri, collectionId, styleId, href)) {
			errors.add("Style resource '" + styleId + "' in '" + stylesListUri + "' has map link href '" + href
					+ "' that does not point to the map associated with that styled resource.");
		}
	}

	private static Map<String, Object> findMapLink(List<?> links) {
		for (Object linkObject : links) {
			if (!(linkObject instanceof Map<?, ?>)) {
				continue;
			}
			Map<String, Object> link = (Map<String, Object>) linkObject;
			String rel = valueAsString(link.get("rel"));
			if (MAP_REL_HTTPS.equals(rel) || MAP_REL_HTTP.equals(rel) || MAP_REL_COMPACT.equals(rel)) {
				return link;
			}
		}
		return null;
	}

	private static boolean pointsToStyledMap(URI stylesListUri, String collectionId, String styleId, String href) {
		URI mapUri = stylesListUri.resolve(href);
		String encodedStyleId = encodePathSegment(styleId);
		String expectedSuffix = collectionId == null ? "/styles/" + encodedStyleId + "/map"
				: "/collections/" + encodePathSegment(collectionId) + "/styles/" + encodedStyleId + "/map";
		return normalizePath(mapUri.getRawPath()).endsWith(expectedSuffix);
	}

	static String encodePathSegment(String value) {
		try {
			return URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20");
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Could not encode URI path segment: " + value, e);
		}
	}

	private static String normalizePath(String path) {
		if (path == null) {
			return "";
		}
		return path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
	}

	private String resolvePath(String path) {
		String base = rootUri.toString();
		if (!base.endsWith("/")) {
			base = base + "/";
		}
		String relativePath = path.startsWith("/") ? path.substring(1) : path;
		return URI.create(base).resolve(relativePath).toString();
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
