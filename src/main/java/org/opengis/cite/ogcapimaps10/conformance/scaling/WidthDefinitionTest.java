package org.opengis.cite.ogcapimaps10.conformance.scaling;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.opengis.cite.ogcapimaps10.conformance.CommonFixture;
import org.opengis.cite.ogcapimaps10.conformance.SuiteAttribute;
import org.opengis.cite.ogcapimaps10.domain.ScalingWidthInteractiveTestResult;
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * Implements Abstract Test A.13: /conf/scaling/width-definition
 *
 * <p>
 * Test Purpose: Verify that the implementation supports the (scaling) width parameter
 * correctly for map requests (Requirement 13: /req/scaling/width-definition).
 *
 * <p>
 * Assertions verified:
 * <ul>
 * <li>[Req13/oas-param] The width query parameter (type: number, required: false) is
 * defined in the OpenAPI document.</li>
 * <li>[Req13/width-pixel-columns] The server returns HTTP 200 for a valid width and, for
 * PNG responses, the returned image width matches the requested pixel count.</li>
 * <li>[Req13/invalid-width] The server returns HTTP 4xx for non-positive or non-integer
 * width values (0, -1, non-numeric).</li>
 * <li>[Req13/max-width] If maxWidth is declared in service metadata, the server returns
 * HTTP 4xx when width exceeds it.</li>
 * <li>[Req13/max-pixels] If maxPixels is declared in service metadata, the server returns
 * HTTP 4xx when width times height exceeds it.</li>
 * <li>[Req13/scale-denom-bbox-conflict] If Subsetting is supported, the server returns
 * HTTP 4xx when width, bbox, and scale-denominator are combined.</li>
 * <li>[Req13/width-scale-denom-no-subsetting] If Subsetting is not supported, the server
 * returns HTTP 4xx when width and scale-denominator are combined.</li>
 * <li>[Req13/default-width] When width is omitted, the server returns HTTP 200 and
 * (interactive) uses appropriate default dimensions reflecting the scale.</li>
 * </ul>
 */
public class WidthDefinitionTest extends CommonFixture {

	private static final String PARAM_WIDTH = "width";

	private static final String PARAM_HEIGHT = "height";

	private static final String PARAM_SCALE_DENOMINATOR = "scale-denominator";

	private static final String REL_MAP = "http://www.opengis.net/def/rel/ogc/1.0/map";

	private static final String CONF_SPATIAL_SUBSETTING = "https://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/spatial-subsetting";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	/**
	 * A.13 Abstract Test for Requirement /req/scaling/width-definition.
	 *
	 * <p>
	 * Verifies that the server supports the width parameter with correct OAS definition,
	 * proper pixel-column interpretation, rejection of invalid values, enforcement of
	 * service metadata limits, rejection of forbidden parameter combinations, and
	 * appropriate default behaviour when width is omitted.
	 * @param context the TestNG test context, used to access suite attributes
	 */
	@Test(description = "A.13 Abstract Test for Requirement /req/scaling/width-definition: "
			+ "Verify that the implementation supports the scaling width parameter correctly for map requests.")
	public void verifyWidthDefinition(ITestContext context) {
		List<String> errors = new ArrayList<>();

		String landingPageUrl = rootUri.toString();

		// Locate the dataset map endpoint
		String mapUrl = findMapUrl(landingPageUrl);
		if (mapUrl == null) {
			throw new SkipException("Could not find a dataset map endpoint (rel=ogc/1.0/map) from the landing page at "
					+ landingPageUrl + ". Skipping A.13 test.");
		}
		String sep = mapUrl.contains("?") ? "&" : "?";

		// --- Assertion A: OAS width parameter defined [Req13/oas-param] ---
		if (!oasHasWidthParam(landingPageUrl)) {
			errors.add("[Req13/oas-param] The width query parameter (schema.type: number, required: false) "
					+ "was not found in the OpenAPI document at " + landingPageUrl + "/api.");
		}

		// --- Assertion B: width = pixel columns [Req13/width-pixel-columns] ---
		// Request a map at width=200, height=100 and verify HTTP 200.
		// For PNG responses, also verify the image width matches the request.
		String urlB = mapUrl + sep + "width=200&height=100&f=png";
		int statusB = getStatusRaw(urlB);
		if (statusB != 200) {
			errors.add("[Req13/width-pixel-columns] Expected HTTP 200 for " + PARAM_WIDTH + "=200&" + PARAM_HEIGHT
					+ "=100 but got HTTP " + statusB + ".");
		}
		else {
			try {
				byte[] bytes = getResponseBytes(urlB);
				if (bytes != null) {
					BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
					if (img != null && img.getWidth() != 200) {
						errors.add("[Req13/width-pixel-columns] Requested " + PARAM_WIDTH
								+ "=200 but the PNG response has width=" + img.getWidth()
								+ "px. The width parameter must define the horizontal pixel count.");
					}
				}
			}
			catch (Exception e) {
				// Cannot decode image; skip dimension sub-check
			}
		}

		// --- Assertion C: invalid width values -> 4xx [Req13/invalid-width] ---
		int statusZero = getStatusRaw(mapUrl + sep + "width=0&f=png");
		if (!is4xx(statusZero)) {
			errors.add("[Req13/invalid-width] Expected HTTP 4xx for " + PARAM_WIDTH
					+ "=0 (not a positive integer) but got HTTP " + statusZero + ".");
		}
		int statusNeg = getStatusRaw(mapUrl + sep + "width=-1&f=png");
		if (!is4xx(statusNeg)) {
			errors.add("[Req13/invalid-width] Expected HTTP 4xx for " + PARAM_WIDTH + "=-1 (negative) but got HTTP "
					+ statusNeg + ".");
		}
		int statusAlpha = getStatusRaw(mapUrl + sep + "width=abc&f=png");
		if (!is4xx(statusAlpha)) {
			errors.add("[Req13/invalid-width] Expected HTTP 4xx for " + PARAM_WIDTH
					+ "=abc (not a number) but got HTTP " + statusAlpha + ".");
		}

		// --- Assertions D & E: service metadata limits ---
		Map<String, Object> ogcLimitsMaps = getXOgcLimitsMaps(landingPageUrl);

		// Assertion D: width > maxWidth -> 4xx [Req13/max-width]
		if (ogcLimitsMaps != null && ogcLimitsMaps.containsKey("maxWidth")) {
			int maxWidth = ((Number) ogcLimitsMaps.get("maxWidth")).intValue();
			int overWidth = maxWidth + 1;
			int statusD = getStatusRaw(mapUrl + sep + PARAM_WIDTH + "=" + overWidth + "&f=png");
			if (!is4xx(statusD)) {
				errors.add("[Req13/max-width] Expected HTTP 4xx (preferably 413) for " + PARAM_WIDTH + "=" + overWidth
						+ " (exceeds maxWidth=" + maxWidth + " from x-OGC-limits.maps) but got HTTP " + statusD + ".");
			}
		}

		// Assertion E: width*height > maxPixels -> 4xx [Req13/max-pixels]
		if (ogcLimitsMaps != null && ogcLimitsMaps.containsKey("maxPixels")) {
			long maxPixels = ((Number) ogcLimitsMaps.get("maxPixels")).longValue();
			int side = (int) Math.sqrt((double) maxPixels) + 1;
			int statusE = getStatusRaw(
					mapUrl + sep + PARAM_WIDTH + "=" + side + "&" + PARAM_HEIGHT + "=" + side + "&f=png");
			if (!is4xx(statusE)) {
				errors.add("[Req13/max-pixels] Expected HTTP 4xx (preferably 413) for " + PARAM_WIDTH + "=" + side + "&"
						+ PARAM_HEIGHT + "=" + side + " (product " + ((long) side * side) + " exceeds maxPixels="
						+ maxPixels + " from x-OGC-limits.maps) but got HTTP " + statusE + ".");
			}
		}

		// --- Assertions F & G: scale-denominator conflicts ---
		boolean hasSubsetting = hasConformanceClass(landingPageUrl, CONF_SPATIAL_SUBSETTING);

		// Assertion F: width+bbox+scale-denominator -> 4xx (only if Subsetting supported)
		// [Req13/scale-denom-bbox-conflict]
		if (hasSubsetting) {
			String rawUrlF = mapUrl + sep
					+ "width=800&height=400&bbox=-180,-90,180,90&scale-denominator=40000000&f=png";
			int statusF = getStatusRaw(rawUrlF);
			if (!is4xx(statusF)) {
				errors.add("[Req13/scale-denom-bbox-conflict] Expected HTTP 4xx when combining " + PARAM_WIDTH
						+ ", bbox, and " + PARAM_SCALE_DENOMINATOR + " but got HTTP " + statusF + ".");
			}
		}

		// Assertion G: width+scale-denominator -> 4xx (only if Subsetting NOT supported)
		// [Req13/width-scale-denom-no-subsetting]
		if (!hasSubsetting) {
			String rawUrlG = mapUrl + sep + "width=800&scale-denominator=40000000&f=png";
			int statusG = getStatusRaw(rawUrlG);
			if (!is4xx(statusG)) {
				errors.add("[Req13/width-scale-denom-no-subsetting] Expected HTTP 4xx when combining " + PARAM_WIDTH
						+ " and " + PARAM_SCALE_DENOMINATOR
						+ " (Spatial Subsetting conformance class not declared) but got HTTP " + statusG + ".");
			}
		}

		// --- Assertion H: omit width -> HTTP 200 + interactive [Req13/default-width] ---
		int statusH = getStatusRaw(mapUrl + sep + "f=png");
		if (statusH != 200) {
			errors.add("[Req13/default-width] Expected HTTP 200 when " + PARAM_WIDTH + " is omitted but got HTTP "
					+ statusH + ".");
		}

		ScalingWidthInteractiveTestResult interactiveResult = (ScalingWidthInteractiveTestResult) context.getSuite()
			.getAttribute(SuiteAttribute.SCALING_WIDTH_INTERACTIVE_TEST_RESULT.getName());
		if (interactiveResult != null && interactiveResult.isEnabled()) {
			if (!interactiveResult.isWidthDefaultAppropriate()) {
				errors.add("[Req13/default-width] Interactive verification failed: the map rendered without a "
						+ PARAM_WIDTH + " parameter (with " + PARAM_SCALE_DENOMINATOR
						+ "=40000000) was reported as NOT having appropriate default dimensions. "
						+ "The server does not appear to compute a default width that accurately "
						+ "reflects the requested scale.");
			}
		}

		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.13 verifyWidthDefinition failed:\n" + String.join("\n", errors));
		}
	}

	// -------------------------------------------------------------------------
	// Private helpers
	// -------------------------------------------------------------------------

	/**
	 * Finds the dataset map URL from the landing page by following the rel=ogc/1.0/map
	 * link, or falls back to {landingPageUrl}/map.
	 * @param landingPageUrl the IUT landing page URL
	 * @return the map URL, or {@code null} if the landing page cannot be reached
	 */
	@SuppressWarnings("unchecked")
	private String findMapUrl(String landingPageUrl) {
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
	 * Checks whether the OpenAPI document exposes a {@code width} query parameter with
	 * the characteristics required by Req 13/A: name=width, in=query, required=false,
	 * schema.type=number. Resolves {@code $ref} pointers to
	 * {@code components/parameters}.
	 * @param landingPageUrl the IUT landing page URL
	 * @return {@code true} if a conformant width parameter definition is found
	 */
	@SuppressWarnings("unchecked")
	private boolean oasHasWidthParam(String landingPageUrl) {
		try {
			Map<String, Object> oas = fetchJson(landingPageUrl + "/api?f=json");
			if (oas == null) {
				return false;
			}
			Map<String, Object> componentParams = new HashMap<>();
			Map<String, Object> components = (Map<String, Object>) oas.get("components");
			if (components != null) {
				Map<String, Object> cp = (Map<String, Object>) components.get("parameters");
				if (cp != null) {
					componentParams.putAll(cp);
				}
			}
			Map<String, Object> paths = (Map<String, Object>) oas.get("paths");
			if (paths == null) {
				return false;
			}
			for (Object pathItemObj : paths.values()) {
				Map<String, Object> pathItem = (Map<String, Object>) pathItemObj;
				for (String method : new String[] { "get", "post", "put", "delete", "patch", "head" }) {
					Map<String, Object> op = (Map<String, Object>) pathItem.get(method);
					if (op == null) {
						continue;
					}
					List<Map<String, Object>> params = (List<Map<String, Object>>) op.get("parameters");
					if (params == null) {
						continue;
					}
					for (Map<String, Object> param : params) {
						Map<String, Object> resolved = resolveParamRef(param, componentParams);
						if (!"width".equals(resolved.get("name")) || !"query".equals(resolved.get("in"))) {
							continue;
						}
						Object required = resolved.get("required");
						if (Boolean.TRUE.equals(required)) {
							continue;
						}
						Map<String, Object> schema = (Map<String, Object>) resolved.get("schema");
						if (schema != null && "number".equals(schema.get("type"))) {
							return true;
						}
					}
				}
			}
		}
		catch (Exception e) {
			// return false
		}
		return false;
	}

	/**
	 * Reads the {@code x-OGC-limits.maps} object from the OpenAPI document's {@code info}
	 * section, returning the map of limit properties ({@code maxWidth},
	 * {@code maxHeight}, {@code maxPixels}).
	 * @param landingPageUrl the IUT landing page URL
	 * @return the limits map, or {@code null} if absent or unreadable
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Object> getXOgcLimitsMaps(String landingPageUrl) {
		try {
			Map<String, Object> oas = fetchJson(landingPageUrl + "/api?f=json");
			if (oas == null) {
				return null;
			}
			Map<String, Object> info = (Map<String, Object>) oas.get("info");
			if (info == null) {
				return null;
			}
			Map<String, Object> xOgcLimits = (Map<String, Object>) info.get("x-OGC-limits");
			if (xOgcLimits == null) {
				return null;
			}
			return (Map<String, Object>) xOgcLimits.get("maps");
		}
		catch (Exception e) {
			return null;
		}
	}

	/**
	 * Checks whether the server declares the given conformance class URI in the
	 * {@code /conformance} resource.
	 * @param landingPageUrl the IUT landing page URL
	 * @param classUri the conformance class URI to look for
	 * @return {@code true} if the conformance class is declared
	 */
	@SuppressWarnings("unchecked")
	private boolean hasConformanceClass(String landingPageUrl, String classUri) {
		try {
			Map<String, Object> conf = fetchJson(landingPageUrl + "/conformance?f=json");
			if (conf == null) {
				return false;
			}
			List<String> classes = (List<String>) conf.get("conformsTo");
			return classes != null && classes.contains(classUri);
		}
		catch (Exception e) {
			return false;
		}
	}

	/**
	 * Resolves a parameter object that may be a {@code $ref} pointer to
	 * {@code #/components/parameters/{key}}.
	 * @param param the raw parameter map (may contain a {@code $ref} key)
	 * @param componentParams the parsed {@code components/parameters} map
	 * @return the resolved parameter map, or the original if no ref is present
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Object> resolveParamRef(Map<String, Object> param, Map<String, Object> componentParams) {
		String ref = (String) param.get("$ref");
		if (ref == null) {
			return param;
		}
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

	private Map<String, Object> fetchJson(String urlString) {
		try {
			URL url = URI.create(urlString).toURL();
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");
			conn.setConnectTimeout(10000);
			conn.setReadTimeout(10000);
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

	private int getStatusRaw(String urlString) {
		try {
			URL url = URI.create(urlString).toURL();
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(10000);
			conn.setReadTimeout(10000);
			return conn.getResponseCode();
		}
		catch (Exception e) {
			return -1;
		}
	}

	private byte[] getResponseBytes(String urlString) {
		try {
			URL url = URI.create(urlString).toURL();
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(10000);
			conn.setReadTimeout(30000);
			if (conn.getResponseCode() == 200) {
				try (InputStream is = conn.getInputStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
					byte[] buf = new byte[8192];
					int len;
					while ((len = is.read(buf)) != -1) {
						baos.write(buf, 0, len);
					}
					return baos.toByteArray();
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
			return URI.create(baseUrl).resolve(url).toString();
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

	private boolean is4xx(int status) {
		return status >= 400 && status <= 499;
	}

}
