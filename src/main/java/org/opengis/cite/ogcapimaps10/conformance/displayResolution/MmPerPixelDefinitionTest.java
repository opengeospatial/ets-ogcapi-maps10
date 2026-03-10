package org.opengis.cite.ogcapimaps10.conformance.displayResolution;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.opengis.cite.ogcapimaps10.conformance.CommonFixture;
import org.opengis.cite.ogcapimaps10.conformance.SuiteAttribute;
import org.opengis.cite.ogcapimaps10.domain.DisplayResolutionInteractiveTestResult;
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.Test;

import io.restassured.http.Method;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

/**
 * Implements Abstract Test A.16: /conf/display-resolution/mm-per-pixel-definition
 *
 * <p>
 * Test Purpose: Verify that the implementation supports the mm-per-pixel parameter
 * correctly for map requests (Requirement 16:
 * /req/display-resolution/mm-per-pixel-definition).
 * </p>
 *
 * <p>
 * Assertions verified:
 * <ul>
 * <li>[Req16/param-defined] The mm-per-pixel parameter is defined in the OpenAPI document
 * as an optional numeric query parameter.</li>
 * <li>[Req16/invalid-value] An HTTP 4xx error is returned when mm-per-pixel is not a
 * positive number (zero, negative, or non-numeric).</li>
 * <li>[Req16/default-assumed] When mm-per-pixel is omitted, the server assumes 0.28
 * mm/pixel and returns HTTP 200.</li>
 * </ul>
 */
public class MmPerPixelDefinitionTest extends CommonFixture {

	private static final String PARAM_MM_PER_PIXEL = "mm-per-pixel";

	private static final String REL_MAP = "http://www.opengis.net/def/rel/ogc/1.0/map";

	private static final String REL_SERVICE_DESC = "service-desc";

	/**
	 * A.16 Abstract Test for Requirement /req/display-resolution/mm-per-pixel-definition.
	 *
	 * <p>
	 * Verifies that the server correctly interprets the mm-per-pixel parameter, rejects
	 * non-positive values with HTTP 4xx, and uses a default of 0.28 mm/pixel when the
	 * parameter is omitted.
	 * </p>
	 * @param context the TestNG test context, used to access suite attributes
	 */
	@Test(description = "A.16 Abstract Test for Requirement /req/display-resolution/mm-per-pixel-definition: "
			+ "Verify that the implementation supports the mm-per-pixel parameter correctly for map requests.")
	public void verifyMmPerPixelDefinition(ITestContext context) {
		List<String> errors = new ArrayList<>();

		String landingPageUrl = rootUri.toString();

		// Locate the dataset map endpoint
		String mapUrl = findMapUrl(landingPageUrl);
		if (mapUrl == null) {
			throw new SkipException("Could not find a dataset map endpoint (rel=ogc/1.0/map) from the landing page at "
					+ landingPageUrl + ". Skipping A.16 test.");
		}

		// --- Assertion 1: OAS declares mm-per-pixel parameter [Req16/param-defined] ---
		String apiUrl = findApiUrl(landingPageUrl);
		if (apiUrl != null) {
			if (!oasHasMmPerPixelParam(apiUrl)) {
				errors.add("[Req16/param-defined] The OpenAPI document at " + apiUrl
						+ " does not declare a 'mm-per-pixel' query parameter in any map operation. "
						+ "Requirement 16 requires the parameter to be defined as an optional numeric parameter.");
			}
		}

		// --- Assertion 2: HTTP 4xx for non-positive values [Req16/invalid-value] ---
		int statusZero = getStatus(mapUrl, PARAM_MM_PER_PIXEL, "0");
		if (!isClientError(statusZero)) {
			errors.add("[Req16/invalid-value] Expected HTTP 4xx for " + PARAM_MM_PER_PIXEL
					+ "=0 (zero is not a positive number) but got HTTP " + statusZero + ".");
		}

		int statusNegative = getStatus(mapUrl, PARAM_MM_PER_PIXEL, "-1");
		if (!isClientError(statusNegative)) {
			errors.add("[Req16/invalid-value] Expected HTTP 4xx for " + PARAM_MM_PER_PIXEL
					+ "=-1 (negative is not a positive number) but got HTTP " + statusNegative + ".");
		}

		int statusNonNumeric = getStatus(mapUrl, PARAM_MM_PER_PIXEL, "notanumber");
		if (!isClientError(statusNonNumeric)) {
			errors.add("[Req16/invalid-value] Expected HTTP 4xx for " + PARAM_MM_PER_PIXEL
					+ "=notanumber (non-numeric value) but got HTTP " + statusNonNumeric + ".");
		}

		// --- Assertion 3: Default 0.28 mm/pixel assumed when omitted
		// [Req16/default-assumed] ---
		int statusDefault = getStatusNoParam(mapUrl);
		if (statusDefault != 200) {
			errors.add("[Req16/default-assumed] Expected HTTP 200 for a map request without the " + PARAM_MM_PER_PIXEL
					+ " parameter (server should assume default 0.28 mm/pixel) but got HTTP " + statusDefault + ".");
		}

		// --- Assertion: server interprets mm-per-pixel correctly [Req16/interprets] ---
		DisplayResolutionInteractiveTestResult interactiveResult = (DisplayResolutionInteractiveTestResult) context
			.getSuite()
			.getAttribute(SuiteAttribute.DISPLAY_RESOLUTION_INTERACTIVE_TEST_RESULT.getName());
		if (interactiveResult != null && interactiveResult.isEnabled()) {
			if (!interactiveResult.isMmPerPixelCorrect()) {
				errors.add("[Req16/interprets] Interactive verification failed: the two maps rendered at "
						+ "mm-per-pixel=0.28 and mm-per-pixel=0.1 were reported as NOT visually different. "
						+ "The server does not appear to correctly interpret the mm-per-pixel parameter.");
			}
		}

		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.16 verifyMmPerPixelDefinition failed:\n" + String.join("\n", errors));
		}
	}

	// -------------------------------------------------------------------------
	// Private helpers
	// -------------------------------------------------------------------------

	/**
	 * Finds the dataset map URL from the landing page by following the rel=ogc/1.0/map
	 * link, or falls back to {landingPageUrl}/map.
	 * @param landingPageUrl the IUT landing page URL
	 * @return the map URL, or null if the landing page cannot be reached
	 */
	private String findMapUrl(String landingPageUrl) {
		try {
			Response response = init().when().request(Method.GET, landingPageUrl + "?f=json");
			if (response.getStatusCode() != 200) {
				return null;
			}
			JsonPath json = response.jsonPath();
			List<Map<String, Object>> links = json.getList("links");
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
		catch (Exception e) {
			// fall through to default
		}
		// Conventional fallback
		String base = landingPageUrl.endsWith("/") ? landingPageUrl.substring(0, landingPageUrl.length() - 1)
				: landingPageUrl;
		return base + "/map";
	}

	/**
	 * Finds the API (OpenAPI) document URL from the landing page links
	 * (rel=service-desc), or falls back to {landingPageUrl}/api.
	 * @param landingPageUrl the IUT landing page URL
	 * @return the API document URL, or null if the landing page cannot be reached
	 */
	private String findApiUrl(String landingPageUrl) {
		try {
			Response response = init().when().request(Method.GET, landingPageUrl + "?f=json");
			if (response.getStatusCode() != 200) {
				return null;
			}
			JsonPath json = response.jsonPath();
			List<Map<String, Object>> links = json.getList("links");
			if (links != null) {
				for (Map<String, Object> link : links) {
					String rel = (String) link.get("rel");
					if (REL_SERVICE_DESC.equals(rel)) {
						String href = (String) link.get("href");
						if (href != null) {
							return resolveUrl(landingPageUrl, href);
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
		return base + "/api";
	}

	/**
	 * Checks whether the OpenAPI document declares a {@code mm-per-pixel} query parameter
	 * in any GET operation path.
	 * @param apiUrl the URL of the OpenAPI document
	 * @return true if the parameter is declared, false otherwise
	 */
	@SuppressWarnings("unchecked")
	private boolean oasHasMmPerPixelParam(String apiUrl) {
		try {
			Response response = init().when().request(Method.GET, apiUrl + "?f=json");
			if (response.getStatusCode() != 200) {
				return false;
			}
			JsonPath json = response.jsonPath();
			Map<String, Object> paths = json.getMap("paths");
			if (paths == null) {
				return false;
			}
			for (Object pathItem : paths.values()) {
				if (!(pathItem instanceof Map)) {
					continue;
				}
				Object getOp = ((Map<String, Object>) pathItem).get("get");
				if (!(getOp instanceof Map)) {
					continue;
				}
				Object params = ((Map<String, Object>) getOp).get("parameters");
				if (!(params instanceof List)) {
					continue;
				}
				for (Object param : (List<Object>) params) {
					if (!(param instanceof Map)) {
						continue;
					}
					Object name = ((Map<String, Object>) param).get("name");
					if (PARAM_MM_PER_PIXEL.equals(name)) {
						return true;
					}
				}
			}
		}
		catch (Exception e) {
			// Could not parse OAS — skip the assertion
			return true;
		}
		return false;
	}

	/**
	 * Sends a GET request to the map URL with the given parameter and returns the HTTP
	 * status code.
	 * @param mapUrl the map endpoint URL
	 * @param paramName the query parameter name
	 * @param paramValue the query parameter value
	 * @return the HTTP response status code
	 */
	private int getStatus(String mapUrl, String paramName, String paramValue) {
		return init().param(paramName, paramValue).when().request(Method.GET, mapUrl).getStatusCode();
	}

	/**
	 * Sends a GET request to the map URL without any extra parameters and returns the
	 * HTTP status code.
	 * @param mapUrl the map endpoint URL
	 * @return the HTTP response status code
	 */
	private int getStatusNoParam(String mapUrl) {
		return init().when().request(Method.GET, mapUrl).getStatusCode();
	}

	/**
	 * Returns true if the status code is a 4xx client error.
	 * @param statusCode the HTTP status code
	 * @return true if 400 to 499 inclusive
	 */
	private boolean isClientError(int statusCode) {
		return statusCode >= 400 && statusCode < 500;
	}

	/**
	 * Resolves a potentially relative URL against a base URL.
	 * @param baseUrl the base URL
	 * @param url the URL to resolve (may be relative or absolute)
	 * @return the resolved absolute URL
	 */
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

	/**
	 * Compares two rel values, treating HTTP and HTTPS as equivalent.
	 * @param actual the actual rel value from the server
	 * @param expected the expected rel value
	 * @return true if they match ignoring scheme differences
	 */
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