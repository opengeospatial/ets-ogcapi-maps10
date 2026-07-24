package org.opengis.cite.ogcapimaps10.conformance.collectionSelection;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.opengis.cite.ogcapimaps10.conformance.CommonFixture;
import org.testng.SkipException;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Implements Abstract Test A.11: /conf/collections-selection/collections-parameter
 *
 * <p>
 * Test Purpose: Verify that the dataset map endpoint supports the optional
 * {@code collections} query parameter (Requirement 11:
 * /req/collections-selection/collections-parameter).
 *
 * <p>
 * Assertions verified:
 * <ul>
 * <li>[Req11/param-defined] The OpenAPI document declares {@code collections} as an
 * optional query parameter for the map operation.</li>
 * <li>[Req11/single-id] A single collection ID (including namespaced IDs such as
 * {@code NaturalEarth:cultural:ne_10m_admin_0_countries}) is accepted with HTTP 200.</li>
 * <li>[Req11/multi-id] Multiple comma-separated collection IDs are accepted with HTTP
 * 200.</li>
 * </ul>
 *
 * <p>
 * URL construction note: collection IDs are appended to the query string as raw strings.
 * Colons ({@code :}) used as namespace separators and commas ({@code ,}) used as ID
 * separators are valid query-string characters (RFC 3986 sub-delimiters) and must NOT be
 * percent-encoded.
 */
public class CollectionsParameterTest extends CommonFixture {

	private static final String REL_MAP = "http://www.opengis.net/def/rel/ogc/1.0/map";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	/**
	 * A.11 Abstract Test for Requirement
	 * /req/collections-selection/collections-parameter.
	 *
	 * <p>
	 * Verifies that the dataset map endpoint accepts the {@code collections} query
	 * parameter with one or more collection identifiers (short IDs or full local URLs).
	 * Collection IDs may contain namespace separators ({@code :}), which must be
	 * preserved unencoded in the request URL.
	 */
	@Test(description = "A.11 Abstract Test for Requirement /req/collections-selection/collections-parameter: "
			+ "Verify that the dataset map endpoint supports the optional collections query parameter.")
	public void verifyCollectionsParameter() {
		List<String> errors = new ArrayList<>();
		String landingPageUrl = rootUri.toString();

		// Discover the dataset map URL from the landing page
		String mapUrl = findDatasetMapUrl(landingPageUrl);
		if (mapUrl == null) {
			throw new SkipException("Could not find a dataset map endpoint (rel=ogc/1.0/map) from the landing page at "
					+ landingPageUrl + ". Skipping A.11 test.");
		}

		// Discover collection IDs — full namespaced IDs preserved as-is (colons not
		// stripped)
		List<String> collectionIds = discoverCollectionIds(landingPageUrl);
		if (collectionIds.isEmpty()) {
			throw new SkipException("No collections found at " + landingPageUrl + "/collections. Skipping A.11 test.");
		}

		// --- Assertion A: OAS declares 'collections' query parameter
		// [Req11/param-defined] ---
		if (!checkOasCollectionsParam(landingPageUrl)) {
			errors.add("[Req11/param-defined] The OpenAPI document does not declare 'collections'"
					+ " as an optional query parameter for the map operation path.");
		}

		String id1 = collectionIds.get(0);

		// --- Assertion B: Single collection ID accepted [Req11/single-id] ---
		// Raw URL: colons in namespaced IDs are valid query characters and must not be
		// encoded.
		// Example:
		// ?collections=NaturalEarth:cultural:ne_10m_admin_0_countries&f=png&width=800&height=400
		String singleUrl = mapUrl + "?collections=" + id1 + "&f=png&width=800&height=400";
		int statusSingle = getStatusRaw(singleUrl);
		if (statusSingle != 200) {
			errors.add("[Req11/single-id] Expected HTTP 200 for collections='" + id1 + "' but got HTTP " + statusSingle
					+ ".");
		}

		// --- Assertion C: Multiple comma-separated IDs accepted [Req11/multi-id] ---
		// The comma separator between IDs is also a valid query character and must not be
		// encoded.
		// Example:
		// ?collections=gebco,NaturalEarth:cultural:ne_10m_admin_0_countries&f=png&...
		if (collectionIds.size() >= 2) {
			String id2 = collectionIds.get(1);
			String multiUrl = mapUrl + "?collections=" + id1 + "," + id2 + "&f=png&width=800&height=400";
			int statusMulti = getStatusRaw(multiUrl);
			if (statusMulti != 200) {
				errors.add("[Req11/multi-id] Expected HTTP 200 for collections='" + id1 + "," + id2 + "' but got HTTP "
						+ statusMulti + ".");
			}
		}

		// --- Assertion D: Full local URL form accepted [Req11/full-url-id] ---
		// The spec allows collection IDs to be expressed as full local resource URLs,
		// e.g.
		// https://server/ogcapi/collections/NaturalEarth:cultural:ne_10m_admin_0_countries
		// Colons in the path segment and in the query value are both valid and unencoded.
		String base = landingPageUrl.endsWith("/") ? landingPageUrl.substring(0, landingPageUrl.length() - 1)
				: landingPageUrl;
		String fullUrlId1 = base + "/collections/" + id1;
		String fullUrlSingleUrl = mapUrl + "?collections=" + fullUrlId1 + "&f=png&width=800&height=400";
		int statusFullUrl = getStatusRaw(fullUrlSingleUrl);
		if (statusFullUrl != 200) {
			errors.add("[Req11/full-url-id] Expected HTTP 200 for full URL form collections='" + fullUrlId1
					+ "' but got HTTP " + statusFullUrl + ".");
		}

		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.11 verifyCollectionsParameter failed:\n" + String.join("\n", errors));
		}
	}

	// -------------------------------------------------------------------------
	// Private helpers
	// -------------------------------------------------------------------------

	/**
	 * Finds the dataset map URL from the landing page by following the link with
	 * rel=ogc/1.0/map. Falls back to {@code {landingPageUrl}/map} if no such link is
	 * found.
	 * @param landingPageUrl the IUT landing page URL
	 * @return the dataset map URL, never null (falls back to base + "/map")
	 */
	@SuppressWarnings("unchecked")
	private String findDatasetMapUrl(String landingPageUrl) {
		try {
			Map<String, Object> landingPage = fetchJson(landingPageUrl + "?f=json");
			if (landingPage != null) {
				List<Map<String, Object>> links = (List<Map<String, Object>>) landingPage.get("links");
				if (links != null) {
					for (Map<String, Object> link : links) {
						String rel = (String) link.get("rel");
						if (rel != null && matchesRelIgnoringScheme(rel, REL_MAP)) {
							String href = (String) link.get("href");
							if (href != null) {
								return resolveUrl(landingPageUrl, href);
							}
						}
					}
				}
			}
		}
		catch (Exception e) {
			// fall through to default
		}
		String base = landingPageUrl.endsWith("/") ? landingPageUrl.substring(0, landingPageUrl.length() - 1)
				: landingPageUrl;
		return base + "/map";
	}

	/**
	 * Retrieves available collection IDs from the {@code /collections} endpoint. Full
	 * namespaced IDs (e.g. {@code NaturalEarth:cultural:ne_10m_admin_0_countries}) are
	 * returned as-is without any encoding or modification.
	 * @param landingPageUrl the IUT landing page URL
	 * @return list of collection IDs; empty if none found or endpoint unreachable
	 */
	@SuppressWarnings("unchecked")
	private List<String> discoverCollectionIds(String landingPageUrl) {
		List<String> ids = new ArrayList<>();
		try {
			String base = landingPageUrl.endsWith("/") ? landingPageUrl.substring(0, landingPageUrl.length() - 1)
					: landingPageUrl;
			Map<String, Object> response = fetchJson(base + "/collections?f=json");
			if (response != null) {
				List<Map<String, Object>> collections = (List<Map<String, Object>>) response.get("collections");
				if (collections != null) {
					for (Map<String, Object> collection : collections) {
						String id = (String) collection.get("id");
						if (id != null && !id.isEmpty()) {
							ids.add(id);
						}
					}
				}
			}
		}
		catch (Exception e) {
			// return whatever was collected
		}
		return ids;
	}

	/**
	 * Checks that the OpenAPI document declares {@code collections} as an optional query
	 * parameter on at least one GET operation (map operation paths).
	 *
	 * <p>
	 * Parameters may be defined inline or via {@code $ref} (e.g. {@code {"$ref":
	 * "#/components/parameters/collections"}}). Both forms are resolved before checking
	 * the parameter name and location.
	 * @param landingPageUrl the IUT landing page URL
	 * @return {@code true} if the parameter is declared, {@code false} otherwise
	 */
	@SuppressWarnings("unchecked")
	private boolean checkOasCollectionsParam(String landingPageUrl) {
		try {
			String base = landingPageUrl.endsWith("/") ? landingPageUrl.substring(0, landingPageUrl.length() - 1)
					: landingPageUrl;
			Map<String, Object> api = fetchJson(base + "/api?f=json");
			if (api == null) {
				return false;
			}
			// Load reusable parameter definitions from components/parameters
			Map<String, Object> componentParams = new java.util.LinkedHashMap<>();
			Map<String, Object> components = (Map<String, Object>) api.get("components");
			if (components != null) {
				Map<String, Object> cp = (Map<String, Object>) components.get("parameters");
				if (cp != null) {
					componentParams.putAll(cp);
				}
			}
			Map<String, Object> paths = (Map<String, Object>) api.get("paths");
			if (paths == null) {
				return false;
			}
			for (Map.Entry<String, Object> pathEntry : paths.entrySet()) {
				Map<String, Object> pathItem = (Map<String, Object>) pathEntry.getValue();
				Map<String, Object> getOp = (Map<String, Object>) pathItem.get("get");
				if (getOp == null) {
					continue;
				}
				List<Map<String, Object>> parameters = (List<Map<String, Object>>) getOp.get("parameters");
				if (parameters == null) {
					continue;
				}
				for (Map<String, Object> param : parameters) {
					// Resolve $ref before checking — e.g. {"$ref":
					// "#/components/parameters/collections"}
					Map<String, Object> resolved = resolveParamRef(param, componentParams);
					if ("collections".equals(resolved.get("name")) && "query".equals(resolved.get("in"))) {
						// Parameter must be optional: required field absent or explicitly
						// false
						Object required = resolved.get("required");
						if (required == null || Boolean.FALSE.equals(required)) {
							return true;
						}
					}
				}
			}
		}
		catch (Exception e) {
			// fall through
		}
		return false;
	}

	/**
	 * Resolves a parameter object that may be an inline definition or a {@code $ref}
	 * pointing to {@code #/components/parameters/{name}}.
	 * @param param the raw parameter map from the OAS paths section
	 * @param componentParams the map of named parameters from
	 * {@code components/parameters}
	 * @return the resolved parameter map, or the original if no {@code $ref} is present
	 * or resolution fails
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Object> resolveParamRef(Map<String, Object> param, Map<String, Object> componentParams) {
		String ref = (String) param.get("$ref");
		if (ref == null) {
			return param;
		}
		// "#/components/parameters/collections" → key "collections"
		String prefix = "#/components/parameters/";
		if (ref.startsWith(prefix)) {
			String key = ref.substring(prefix.length());
			Map<String, Object> resolved = (Map<String, Object>) componentParams.get(key);
			if (resolved != null) {
				return resolved;
			}
		}
		return param;
	}

	/**
	 * Makes an HTTP GET request using the raw URL string without applying any additional
	 * percent-encoding. This preserves colons ({@code :}) in namespaced collection IDs
	 * and commas ({@code ,}) between collection IDs as required by the spec.
	 * @param rawUrl the fully constructed request URL
	 * @return the HTTP response status code, or {@code -1} on connection error
	 */
	private int getStatusRaw(String rawUrl) {
		try {
			URL url = new URL(rawUrl);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(10000);
			conn.setReadTimeout(10000);
			applyAuth(conn);
			return conn.getResponseCode();
		}
		catch (Exception e) {
			return -1;
		}
	}

	private Map<String, Object> fetchJson(String urlString) {
		try {
			URL url = new URL(urlString);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");
			conn.setConnectTimeout(10000);
			conn.setReadTimeout(10000);
			applyAuth(conn);
			if (conn.getResponseCode() == 200) {
				try (InputStream is = conn.getInputStream()) {
					return OBJECT_MAPPER.readValue(is, new TypeReference<Map<String, Object>>() {
					});
				}
			}
		}
		catch (Exception e) {
			// return null
		}
		return null;
	}

	private String resolveUrl(String baseUrl, String url) {
		if (url.startsWith("http://") || url.startsWith("https://")) {
			return url;
		}
		try {
			return java.net.URI.create(baseUrl).resolve(url).toString();
		}
		catch (Exception e) {
			return url;
		}
	}

	private boolean matchesRelIgnoringScheme(String actual, String expected) {
		return normalizeScheme(actual).equals(normalizeScheme(expected));
	}

	private String normalizeScheme(String rel) {
		if (rel.startsWith("https://")) {
			return "http://" + rel.substring("https://".length());
		}
		return rel;
	}

}
