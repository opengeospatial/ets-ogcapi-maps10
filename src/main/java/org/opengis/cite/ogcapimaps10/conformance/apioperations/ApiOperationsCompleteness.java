package org.opengis.cite.ogcapimaps10.conformance.apioperations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opengis.cite.ogcapimaps10.conformance.CommonFixture;
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import io.restassured.response.Response;

/**
 * A.22.1. Abstract Test for Requirement API Operations completeness
 *
 * <pre>
 * Abstract test A.61
 *
 * Identifier:    /conf/api-operations/completeness
 * Requirement:   /req/api-operations/completeness
 * Test purpose:  Verify that the implementation completely and correctly
 *                describes the map resources
 *
 * Test method:
 * Given: an API conforming to OGC API — Common — Part 1: Core "Landing Page"
 *        conformance class,
 * When:  retrieving the API description
 * Then:
 * - assert that the API definition provides paths for all map, custom
 *   projections, tileset, tilesets list and tile resources provided by
 *   the API instance,
 * - assert that the resource paths defined in the API definition are
 *   consistent with the links to the same resources provided by the
 *   landing page, collections, tileset and tilesets list resources,
 * - assert that the resource paths defined in the API definition provide
 *   the description of the parameters that the map, tileset and tile
 *   resources need to operate that are specified in corresponding
 *   conformance classes.
 * </pre>
 */
public class ApiOperationsCompleteness extends CommonFixture {

	private static final String CONF_API_OPERATIONS = "http://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/api-operations";

	private static final String REL_SERVICE_DESC = "service-desc";

	private static final String REL_MAP = "http://www.opengis.net/def/rel/ogc/1.0/map";

	private static final String REL_TILESETS_MAP = "http://www.opengis.net/def/rel/ogc/1.0/tilesets-map";

	/** Map resource link paths discovered from landing page and collections. */
	private Set<String> actualMapPaths;

	/** Map tileset link paths discovered from landing page and collections. */
	private Set<String> actualTilesetPaths;

	/** Paths defined in the OpenAPI definition. */
	private Set<String> apiDefinitionPaths;

	/** Parameters defined per path in the OpenAPI definition. */
	private Map<String, Set<String>> apiDefinitionParameters;

	/** Conformance classes declared by the server. */
	private Set<String> conformanceClasses;

	/**
	 * Base path prefix from the root URI (e.g., "/ogcapi") to strip from actual paths.
	 */
	private String basePath;

	/**
	 * Checks preconditions: the server must declare support for the api-operations
	 * conformance class, and a valid API definition must be retrievable.
	 * @param testContext The test context containing suite attributes.
	 */
	@BeforeClass
	public void setup(ITestContext testContext) {
		// Determine the base path prefix (e.g., "/ogcapi") to strip from actual paths
		this.basePath = rootUri.getPath();
		if (this.basePath != null && this.basePath.endsWith("/")) {
			this.basePath = this.basePath.substring(0, this.basePath.length() - 1);
		}

		// Step 1: Check /conformance for api-operations support
		this.conformanceClasses = fetchConformanceClasses();
		if (!containsIgnoringScheme(conformanceClasses, CONF_API_OPERATIONS)) {
			throw new SkipException(
					"Server does not declare conformance class " + CONF_API_OPERATIONS + ". Skipping A.61 tests.");
		}

		// Step 2: Fetch and parse OpenAPI definition (left side)
		String apiDefinitionUrl = findServiceDescUrl();
		if (apiDefinitionUrl == null) {
			throw new SkipException("No API definition URL found (rel='service-desc') in landing page.");
		}
		parseApiDefinition(apiDefinitionUrl);

		// Step 3: Discover actual map resource links (right side)
		discoverActualResources();
	}

	/**
	 * <pre>
	 * Abstract test A.61
	 *
	 * Identifier: /conf/api-operations/completeness
	 * Requirement: /req/api-operations/completeness
	 * Test purpose: Verify that the implementation completely and correctly
	 *               describes the map resources
	 *
	 * Test method:
	 * Given: an API conforming to OGC API — Common — Part 1: Core
	 *        "Landing Page" conformance class
	 * When: retrieving the API description
	 * Then:
	 * - assert that the API definition provides paths for all map, custom
	 *   projections, tileset, tilesets list and tile resources
	 * - assert that the resource paths are consistent with the links
	 *   provided by the landing page, collections, tileset and tilesets
	 *   list resources
	 * - assert that the resource paths provide the description of the
	 *   parameters specified in corresponding conformance classes
	 * </pre>
	 */
	@Test(description = "Implements A.22.1. Abstract Test for Requirement API Operations completeness "
			+ "(Requirement /req/api-operations/completeness)")
	public void verifyApiOperationsCompleteness() {
		List<String> errors = new ArrayList<>();

		// ============================================================
		// Part A: The API definition SHALL provide paths for all map,
		// custom projections, tileset, tilesets list and tile resources
		// provided by the API instance.
		// ============================================================
		for (String actualPath : actualMapPaths) {
			if (!hasMatchingApiPath(actualPath, apiDefinitionPaths)) {
				errors.add("[Part A] Map resource link '" + actualPath
						+ "' has no matching path pattern in the API definition");
			}
		}

		for (String actualPath : actualTilesetPaths) {
			if (!hasMatchingApiPath(actualPath, apiDefinitionPaths)) {
				errors.add("[Part A] Tileset resource link '" + actualPath
						+ "' has no matching path pattern in the API definition");
			}
		}

		// ============================================================
		// Part B: The resource paths defined in the API definition SHALL
		// be consistent with the links to the same resources provided by
		// the landing page, collections, tileset and tilesets list
		// resources.
		// ============================================================
		Set<String> apiMapPaths = new HashSet<>();
		for (String path : apiDefinitionPaths) {
			if (path.contains("/map")) {
				apiMapPaths.add(path);
			}
		}

		for (String apiPath : apiMapPaths) {
			// Only check concrete paths (not template paths like
			// /collections/{collectionId}/map)
			if (!apiPath.contains("{")) {
				boolean found = actualMapPaths.contains(apiPath) || actualTilesetPaths.contains(apiPath);
				if (!found) {
					errors.add("[Part B] API definition path '" + apiPath
							+ "' has no corresponding resource link from landing page or collections");
				}
			}
		}

		for (String actualPath : actualMapPaths) {
			if (!hasMatchingApiPath(actualPath, apiDefinitionPaths)) {
				errors.add("[Part B] Resource link '" + actualPath
						+ "' is not consistent with any path in the API definition");
			}
		}

		// ============================================================
		// Part C: The resource paths defined in the API definition SHALL
		// provide the description of the parameters that the map, tileset
		// and tile resources need to operate that are specified in
		// corresponding conformance classes.
		// ============================================================
		Map<String, String> conformanceToParam = new HashMap<>();
		conformanceToParam.put("http://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/spatial-subsetting", "bbox");
		conformanceToParam.put("http://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/scaling", "width");
		conformanceToParam.put("http://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/display-resolution", "mm-per-pixel");
		conformanceToParam.put("http://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/crs", "crs");
		conformanceToParam.put("http://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/background", "bgcolor");
		conformanceToParam.put("http://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/datetime", "datetime");
		conformanceToParam.put("http://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/general-subsetting", "subset");
		conformanceToParam.put("http://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/collections-selection",
				"collections");

		Set<String> requiredParams = new HashSet<>();
		for (Map.Entry<String, String> entry : conformanceToParam.entrySet()) {
			if (containsIgnoringScheme(conformanceClasses, entry.getKey())) {
				requiredParams.add(entry.getValue());
			}
		}

		for (Map.Entry<String, Set<String>> entry : apiDefinitionParameters.entrySet()) {
			String path = entry.getKey();
			// Only check direct map paths (e.g., /map, /collections/{id}/map)
			// Skip paths that don't contain /map
			if (!path.contains("/map")) {
				continue;
			}
			// Skip tileset-related sub-paths (/map/tiles, /map/tiles/{...})
			// as they have different parameter sets
			if (path.contains("/tiles")) {
				continue;
			}
			Set<String> definedParams = entry.getValue();

			// Determine if this is a collection-level path
			boolean isCollectionLevel = path.contains("/collections/");

			for (String requiredParam : requiredParams) {
				// 'collections' parameter only applies to dataset-level map paths
				if ("collections".equals(requiredParam) && isCollectionLevel) {
					continue;
				}
				if (!definedParams.contains(requiredParam)) {
					errors.add("[Part C] Path '" + path + "' is missing required parameter '" + requiredParam
							+ "' (required by declared conformance class)");
				}
			}
		}

		if (!errors.isEmpty()) {
			StringBuilder message = new StringBuilder();
			message.append("API Operations completeness verification failed with ")
				.append(errors.size())
				.append(" error(s):\n");
			for (int i = 0; i < errors.size(); i++) {
				message.append("  ").append(i + 1).append(". ").append(errors.get(i)).append("\n");
			}
			throw new AssertionError(message.toString());
		}
	}

	// ========================================================================
	// Setup helper methods
	// ========================================================================

	/**
	 * Fetches the conformance classes declared by the server.
	 * @return A set of conformance class URIs.
	 */
	private Set<String> fetchConformanceClasses() {
		Set<String> classes = new HashSet<>();
		String baseUrl = getBaseUrl();
		try {
			Response response = init().accept("application/json").when().get(baseUrl + "/conformance");
			if (response.getStatusCode() == 200) {
				List<String> conformsTo = response.jsonPath().getList("conformsTo");
				if (conformsTo != null) {
					classes.addAll(conformsTo);
				}
			}
		}
		catch (Exception e) {
			// Failed to fetch conformance
		}
		return classes;
	}

	/**
	 * Finds the API definition URL (rel="service-desc") from the landing page.
	 * @return The API definition URL, or null if not found.
	 */
	private String findServiceDescUrl() {
		String baseUrl = getBaseUrl();
		try {
			Response response = init().accept("application/json").when().get(baseUrl);
			if (response.getStatusCode() == 200) {
				List<Map<String, Object>> links = response.jsonPath().getList("links");
				if (links != null) {
					for (Map<String, Object> link : links) {
						String rel = (String) link.get("rel");
						if (REL_SERVICE_DESC.equals(rel)) {
							String href = (String) link.get("href");
							return resolveUrl(baseUrl, href);
						}
					}
				}
			}
		}
		catch (Exception e) {
			// Failed to fetch landing page
		}
		return null;
	}

	/**
	 * Parses the OpenAPI definition to extract paths and their parameters.
	 * @param apiDefinitionUrl The URL of the API definition.
	 */
	@SuppressWarnings("unchecked")
	private void parseApiDefinition(String apiDefinitionUrl) {
		this.apiDefinitionPaths = new HashSet<>();
		this.apiDefinitionParameters = new HashMap<>();

		try {
			Response response = init().accept("application/json").when().get(apiDefinitionUrl);
			if (response.getStatusCode() != 200) {
				throw new SkipException("Failed to retrieve API definition from " + apiDefinitionUrl + " (HTTP "
						+ response.getStatusCode() + ")");
			}

			Map<String, Object> paths = response.jsonPath().getMap("paths");
			if (paths == null) {
				throw new SkipException("API definition has no 'paths' object");
			}

			// Pre-load components/parameters for resolving $ref
			Map<String, Object> componentParameters = response.jsonPath().getMap("components.parameters");

			for (Map.Entry<String, Object> pathEntry : paths.entrySet()) {
				String path = pathEntry.getKey();
				apiDefinitionPaths.add(path);

				// Extract parameters for GET operation
				Set<String> params = new HashSet<>();
				try {
					Map<String, Object> pathItem = (Map<String, Object>) pathEntry.getValue();
					Map<String, Object> getOp = (Map<String, Object>) pathItem.get("get");
					if (getOp != null) {
						List<Map<String, Object>> parameters = (List<Map<String, Object>>) getOp.get("parameters");
						if (parameters != null) {
							for (Map<String, Object> param : parameters) {
								String name = (String) param.get("name");
								if (name != null) {
									params.add(name);
								}
								// Handle $ref parameters — resolve to actual component
								// to get the real parameter name
								String ref = (String) param.get("$ref");
								if (ref != null) {
									String resolvedName = resolveRefParamName(ref, componentParameters);
									if (resolvedName != null) {
										params.add(resolvedName);
									}
								}
							}
						}
					}
				}
				catch (Exception e) {
					// Failed to parse parameters for this path
				}
				apiDefinitionParameters.put(path, params);
			}
		}
		catch (SkipException e) {
			throw e;
		}
		catch (Exception e) {
			throw new SkipException("Failed to parse API definition: " + e.getMessage());
		}
	}

	/**
	 * Discovers actual map and tileset resource links from the landing page and
	 * collections.
	 */
	@SuppressWarnings("unchecked")
	private void discoverActualResources() {
		this.actualMapPaths = new HashSet<>();
		this.actualTilesetPaths = new HashSet<>();
		String baseUrl = getBaseUrl();

		// 1. Check landing page links
		try {
			Response response = init().accept("application/json").when().get(baseUrl);
			if (response.getStatusCode() == 200) {
				List<Map<String, Object>> links = response.jsonPath().getList("links");
				collectResourceLinks(links, baseUrl);
			}
		}
		catch (Exception e) {
			// Failed to check landing page
		}

		// 2. Check collections
		try {
			Response response = init().accept("application/json").when().get(baseUrl + "/collections");
			if (response.getStatusCode() == 200) {
				List<Map<String, Object>> collections = response.jsonPath().getList("collections");
				if (collections != null) {
					for (Map<String, Object> collection : collections) {
						List<Map<String, Object>> links = (List<Map<String, Object>>) collection.get("links");
						collectResourceLinks(links, baseUrl);
					}
				}
			}
		}
		catch (Exception e) {
			// Failed to check collections
		}
	}

	// ========================================================================
	// Utility methods
	// ========================================================================

	/**
	 * Collects map and tileset resource links from a list of link objects.
	 * @param links The list of link objects.
	 * @param baseUrl The base URL for resolving relative URLs.
	 */
	private void collectResourceLinks(List<Map<String, Object>> links, String baseUrl) {
		if (links == null) {
			return;
		}
		for (Map<String, Object> link : links) {
			String rel = (String) link.get("rel");
			String href = (String) link.get("href");
			if (rel == null || href == null) {
				continue;
			}
			String resolvedUrl = resolveUrl(baseUrl, href);
			String path = extractPath(resolvedUrl);
			if (path == null) {
				continue;
			}
			// Strip base path prefix (e.g., "/ogcapi") and format suffixes
			path = stripBasePath(cleanPath(path));
			if (matchesRelIgnoringScheme(rel, REL_MAP)) {
				actualMapPaths.add(path);
			}
			else if (matchesRelIgnoringScheme(rel, REL_TILESETS_MAP)) {
				actualTilesetPaths.add(path);
			}
		}
	}

	/**
	 * Checks if an actual resource path matches any API definition path pattern. Converts
	 * template paths (e.g., /collections/{collectionId}/map) to regex patterns and
	 * matches against the actual path.
	 * @param actualPath The actual resource path.
	 * @param apiPaths The set of API definition paths (may contain templates).
	 * @return true if a matching path pattern exists.
	 */
	private boolean hasMatchingApiPath(String actualPath, Set<String> apiPaths) {
		// Strip query parameters, format suffixes and base path for comparison
		String cleanPath = stripBasePath(cleanPath(actualPath));

		for (String apiPath : apiPaths) {
			// Convert template path to regex: {param} -> [^/]+
			String pattern = apiPath.replaceAll("\\{[^}]+\\}", "[^/]+");
			if (cleanPath.matches(pattern)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Strips the base path prefix from a path. For example, if the base path is "/ogcapi"
	 * and the path is "/ogcapi/collections/foo/map", the result is
	 * "/collections/foo/map".
	 * @param path The path to strip.
	 * @return The path without the base path prefix.
	 */
	private String stripBasePath(String path) {
		if (basePath != null && !basePath.isEmpty() && path.startsWith(basePath)) {
			return path.substring(basePath.length());
		}
		return path;
	}

	/**
	 * Cleans a path by removing query parameters and common format suffixes.
	 * @param path The path to clean.
	 * @return The cleaned path.
	 */
	private String cleanPath(String path) {
		// Remove query parameters
		int queryIdx = path.indexOf('?');
		if (queryIdx >= 0) {
			path = path.substring(0, queryIdx);
		}
		// Remove common format suffixes (.json, .png, .jpg, .tif, etc.)
		path = path.replaceAll("\\.(json|png|jpg|jpeg|tif|tiff|html|xml)$", "");
		return path;
	}

	/**
	 * Resolves a $ref parameter to its actual "name" field by looking up the component
	 * definition. For example, $ref "#/components/parameters/collections-maps" might
	 * resolve to a component whose "name" field is "collections".
	 * @param ref The $ref string (e.g., "#/components/parameters/collections-maps").
	 * @param componentParameters The pre-loaded components/parameters map from the
	 * OpenAPI definition.
	 * @return The actual parameter name, or the ref key as fallback.
	 */
	@SuppressWarnings("unchecked")
	private String resolveRefParamName(String ref, Map<String, Object> componentParameters) {
		if (ref == null || !ref.contains("/")) {
			return null;
		}
		String refKey = ref.substring(ref.lastIndexOf('/') + 1);

		// Try to resolve from components/parameters
		if (componentParameters != null && componentParameters.containsKey(refKey)) {
			Object component = componentParameters.get(refKey);
			if (component instanceof Map) {
				String name = (String) ((Map<String, Object>) component).get("name");
				if (name != null) {
					return name;
				}
			}
		}

		// Fallback to ref key if component not found
		return refKey;
	}

	/**
	 * Extracts the path component from a URL.
	 * @param url The URL string.
	 * @return The path component, or null if extraction fails.
	 */
	private String extractPath(String url) {
		if (url == null) {
			return null;
		}
		try {
			java.net.URI uri = java.net.URI.create(url);
			return uri.getPath();
		}
		catch (Exception e) {
			return null;
		}
	}

	private String getBaseUrl() {
		String baseUrl = rootUri.toString();
		if (baseUrl.endsWith("/")) {
			baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
		}
		return baseUrl;
	}

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

	/**
	 * Checks if a set contains a value, ignoring HTTP/HTTPS scheme differences.
	 * @param set The set to check.
	 * @param value The value to look for.
	 * @return true if the set contains the value (with scheme normalization).
	 */
	private static boolean containsIgnoringScheme(Set<String> set, String value) {
		String normalized = normalizeScheme(value);
		for (String item : set) {
			if (normalizeScheme(item).equals(normalized)) {
				return true;
			}
		}
		return false;
	}

	private static String normalizeScheme(String rel) {
		if (rel.startsWith("https://")) {
			return "http://" + rel.substring("https://".length());
		}
		return rel;
	}

}
