package org.opengis.cite.ogcapimaps10.conformance.apioperations;

import java.net.URI;
import java.util.ArrayList;
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
 * A.22.2. Abstract Test for Requirement API Operation identifiers.
 *
 * <pre>
 * Abstract test A.62
 *
 * Identifier:    /conf/api-operations/operation-id
 * Requirement:   /req/api-operations/operation-id
 * Test purpose:  Verify that the implementation uses the correct API operation
 *                identifier suffixes to identify the resources defined in the
 *                Maps API Standard
 *
 * Test method:
 * Given: an API implementation conforming to OGC API - Common - Part 1: Core
 *        "Landing Page" conformance class supporting an API definition
 *        language with a concept of operation identifiers
 * When:  retrieving the API description
 * Then:
 * - assert that the paths defined in the API definition have an operation
 *   identifier value ending with the relevant dot-separated suffix
 *   corresponding to the resource as specified in Table 11
 * </pre>
 */
public class ApiOperationIdentifiers extends CommonFixture {

	private static final String CONF_API_OPERATIONS = "http://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/api-operations";

	private static final String REL_SERVICE_DESC = "service-desc";

	private String basePath;

	private Set<String> conformanceClasses;

	private List<PathOperation> apiOperations;

	/**
	 * Checks preconditions: the server must declare support for the api-operations
	 * conformance class, and a valid API definition must be retrievable.
	 * @param testContext The test context containing suite attributes.
	 */
	@BeforeClass
	public void setup(ITestContext testContext) {
		this.basePath = rootUri.getPath();
		if (this.basePath != null && this.basePath.endsWith("/")) {
			this.basePath = this.basePath.substring(0, this.basePath.length() - 1);
		}

		this.conformanceClasses = fetchConformanceClasses();
		if (!containsIgnoringScheme(conformanceClasses, CONF_API_OPERATIONS)) {
			throw new SkipException(
					"Server does not declare conformance class " + CONF_API_OPERATIONS + ". Skipping A.62 tests.");
		}

		String apiDefinitionUrl = findServiceDescUrl();
		if (apiDefinitionUrl == null) {
			throw new SkipException("No API definition URL found (rel='service-desc') in landing page.");
		}

		parseApiDefinition(apiDefinitionUrl);
	}

	/**
	 * <pre>
	 * Abstract test A.62
	 *
	 * Identifier: /conf/api-operations/operation-id
	 * Requirement: /req/api-operations/operation-id
	 * Test purpose: Verify that the implementation uses the correct API operation
	 *               identifier suffixes to identify the resources defined in the
	 *               Maps API Standard
	 * </pre>
	 */
	@Test(description = "Implements A.22.2. Abstract Test for Requirement API Operation identifiers "
			+ "(Requirement /req/api-operations/operation-id)")
	public void verifyApiOperationIdentifiers() {
		List<String> errors = new ArrayList<>();

		for (PathOperation pathOperation : apiOperations) {
			String expectedSuffix = expectedSuffixForPath(pathOperation.path, basePath);
			if (expectedSuffix == null) {
				continue;
			}
			if (pathOperation.operationId == null || pathOperation.operationId.trim().isEmpty()) {
				errors.add("Path '" + pathOperation.path + "' is missing a GET operationId. Expected suffix '"
						+ expectedSuffix + "'.");
				continue;
			}
			if (!pathOperation.operationId.endsWith(expectedSuffix)) {
				errors.add("Path '" + pathOperation.path + "' has operationId '" + pathOperation.operationId
						+ "' which does not end with expected suffix '" + expectedSuffix + "'.");
			}
		}

		if (!errors.isEmpty()) {
			StringBuilder message = new StringBuilder();
			message.append("API Operation identifier verification failed with ")
				.append(errors.size())
				.append(" error(s):\n");
			for (int i = 0; i < errors.size(); i++) {
				message.append("  ").append(i + 1).append(". ").append(errors.get(i)).append("\n");
			}
			throw new AssertionError(message.toString());
		}
	}

	@SuppressWarnings("unchecked")
	private void parseApiDefinition(String apiDefinitionUrl) {
		this.apiOperations = new ArrayList<>();

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

			for (Map.Entry<String, Object> pathEntry : paths.entrySet()) {
				String path = pathEntry.getKey();
				String operationId = null;
				if (pathEntry.getValue() instanceof Map) {
					Map<String, Object> pathItem = (Map<String, Object>) pathEntry.getValue();
					Object getOperationObject = pathItem.get("get");
					if (getOperationObject instanceof Map) {
						Map<String, Object> getOperation = (Map<String, Object>) getOperationObject;
						operationId = (String) getOperation.get("operationId");
					}
				}
				apiOperations.add(new PathOperation(path, operationId));
			}
		}
		catch (SkipException e) {
			throw e;
		}
		catch (Exception e) {
			throw new SkipException("Failed to parse API definition: " + e.getMessage());
		}
	}

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

	static String expectedSuffixForPath(String path, String basePath) {
		String normalizedPath = normalizePath(path, basePath);
		if (normalizedPath == null) {
			return null;
		}

		List<String> segments = splitSegments(normalizedPath);
		if (segments.isEmpty()) {
			return null;
		}

		if (segments.size() == 1 && "projections".equals(segments.get(0))) {
			return "getCustomCRSProjections";
		}

		boolean collectionOrigin = segments.size() >= 2 && "collections".equals(segments.get(0));
		String origin = collectionOrigin ? "collection" : "dataset";
		int index = collectionOrigin ? 2 : 0;

		if (collectionOrigin && segments.size() <= 2) {
			return null;
		}

		boolean styled = false;
		if (segments.size() > index + 1 && "styles".equals(segments.get(index))) {
			styled = true;
			index += 2;
		}

		if (segments.size() <= index || !"map".equals(segments.get(index))) {
			return null;
		}

		if (segments.size() == index + 1) {
			return styled ? "." + origin + ".style.getMap" : "." + origin + ".getMap";
		}
		if (segments.size() == index + 2 && "tiles".equals(segments.get(index + 1))) {
			return styled ? "." + origin + ".style.map.getTileSetsList" : "." + origin + ".map.getTileSetsList";
		}
		if (segments.size() == index + 3 && "tiles".equals(segments.get(index + 1))) {
			return styled ? "." + origin + ".style.map.getTileSet" : "." + origin + ".map.getTileSet";
		}
		if (segments.size() > index + 3 && "tiles".equals(segments.get(index + 1))) {
			return styled ? "." + origin + ".style.map.getTile" : "." + origin + ".map.getTile";
		}
		return null;
	}

	static String normalizePath(String path, String basePath) {
		if (path == null) {
			return null;
		}
		String normalizedPath = path.trim();
		if (normalizedPath.isEmpty()) {
			return null;
		}
		int queryIdx = normalizedPath.indexOf('?');
		if (queryIdx >= 0) {
			normalizedPath = normalizedPath.substring(0, queryIdx);
		}
		normalizedPath = normalizedPath.replaceAll("\\.(json|png|jpg|jpeg|tif|tiff|html|xml)$", "");
		try {
			if (normalizedPath.startsWith("http://") || normalizedPath.startsWith("https://")) {
				normalizedPath = URI.create(normalizedPath).getPath();
			}
		}
		catch (Exception e) {
			return null;
		}
		if (normalizedPath == null || normalizedPath.isEmpty()) {
			return null;
		}
		if (basePath != null && !basePath.isEmpty() && normalizedPath.startsWith(basePath)) {
			normalizedPath = normalizedPath.substring(basePath.length());
		}
		if (normalizedPath.startsWith("/")) {
			normalizedPath = normalizedPath.substring(1);
		}
		if (normalizedPath.endsWith("/")) {
			normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
		}
		return normalizedPath.isEmpty() ? null : normalizedPath;
	}

	private static List<String> splitSegments(String normalizedPath) {
		List<String> segments = new ArrayList<>();
		if (normalizedPath == null || normalizedPath.isEmpty()) {
			return segments;
		}
		for (String segment : normalizedPath.split("/")) {
			if (!segment.isEmpty()) {
				segments.add(segment);
			}
		}
		return segments;
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

	private static final class PathOperation {

		private final String path;

		private final String operationId;

		private PathOperation(String path, String operationId) {
			this.path = path;
			this.operationId = operationId;
		}

	}

}
