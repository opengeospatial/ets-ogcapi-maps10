package org.opengis.cite.ogcapimaps10.conformance.datetime;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.opengis.cite.ogcapimaps10.conformance.CommonFixture;
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Implements Abstract Test A.27: /conf/datetime/datetime-definition
 *
 * <p>
 * Test Purpose: Verify that the implementation supports the {@code datetime} parameter
 * (Requirement 27: /req/datetime/datetime-definition).
 *
 * <p>
 * Assertions verified:
 * <ul>
 * <li>[Req27/A] {@code datetime} parameter defined in OAS: {@code in=query},
 * {@code required=false}, {@code schema.type=string}, {@code style=form},
 * {@code explode=false}.</li>
 * <li>[Req27/B+C] {@code datetime=<instant>} using RFC 3339 Z-notation → HTTP 200 or
 * 204.</li>
 * <li>[Req27/B] {@code datetime=<start>/<end>} bounded interval → HTTP 200 or 204.</li>
 * <li>[Req27/D] {@code datetime=../<end>} open-start with double-dot → HTTP 200 or
 * 204.</li>
 * <li>[Req27/D] {@code datetime=/<end>} open-start with empty string → HTTP 200 or
 * 204.</li>
 * <li>[Req27/D] {@code datetime=<start>/..} open-end with double-dot → HTTP 200 or
 * 204.</li>
 * <li>[Req27/D] {@code datetime=<start>/} open-end with empty string → HTTP 200 or
 * 204.</li>
 * </ul>
 */
public class DateTimeDefinitionTest extends CommonFixture {

	private static final String DATETIME_CONF_URI = "https://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/datetime";

	private static final String REL_MAP = "http://www.opengis.net/def/rel/ogc/1.0/map";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	/** RFC 3339 Z instant used for instant and open-end interval tests. */
	private static final String INSTANT = "2000-01-01T00:00:00Z";

	/** Far-future date used as end of open-start interval tests. */
	private static final String FUTURE = "2099-12-31T23:59:59Z";

	private String mapUrl;

	private String sep;

	private boolean datetimeSupported;

	// -------------------------------------------------------------------------
	// Setup
	// -------------------------------------------------------------------------

	/**
	 * Checks whether the server declares the datetime conformance class and discovers the
	 * map endpoint. SkipException is not thrown here; each test method guards itself so
	 * that skip messages appear correctly in the report.
	 * @param testContext the TestNG test context
	 */
	@BeforeClass
	public void discoverTargets(ITestContext testContext) {
		datetimeSupported = isDatetimeDeclared();
		if (!datetimeSupported) {
			return;
		}
		mapUrl = findMapUrl(rootUri.toString());
		if (mapUrl != null) {
			sep = mapUrl.contains("?") ? "&" : "?";
		}
	}

	// -------------------------------------------------------------------------
	// Tests
	// -------------------------------------------------------------------------

	/**
	 * A.27 — Req 27/A: verifies that the {@code datetime} parameter is defined in the
	 * OpenAPI document with the required characteristics.
	 */
	@Test(description = "A.27 Req 27/A: datetime OAS definition — in=query, required=false, schema.type=string, "
			+ "style=form (or absent), explode=false (or absent).")
	public void verifyDatetimeOasDefinition() {
		if (!datetimeSupported) {
			throw new SkipException("Server does not declare " + DATETIME_CONF_URI + ". Skipping A.27.");
		}
		Map<String, Object> apiDoc = fetchApiDocument();
		if (apiDoc == null) {
			throw new SkipException("Could not retrieve OpenAPI document from " + rootUri + ". Skipping 27/A.");
		}
		String error = validateDatetimeParamInOas(apiDoc);
		clearMessages();
		if (error != null) {
			throw new AssertionError("A.27 verifyDatetimeOasDefinition failed:\n" + error);
		}
	}

	/**
	 * A.27 — Req 27/B + 27/C: verifies that a single RFC 3339 Z-notation instant is
	 * accepted, returning HTTP 200 or 204.
	 */
	@Test(description = "A.27 Req 27/B+C: datetime=<instant> (RFC 3339 Z-notation) → HTTP 200 or 204, "
			+ "non-empty body on 200.")
	public void verifyDatetimeInstant() {
		if (!datetimeSupported) {
			throw new SkipException("Server does not declare " + DATETIME_CONF_URI + ". Skipping A.27.");
		}
		if (mapUrl == null) {
			throw new SkipException("No map endpoint found at " + rootUri + ". Skipping A.27 instant test.");
		}
		List<String> errors = new ArrayList<>();
		HttpResult result = fetch(mapUrl + sep + "datetime=" + INSTANT + "&f=png");
		checkDatetimeResult(result, "[Req27/B+C]", "datetime=" + INSTANT, errors);
		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.27 verifyDatetimeInstant failed:\n" + String.join("\n", errors));
		}
	}

	/**
	 * A.27 — Req 27/B: verifies that a bounded interval ({@code start/end}) is accepted,
	 * returning HTTP 200 or 204.
	 */
	@Test(description = "A.27 Req 27/B: datetime=<start>/<end> bounded interval → HTTP 200 or 204.")
	public void verifyDatetimeBoundedInterval() {
		if (!datetimeSupported) {
			throw new SkipException("Server does not declare " + DATETIME_CONF_URI + ". Skipping A.27.");
		}
		if (mapUrl == null) {
			throw new SkipException("No map endpoint found at " + rootUri + ". Skipping A.27 bounded-interval test.");
		}
		List<String> errors = new ArrayList<>();
		String datetimeValue = INSTANT + "/" + FUTURE;
		HttpResult result = fetch(mapUrl + sep + "datetime=" + datetimeValue + "&f=png");
		checkDatetimeResult(result, "[Req27/B]", "datetime=" + datetimeValue, errors);
		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.27 verifyDatetimeBoundedInterval failed:\n" + String.join("\n", errors));
		}
	}

	/**
	 * A.27 — Req 27/D: verifies that an open-start interval using {@code ..} is accepted,
	 * returning HTTP 200 or 204.
	 */
	@Test(description = "A.27 Req 27/D: datetime=../<end> open-start with double-dot → HTTP 200 or 204.")
	public void verifyDatetimeOpenStartDoubleDot() {
		if (!datetimeSupported) {
			throw new SkipException("Server does not declare " + DATETIME_CONF_URI + ". Skipping A.27.");
		}
		if (mapUrl == null) {
			throw new SkipException(
					"No map endpoint found at " + rootUri + ". Skipping A.27 open-start double-dot test.");
		}
		List<String> errors = new ArrayList<>();
		String datetimeValue = "../" + FUTURE;
		HttpResult result = fetch(mapUrl + sep + "datetime=" + datetimeValue + "&f=png");
		checkDatetimeResult(result, "[Req27/D]", "datetime=" + datetimeValue, errors);
		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.27 verifyDatetimeOpenStartDoubleDot failed:\n" + String.join("\n", errors));
		}
	}

	/**
	 * A.27 — Req 27/D: verifies that an open-start interval using an empty string is
	 * accepted, returning HTTP 200 or 204.
	 */
	@Test(description = "A.27 Req 27/D: datetime=/<end> open-start with empty string → HTTP 200 or 204.")
	public void verifyDatetimeOpenStartEmpty() {
		if (!datetimeSupported) {
			throw new SkipException("Server does not declare " + DATETIME_CONF_URI + ". Skipping A.27.");
		}
		if (mapUrl == null) {
			throw new SkipException(
					"No map endpoint found at " + rootUri + ". Skipping A.27 open-start empty-string test.");
		}
		List<String> errors = new ArrayList<>();
		String datetimeValue = "/" + FUTURE;
		HttpResult result = fetch(mapUrl + sep + "datetime=" + datetimeValue + "&f=png");
		checkDatetimeResult(result, "[Req27/D]", "datetime=" + datetimeValue, errors);
		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.27 verifyDatetimeOpenStartEmpty failed:\n" + String.join("\n", errors));
		}
	}

	/**
	 * A.27 — Req 27/D: verifies that an open-end interval using {@code ..} is accepted,
	 * returning HTTP 200 or 204.
	 */
	@Test(description = "A.27 Req 27/D: datetime=<start>/.. open-end with double-dot → HTTP 200 or 204.")
	public void verifyDatetimeOpenEndDoubleDot() {
		if (!datetimeSupported) {
			throw new SkipException("Server does not declare " + DATETIME_CONF_URI + ". Skipping A.27.");
		}
		if (mapUrl == null) {
			throw new SkipException(
					"No map endpoint found at " + rootUri + ". Skipping A.27 open-end double-dot test.");
		}
		List<String> errors = new ArrayList<>();
		String datetimeValue = INSTANT + "/..";
		HttpResult result = fetch(mapUrl + sep + "datetime=" + datetimeValue + "&f=png");
		checkDatetimeResult(result, "[Req27/D]", "datetime=" + datetimeValue, errors);
		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.27 verifyDatetimeOpenEndDoubleDot failed:\n" + String.join("\n", errors));
		}
	}

	/**
	 * A.27 — Req 27/D: verifies that an open-end interval using an empty string is
	 * accepted, returning HTTP 200 or 204.
	 */
	@Test(description = "A.27 Req 27/D: datetime=<start>/ open-end with empty string → HTTP 200 or 204.")
	public void verifyDatetimeOpenEndEmpty() {
		if (!datetimeSupported) {
			throw new SkipException("Server does not declare " + DATETIME_CONF_URI + ". Skipping A.27.");
		}
		if (mapUrl == null) {
			throw new SkipException(
					"No map endpoint found at " + rootUri + ". Skipping A.27 open-end empty-string test.");
		}
		List<String> errors = new ArrayList<>();
		String datetimeValue = INSTANT + "/";
		HttpResult result = fetch(mapUrl + sep + "datetime=" + datetimeValue + "&f=png");
		checkDatetimeResult(result, "[Req27/D]", "datetime=" + datetimeValue, errors);
		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.27 verifyDatetimeOpenEndEmpty failed:\n" + String.join("\n", errors));
		}
	}

	// -------------------------------------------------------------------------
	// Assertion helper
	// -------------------------------------------------------------------------

	/**
	 * Checks that the HTTP result has status 200 or 204, and that a 200 response has a
	 * non-empty body. Appends any failures to {@code errors}.
	 */
	private void checkDatetimeResult(HttpResult result, String reqId, String paramLabel, List<String> errors) {
		if (result.status != 200 && result.status != 204) {
			errors.add(reqId + " Expected HTTP 200 or 204 for " + paramLabel + " but got HTTP " + result.status + ".");
		}
		else if (result.status == 200 && result.body.length == 0) {
			errors.add(reqId + " HTTP 200 received but response body was empty.");
		}
	}

	// -------------------------------------------------------------------------
	// OAS validation helpers
	// -------------------------------------------------------------------------

	/**
	 * Locates the {@code datetime} parameter in the API document (components first, then
	 * path operations) and validates its schema. Returns an error message, or
	 * {@code null} when valid or when the parameter is not found.
	 */
	private String validateDatetimeParamInOas(Map<String, Object> apiDoc) {
		Map<String, Object> param = findDatetimeParamInComponents(apiDoc);
		if (param == null) {
			param = findDatetimeParamInPaths(apiDoc);
		}
		return param != null ? validateDatetimeParamSchema(param) : null;
	}

	/** Looks up {@code components/parameters/datetime} in the API document. */
	@SuppressWarnings("unchecked")
	private Map<String, Object> findDatetimeParamInComponents(Map<String, Object> apiDoc) {
		Map<String, Object> components = castMap(apiDoc.get("components"));
		if (components == null) {
			return null;
		}
		Map<String, Object> parameters = castMap(components.get("parameters"));
		if (parameters == null) {
			return null;
		}
		return castMap(parameters.get("datetime"));
	}

	/** Searches every path item in {@code paths} for a {@code datetime} parameter. */
	private Map<String, Object> findDatetimeParamInPaths(Map<String, Object> apiDoc) {
		Map<String, Object> paths = castMap(apiDoc.get("paths"));
		if (paths == null) {
			return null;
		}
		for (Object pathItemObj : paths.values()) {
			Map<String, Object> found = findDatetimeParamInPathItem(castMap(pathItemObj));
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	/** Searches GET and POST operations within a single path item. */
	private Map<String, Object> findDatetimeParamInPathItem(Map<String, Object> pathItem) {
		if (pathItem == null) {
			return null;
		}
		for (String method : Arrays.asList("get", "post")) {
			Map<String, Object> found = findDatetimeParamInOperation(castMap(pathItem.get(method)));
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	/** Returns the {@code datetime} entry from an operation's parameter list, or null. */
	private Map<String, Object> findDatetimeParamInOperation(Map<String, Object> operation) {
		if (operation == null) {
			return null;
		}
		Object paramsObj = operation.get("parameters");
		if (!(paramsObj instanceof List)) {
			return null;
		}
		for (Object p : (List<?>) paramsObj) {
			Map<String, Object> param = castMap(p);
			if (param != null && "datetime".equals(param.get("name"))) {
				return param;
			}
		}
		return null;
	}

	/**
	 * Validates the individual OAS fields of the {@code datetime} parameter. Returns a
	 * semicolon-separated error string, or {@code null} when all fields are correct.
	 */
	private String validateDatetimeParamSchema(Map<String, Object> param) {
		List<String> errors = new ArrayList<>();
		checkParamIn(param, errors);
		checkParamRequired(param, errors);
		checkSchemaType(param, errors);
		checkParamStyle(param, errors);
		checkParamExplode(param, errors);
		return errors.isEmpty() ? null : String.join("; ", errors);
	}

	private void checkParamIn(Map<String, Object> param, List<String> errors) {
		Object in = param.get("in");
		if (!"query".equals(in)) {
			errors.add("Expected 'in=query' but found 'in=" + in + "'.");
		}
	}

	private void checkParamRequired(Map<String, Object> param, List<String> errors) {
		if (Boolean.TRUE.equals(param.get("required"))) {
			errors.add("Expected 'required=false' (or absent) but found 'required=true'.");
		}
	}

	private void checkSchemaType(Map<String, Object> param, List<String> errors) {
		Map<String, Object> schema = castMap(param.get("schema"));
		if (schema == null) {
			return;
		}
		Object type = schema.get("type");
		if (type != null && !"string".equals(type)) {
			errors.add("Expected 'schema.type=string' but found 'schema.type=" + type + "'.");
		}
	}

	private void checkParamStyle(Map<String, Object> param, List<String> errors) {
		Object style = param.get("style");
		if (style != null && !"form".equals(style)) {
			errors.add("Expected 'style=form' (or absent) but found 'style=" + style + "'.");
		}
	}

	private void checkParamExplode(Map<String, Object> param, List<String> errors) {
		if (Boolean.TRUE.equals(param.get("explode"))) {
			errors.add("Expected 'explode=false' (or absent) but found 'explode=true'.");
		}
	}

	// -------------------------------------------------------------------------
	// Discovery helpers
	// -------------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	private boolean isDatetimeDeclared() {
		try {
			Map<String, Object> conformance = fetchJson(baseUrl() + "/conformance?f=json");
			if (conformance == null) {
				return false;
			}
			List<String> conformsTo = (List<String>) conformance.get("conformsTo");
			return conformsTo != null && conformsTo.stream()
				.anyMatch(u -> u != null && normalizeScheme(u).equals(normalizeScheme(DATETIME_CONF_URI)));
		}
		catch (Exception e) {
			return false;
		}
	}

	/**
	 * Fetches the OpenAPI document, trying {@code /api?f=json} then the service-desc
	 * link.
	 */
	private Map<String, Object> fetchApiDocument() {
		Map<String, Object> doc = fetchJson(baseUrl() + "/api?f=json");
		if (doc != null) {
			return doc;
		}
		String serviceDescUrl = findServiceDescUrl();
		return serviceDescUrl != null ? fetchJson(serviceDescUrl) : null;
	}

	/** Finds the service-desc URL from the landing page links. */
	@SuppressWarnings("unchecked")
	private String findServiceDescUrl() {
		try {
			Map<String, Object> landing = fetchJson(baseUrl() + "?f=json");
			if (landing == null) {
				return null;
			}
			List<Map<String, Object>> links = (List<Map<String, Object>>) landing.get("links");
			if (links == null) {
				return null;
			}
			for (Map<String, Object> link : links) {
				String rel = (String) link.get("rel");
				if (isServiceDescRel(rel)) {
					String href = (String) link.get("href");
					if (href != null) {
						return href + (href.contains("?") ? "&" : "?") + "f=json";
					}
				}
			}
		}
		catch (Exception e) {
			// fall through
		}
		return null;
	}

	private boolean isServiceDescRel(String rel) {
		return "service-desc".equals(rel) || "http://www.opengis.net/def/rel/ogc/1.0/api-definition".equals(rel);
	}

	/** Returns the href of the first map link in the landing page, or a default URL. */
	@SuppressWarnings("unchecked")
	private String findMapUrl(String landingPageUrl) {
		try {
			Map<String, Object> landingPage = fetchJson(baseUrl() + "?f=json");
			if (landingPage != null) {
				List<Map<String, Object>> links = (List<Map<String, Object>>) landingPage.get("links");
				String href = findMapHref(links);
				if (href != null) {
					return resolveUrl(landingPageUrl, href);
				}
			}
		}
		catch (Exception e) {
			// fall through to default
		}
		return baseUrl() + "/map";
	}

	/** Returns the href of the first link whose rel matches the map relation, or null. */
	private String findMapHref(List<Map<String, Object>> links) {
		if (links == null) {
			return null;
		}
		for (Map<String, Object> link : links) {
			String rel = (String) link.get("rel");
			if (rel != null && matchesRelIgnoringScheme(rel, REL_MAP)) {
				return (String) link.get("href");
			}
		}
		return null;
	}

	// -------------------------------------------------------------------------
	// HTTP helpers
	// -------------------------------------------------------------------------

	private Map<String, Object> fetchJson(String urlString) {
		try {
			HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");
			conn.setConnectTimeout(30000);
			conn.setReadTimeout(30000);
			conn.setInstanceFollowRedirects(true);
			applyAuth(conn);
			if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
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

	private HttpResult fetch(String urlString) {
		try {
			HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(30000);
			conn.setReadTimeout(60000);
			conn.setInstanceFollowRedirects(true);
			applyAuth(conn);
			int status = conn.getResponseCode();
			byte[] body = new byte[0];
			if (status == 200) {
				try (InputStream is = conn.getInputStream()) {
					body = readAllBytes(is);
				}
			}
			return new HttpResult(status, body);
		}
		catch (Exception e) {
			System.err.println("[A.27] HTTP request failed for: " + urlString + " => " + e.getMessage());
			return new HttpResult(-1, new byte[0]);
		}
	}

	private byte[] readAllBytes(InputStream is) throws Exception {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		byte[] chunk = new byte[8192];
		int n;
		while ((n = is.read(chunk)) != -1) {
			buffer.write(chunk, 0, n);
		}
		return buffer.toByteArray();
	}

	// -------------------------------------------------------------------------
	// Utility helpers
	// -------------------------------------------------------------------------

	private String baseUrl() {
		String url = rootUri.toString();
		return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
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

	private String normalizeScheme(String uri) {
		if (uri != null && uri.startsWith("https://")) {
			return "http://" + uri.substring("https://".length());
		}
		return uri;
	}

	/**
	 * Safe cast from Object to {@code Map<String, Object>}; returns null if not a Map.
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Object> castMap(Object obj) {
		return obj instanceof Map ? (Map<String, Object>) obj : null;
	}

	// -------------------------------------------------------------------------
	// Inner class
	// -------------------------------------------------------------------------

	private static class HttpResult {

		final int status;

		final byte[] body;

		HttpResult(int status, byte[] body) {
			this.status = status;
			this.body = body;
		}

	}

}
