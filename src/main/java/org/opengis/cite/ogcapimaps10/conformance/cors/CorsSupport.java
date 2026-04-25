package org.opengis.cite.ogcapimaps10.conformance.cors;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opengis.cite.ogcapimaps10.conformance.CommonFixture;
import org.opengis.cite.ogcapimaps10.conformance.SuiteAttribute;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A.23.1. Abstract Test for Requirement CORS.
 *
 * <pre>
 * Abstract test A.63
 *
 * Identifier: /conf/cors/cors
 * Requirement: Requirement 63: /req/cors/cors
 * Test purpose: Verify that the implementation completely and correctly
 *               implement CORS
 * </pre>
 */
public class CorsSupport extends CommonFixture {

	private static final String TEST_ORIGIN = "https://example.com";

	private static final String REL_MAP_HTTP = "http://www.opengis.net/def/rel/ogc/1.0/map";

	private static final String REL_MAP_HTTPS = "https://www.opengis.net/def/rel/ogc/1.0/map";

	private static final String REL_TILESETS_MAP_HTTP = "http://www.opengis.net/def/rel/ogc/1.0/tilesets-map";

	private static final String REL_TILESETS_MAP_HTTPS = "https://www.opengis.net/def/rel/ogc/1.0/tilesets-map";

	private static final String REL_SERVICE_DESC = "service-desc";

	private int noOfCollections;

	private final List<ResourceUnderTest> resources = new ArrayList<>();

	@BeforeClass
	public void initParameters(ITestContext context) {
		Object noOfCollectionsAttr = context.getSuite().getAttribute(SuiteAttribute.NO_OF_COLLECTIONS.getName());
		if (noOfCollectionsAttr != null) {
			this.noOfCollections = (Integer) noOfCollectionsAttr;
		}
		else {
			this.noOfCollections = 10;
		}

		discoverResources();
		if (resources.isEmpty()) {
			throw new SkipException("No map-related resources found to verify CORS support.");
		}
	}

	/**
	 * <pre>
	 * Abstract test A.63
	 *
	 * Identifier: /conf/cors/cors
	 * Requirement: Requirement 63: /req/cors/cors
	 * Test purpose: Verify that the implementation completely and correctly
	 *               implement CORS
	 * </pre>
	 */
	@Test(description = "Implements A.23.1. Abstract Test for Requirement CORS (Requirement /req/cors/cors)")
	public void verifyCorsSupport() {
		List<String> errors = new ArrayList<>();

		for (ResourceUnderTest resource : resources) {
			verifyGetCors(resource, errors);
		}

		if (!errors.isEmpty()) {
			StringBuilder message = new StringBuilder();
			message.append("CORS verification failed with ").append(errors.size()).append(" error(s):\n");
			for (int i = 0; i < errors.size(); i++) {
				message.append("  ").append(i + 1).append(". ").append(errors.get(i)).append("\n");
			}
			Assert.fail(message.toString());
		}
	}

	private void discoverResources() {
		Set<String> seen = new LinkedHashSet<>();

		List<Map<String, Object>> collections = readCollections();
		for (Map<String, Object> collection : collections) {
			Object linksObj = collection.get("links");
			if (!(linksObj instanceof List<?>)) {
				continue;
			}
			List<Map<String, Object>> links = castLinks((List<?>) linksObj);
			addLinkResource(links, REL_MAP_HTTP, "collection-map", seen);
			addLinkResource(links, REL_MAP_HTTPS, "collection-map", seen);
			addLinkResource(links, REL_TILESETS_MAP_HTTP, "collection-tilesets-map", seen);
			addLinkResource(links, REL_TILESETS_MAP_HTTPS, "collection-tilesets-map", seen);
		}

		List<Map<String, Object>> landingPageLinks = readLandingPageLinks();
		addLinkResource(landingPageLinks, REL_MAP_HTTP, "dataset-map", seen);
		addLinkResource(landingPageLinks, REL_MAP_HTTPS, "dataset-map", seen);
		addLinkResource(landingPageLinks, REL_TILESETS_MAP_HTTP, "dataset-tilesets-map", seen);
		addLinkResource(landingPageLinks, REL_TILESETS_MAP_HTTPS, "dataset-tilesets-map", seen);

		String apiDefinitionUrl = findServiceDescUrl(landingPageLinks);
		if (apiDefinitionUrl != null) {
			discoverApiDefinitionResources(apiDefinitionUrl, seen);
		}
	}

	private List<Map<String, Object>> readCollections() {
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			String collectionsUrl = getBaseUrl() + "/collections";
			HttpURLConnection connection = openConnection(collectionsUrl, "GET");
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Accept", "application/json");

			Map<String, Object> data = objectMapper.readValue(connection.getInputStream(),
					new TypeReference<Map<String, Object>>() {
					});
			Object collectionsObj = data.get("collections");
			if (!(collectionsObj instanceof List<?>)) {
				return List.of();
			}

			List<Map<String, Object>> collections = ((List<?>) collectionsObj).stream()
				.filter(item -> item instanceof Map)
				.map(item -> (Map<String, Object>) item)
				.toList();

			int limit = Math.min(noOfCollections, collections.size());
			return new ArrayList<>(collections.subList(0, limit));
		}
		catch (Exception e) {
			return List.of();
		}
	}

	private List<Map<String, Object>> readLandingPageLinks() {
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			HttpURLConnection connection = openConnection(getBaseUrl(), "GET");
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Accept", "application/json");

			Map<String, Object> data = objectMapper.readValue(connection.getInputStream(),
					new TypeReference<Map<String, Object>>() {
					});
			Object linksObj = data.get("links");
			if (!(linksObj instanceof List<?>)) {
				return List.of();
			}
			return castLinks((List<?>) linksObj);
		}
		catch (Exception e) {
			return List.of();
		}
	}

	private void discoverApiDefinitionResources(String apiDefinitionUrl, Set<String> seen) {
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			HttpURLConnection connection = openConnection(apiDefinitionUrl, "GET");
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Accept", "application/json");

			Map<String, Object> apiDefinition = objectMapper.readValue(connection.getInputStream(),
					new TypeReference<Map<String, Object>>() {
					});
			Object pathsObj = apiDefinition.get("paths");
			if (!(pathsObj instanceof Map<?, ?> paths)) {
				return;
			}

			for (Object pathKeyObj : paths.keySet()) {
				if (!(pathKeyObj instanceof String path)) {
					continue;
				}
				if (containsTemplateParameter(path)) {
					continue;
				}
				String resourceType = classifyPath(path);
				if (resourceType == null) {
					continue;
				}
				String absoluteUrl = resolveApiPath(apiDefinitionUrl, path);
				if (absoluteUrl == null) {
					continue;
				}
				if (seen.add(resourceType + "|" + absoluteUrl)) {
					resources.add(new ResourceUnderTest(resourceType, absoluteUrl, null));
				}
			}
		}
		catch (Exception e) {
			// Ignore API definition discovery failures and continue with link-based
			// resources.
		}
	}

	private String classifyPath(String path) {
		if (path == null) {
			return null;
		}
		String normalized = path.toLowerCase();
		if (normalized.contains("/styles/") && normalized.endsWith("/map")) {
			return "styled-map";
		}
		if (normalized.contains("/map/tiles/") && normalized.contains("{tilematrix}")
				&& normalized.contains("{tilerow}") && normalized.contains("{tilecol}")) {
			return "tile";
		}
		if (normalized.endsWith("/map/tiles") || normalized.contains("/map/tiles{")) {
			return "tilesets-list";
		}
		if (normalized.contains("/map/tiles/") && !normalized.contains("{tilematrix}")) {
			return "tileset";
		}
		if (normalized.endsWith("/map")) {
			return "map";
		}
		if (normalized.endsWith("/projections")) {
			return "projections";
		}
		return null;
	}

	private void addLinkResource(List<Map<String, Object>> links, String expectedRel, String resourceType,
			Set<String> seen) {
		Map<String, Object> link = findLinkByRel(links, expectedRel);
		if (link == null) {
			return;
		}
		String href = valueAsString(link.get("href"));
		if (href == null || href.isEmpty()) {
			return;
		}
		String absoluteUrl = resolveUrl(getBaseUrl(), href);
		String accept = getAcceptHeader(resourceType, valueAsString(link.get("type")));
		if (seen.add(resourceType + "|" + absoluteUrl)) {
			resources.add(new ResourceUnderTest(resourceType, absoluteUrl, accept));
		}
	}

	private String getAcceptHeader(String resourceType, String linkType) {
		if ("collection-map".equals(resourceType) || "dataset-map".equals(resourceType) || "map".equals(resourceType)
				|| "styled-map".equals(resourceType)) {
			return null;
		}
		if ("collection-tilesets-map".equals(resourceType) || "dataset-tilesets-map".equals(resourceType)
				|| "tilesets-list".equals(resourceType) || "tileset".equals(resourceType)
				|| "projections".equals(resourceType)) {
			return "application/json";
		}
		return linkType;
	}

	private void verifyGetCors(ResourceUnderTest resource, List<String> errors) {
		try {
			HttpURLConnection connection = openConnection(resource.url, "GET");
			connection.setRequestProperty("Origin", TEST_ORIGIN);
			if (resource.accept != null && !resource.accept.isEmpty()) {
				connection.setRequestProperty("Accept", resource.accept);
			}

			int statusCode = connection.getResponseCode();
			if (statusCode == HttpURLConnection.HTTP_UNAVAILABLE) {
				errors.add("GET " + resource.resourceType + " resource '" + resource.url
						+ "' returned HTTP 503 while testing CORS. This looks like a temporary server issue.");
				return;
			}
			if (statusCode >= 400) {
				errors.add("GET " + resource.resourceType + " resource '" + resource.url + "' returned HTTP "
						+ statusCode + " when testing CORS.");
				return;
			}

			String allowOrigin = connection.getHeaderField("Access-Control-Allow-Origin");
			if (!isAllowedOrigin(allowOrigin)) {
				errors.add("GET " + resource.resourceType + " resource '" + resource.url
						+ "' did not return a valid Access-Control-Allow-Origin header for origin " + TEST_ORIGIN
						+ ". Actual value: " + allowOrigin);
			}
		}
		catch (Exception e) {
			errors.add("GET " + resource.resourceType + " resource '" + resource.url
					+ "' could not be validated for CORS: " + e.getMessage());
		}
	}

	private void verifyOptionsCors(ResourceUnderTest resource, List<String> errors) {
		try {
			HttpURLConnection connection = openConnection(resource.url, "OPTIONS");
			connection.setRequestProperty("Origin", TEST_ORIGIN);
			connection.setRequestProperty("Access-Control-Request-Method", "GET");

			int statusCode = connection.getResponseCode();
			if (statusCode == HttpURLConnection.HTTP_UNAVAILABLE) {
				errors.add("OPTIONS " + resource.resourceType + " resource '" + resource.url
						+ "' returned HTTP 503 while testing CORS preflight. This looks like a temporary server issue.");
				return;
			}
			if (statusCode >= 400) {
				errors.add("OPTIONS " + resource.resourceType + " resource '" + resource.url + "' returned HTTP "
						+ statusCode + " when testing CORS preflight.");
				return;
			}

			String allowOrigin = connection.getHeaderField("Access-Control-Allow-Origin");
			if (!isAllowedOrigin(allowOrigin)) {
				errors.add("OPTIONS " + resource.resourceType + " resource '" + resource.url
						+ "' did not return a valid Access-Control-Allow-Origin header for origin " + TEST_ORIGIN
						+ ". Actual value: " + allowOrigin);
			}

			String allowMethods = connection.getHeaderField("Access-Control-Allow-Methods");
			if (allowMethods == null || !containsToken(allowMethods, "GET")) {
				errors.add("OPTIONS " + resource.resourceType + " resource '" + resource.url
						+ "' did not advertise GET in Access-Control-Allow-Methods. Actual value: " + allowMethods);
			}
		}
		catch (Exception e) {
			errors.add("OPTIONS " + resource.resourceType + " resource '" + resource.url
					+ "' could not be validated for CORS preflight: " + e.getMessage());
		}
	}

	private HttpURLConnection openConnection(String targetUrl, String method) throws Exception {
		HttpURLConnection connection = (HttpURLConnection) new URL(targetUrl).openConnection();
		connection.setRequestMethod(method);
		connection.setConnectTimeout(5000);
		connection.setReadTimeout(5000);
		return connection;
	}

	private String findServiceDescUrl(List<Map<String, Object>> links) {
		Map<String, Object> link = findLinkByRel(links, REL_SERVICE_DESC);
		if (link == null) {
			return null;
		}
		String href = valueAsString(link.get("href"));
		return resolveUrl(getBaseUrl(), href);
	}

	private String getBaseUrl() {
		String baseUrl = rootUri.toString();
		if (baseUrl.endsWith("/")) {
			baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
		}
		return baseUrl;
	}

	private List<Map<String, Object>> castLinks(List<?> links) {
		return links.stream().filter(item -> item instanceof Map).map(item -> (Map<String, Object>) item).toList();
	}

	private String resolveUrl(String baseUrl, String url) {
		if (url == null) {
			return null;
		}
		if (url.startsWith("http://") || url.startsWith("https://")) {
			return url;
		}
		try {
			URI base = URI.create(baseUrl);
			return base.resolve(url).toString();
		}
		catch (Exception e) {
			if (url.startsWith("/")) {
				try {
					URI base = URI.create(baseUrl);
					return base.getScheme() + "://" + base.getAuthority() + url;
				}
				catch (Exception ex) {
					return url;
				}
			}
			return url;
		}
	}

	private String resolveApiPath(String apiDefinitionUrl, String path) {
		if (path == null || path.isEmpty()) {
			return null;
		}
		try {
			URI apiUri = URI.create(apiDefinitionUrl);
			String apiPath = apiUri.getPath();
			if (apiPath == null) {
				return null;
			}
			int lastSlash = apiPath.lastIndexOf('/');
			String basePath = lastSlash >= 0 ? apiPath.substring(0, lastSlash + 1) : "/";
			URI base = new URI(apiUri.getScheme(), apiUri.getAuthority(), basePath, null, null);
			return base.resolve(path.startsWith("/") ? "." + path : path).toString();
		}
		catch (Exception e) {
			return null;
		}
	}

	private boolean containsTemplateParameter(String path) {
		return path.contains("{") || path.contains("}");
	}

	private boolean isAllowedOrigin(String allowOrigin) {
		return "*".equals(allowOrigin) || TEST_ORIGIN.equals(allowOrigin);
	}

	private boolean containsToken(String headerValue, String expectedToken) {
		if (headerValue == null) {
			return false;
		}
		for (String token : headerValue.split(",")) {
			if (expectedToken.equalsIgnoreCase(token.trim())) {
				return true;
			}
		}
		return false;
	}

	private String valueAsString(Object value) {
		return value instanceof String ? (String) value : null;
	}

	public static Map<String, Object> findLinkByRel(List<Map<String, Object>> links, String expectedRel) {
		if (links == null) {
			return null;
		}
		for (Map<String, Object> link : links) {
			Object rel = link.get("rel");
			if (expectedRel.equals(rel)) {
				return link;
			}
		}
		return null;
	}

	private static final class ResourceUnderTest {

		private final String resourceType;

		private final String url;

		private final String accept;

		private ResourceUnderTest(String resourceType, String url, String accept) {
			this.resourceType = resourceType;
			this.url = url;
			this.accept = accept;
		}

	}

}
