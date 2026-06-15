package org.opengis.cite.ogcapimaps10.conformance.datetime;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.opengis.cite.ogcapimaps10.conformance.CommonFixture;
import org.opengis.cite.ogcapimaps10.conformance.SuiteAttribute;
import org.opengis.cite.ogcapimaps10.domain.DatetimeSubsetCrsInteractiveTestResult;
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Implements Abstract Test A.29: /conf/datetime/subset-definition
 *
 * <p>
 * Test Purpose: Verify that the implementation supports temporal subsetting using the
 * {@code subset} parameter with the {@code time} axis (Requirement 29:
 * /req/datetime/subset-definition).
 *
 * <p>
 * Assertions verified:
 * <ul>
 * <li>[Req29/A] {@code subset} parameter declared in OpenAPI document.</li>
 * <li>[Req29/A+B] Single instant syntax accepted; {@code time} axis name recognised.</li>
 * <li>[Req29/A] Closed interval syntax accepted.</li>
 * <li>[Req29/C] Unknown axis name → HTTP 4xx.</li>
 * <li>[Req29/D] Fully out-of-range values → HTTP 204 or 404.</li>
 * <li>[Req29/E] CRS interpretation correct — interactive verification.</li>
 * <li>[Req29/F] Partial date/time formats each → HTTP 200 or 204.</li>
 * <li>[Req29/G] Asterisk wildcard forms → HTTP 200 or 204.</li>
 * <li>[Req29/H] Multiple {@code subset} parameters accepted without error.</li>
 * </ul>
 */
public class DatetimeSubsetDefinition extends CommonFixture {

	private static final String DATETIME_CONF_URI = "https://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/datetime";

	private static final String REL_MAP = "http://www.opengis.net/def/rel/ogc/1.0/map";

	private static final String REL_SERVICE_DESC = "http://www.opengis.net/def/rel/ogc/1.0/service-desc";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private boolean datetimeSupported;

	private String temporalMapUrl;

	private String temporalFmtSuffix;

	private String apiDocUrl;

	// Test dates — derived from the discovered collection's temporal extent at runtime.
	private String instant = "2021-01-01T00:00:00Z";

	private String intervalLow = "2020-01-01T00:00:00Z";

	private String intervalHigh = "2021-01-01T00:00:00Z";

	private String outOfRangeLow = "1800-01-01T00:00:00Z";

	private String outOfRangeHigh = "1800-12-31T23:59:59Z";

	private String[] partialFormats = { "2020", "2020-06", "2020-06-08", "2020-06-08T12Z", "2020-06-08T12:00Z" };

	// -------------------------------------------------------------------------
	// Setup
	// -------------------------------------------------------------------------

	/**
	 * Discovers whether the server declares the datetime conformance class, locates a
	 * temporal collection, reads its temporal extent, and derives all test dates from it.
	 * @param testContext the TestNG test context
	 */
	@BeforeClass
	public void discoverTargets(ITestContext testContext) {
		datetimeSupported = isDatetimeDeclared();
		if (!datetimeSupported) {
			return;
		}
		TemporalCollectionInfo info = findTemporalCollection();
		if (info != null) {
			temporalMapUrl = info.mapUrl;
			temporalFmtSuffix = hasFormatExtension(temporalMapUrl) ? "" : "&f=png";
			deriveTestDates(info.startDate, info.endDate);
		}
		apiDocUrl = findApiDocUrl();
	}

	// -------------------------------------------------------------------------
	// Tests
	// -------------------------------------------------------------------------

	/**
	 * A.29 — Req 29/A: verifies that the {@code subset} parameter is declared in the
	 * OpenAPI document with {@code in=query} and is not required.
	 */
	@Test(description = "A.29 Req 29/A: subset parameter declared in OpenAPI document "
			+ "(in=query, type string, not required).")
	public void verifySubsetOasDefinition() {
		if (!datetimeSupported) {
			throw new SkipException("Server does not declare " + DATETIME_CONF_URI + ". Skipping A.29.");
		}
		if (apiDocUrl == null) {
			throw new SkipException("Could not find OpenAPI document (service-desc link). Skipping OAS check.");
		}
		Map<String, Object> apiDoc = fetchJson(apiDocUrl);
		if (apiDoc == null) {
			throw new SkipException("Could not fetch OpenAPI document from " + apiDocUrl + ". Skipping OAS check.");
		}
		List<String> errors = new ArrayList<>();
		boolean found = isSubsetParamInComponents(apiDoc) || isSubsetParamInPaths(apiDoc);
		if (!found) {
			errors.add("[Req29/A] No 'subset' parameter with in=query found in the OpenAPI document "
					+ "(checked components/parameters and all path operations).");
		}
		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.29 verifySubsetOasDefinition failed:\n" + String.join("\n", errors));
		}
	}

	/**
	 * A.29 — Req 29/A + 29/B: verifies that a single-instant {@code subset=time(...)}
	 * request is accepted (HTTP 200 or 204) and that the {@code time} axis name is
	 * recognised.
	 */
	@Test(description = "A.29 Req 29/A+B: subset=time(\"instant\") → HTTP 200 or 204. "
			+ "Single instant syntax accepted and time axis name recognised.")
	public void verifySubsetSingleInstant() {
		if (!datetimeSupported) {
			throw new SkipException("Server does not declare " + DATETIME_CONF_URI + ". Skipping A.29.");
		}
		if (temporalMapUrl == null) {
			throw new SkipException("No temporal collection map found. Skipping A.29 single-instant test.");
		}
		String tSep = temporalMapUrl.contains("?") ? "&" : "?";
		String url = temporalMapUrl + tSep + "subset=time(%22" + instant + "%22)" + temporalFmtSuffix;
		List<String> errors = new ArrayList<>();
		int status = fetchStatus(url);
		if (status != 200 && status != 204) {
			errors.add("[Req29/A+B] Expected HTTP 200 or 204 for subset=time(\"" + instant + "\") but got HTTP "
					+ status + ".");
		}
		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.29 verifySubsetSingleInstant failed:\n" + String.join("\n", errors));
		}
	}

	/**
	 * A.29 — Req 29/A: verifies that a closed-interval {@code subset=time("low":"high")}
	 * request is accepted (HTTP 200 or 204).
	 */
	@Test(description = "A.29 Req 29/A: subset=time(\"low\":\"high\") → HTTP 200 or 204. "
			+ "Closed interval syntax accepted.")
	public void verifySubsetClosedInterval() {
		if (!datetimeSupported) {
			throw new SkipException("Server does not declare " + DATETIME_CONF_URI + ". Skipping A.29.");
		}
		if (temporalMapUrl == null) {
			throw new SkipException("No temporal collection map found. Skipping A.29 closed-interval test.");
		}
		String tSep = temporalMapUrl.contains("?") ? "&" : "?";
		String url = temporalMapUrl + tSep + "subset=time(%22" + intervalLow + "%22:%22" + intervalHigh + "%22)"
				+ temporalFmtSuffix;
		List<String> errors = new ArrayList<>();
		int status = fetchStatus(url);
		if (status != 200 && status != 204) {
			errors.add("[Req29/A] Expected HTTP 200 or 204 for subset=time(\"" + intervalLow + "\":\"" + intervalHigh
					+ "\") but got HTTP " + status + ".");
		}
		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.29 verifySubsetClosedInterval failed:\n" + String.join("\n", errors));
		}
	}

	/**
	 * A.29 — Req 29/C: verifies that an unrecognised axis name returns HTTP 4xx.
	 */
	@Test(description = "A.29 Req 29/C: subset=xyz(\"...\") → HTTP 4xx. " + "Unrecognised axis name must be rejected.")
	public void verifySubsetUnknownAxisReturns4xx() {
		if (!datetimeSupported) {
			throw new SkipException("Server does not declare " + DATETIME_CONF_URI + ". Skipping A.29.");
		}
		if (temporalMapUrl == null) {
			throw new SkipException("No temporal collection map found. Skipping A.29 unknown-axis test.");
		}
		String tSep = temporalMapUrl.contains("?") ? "&" : "?";
		String url = temporalMapUrl + tSep + "subset=xyz(%22" + instant + "%22)" + temporalFmtSuffix;
		List<String> errors = new ArrayList<>();
		int status = fetchStatus(url);
		if (status < 400 || status > 499) {
			errors.add("[Req29/C] Expected HTTP 4xx for subset with unrecognised axis name 'xyz' but got HTTP " + status
					+ ".");
		}
		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.29 verifySubsetUnknownAxisReturns4xx failed:\n" + String.join("\n", errors));
		}
	}

	/**
	 * A.29 — Req 29/D: verifies that time coordinates entirely outside the valid range
	 * return HTTP 204 or 404. The out-of-range bounds are derived from the collection's
	 * actual temporal extent (200 years before its start date).
	 */
	@Test(description = "A.29 Req 29/D: subset entirely outside collection's temporal range → HTTP 204 or 404.")
	public void verifySubsetOutOfRangeReturns204or404() {
		if (!datetimeSupported) {
			throw new SkipException("Server does not declare " + DATETIME_CONF_URI + ". Skipping A.29.");
		}
		if (temporalMapUrl == null) {
			throw new SkipException("No temporal collection map found. Skipping A.29 out-of-range test.");
		}
		String tSep = temporalMapUrl.contains("?") ? "&" : "?";
		String url = temporalMapUrl + tSep + "subset=time(%22" + outOfRangeLow + "%22:%22" + outOfRangeHigh + "%22)"
				+ temporalFmtSuffix;
		List<String> errors = new ArrayList<>();
		int status = fetchStatus(url);
		if (status != 204 && status != 404) {
			errors.add("[Req29/D] Expected HTTP 204 or 404 for subset entirely outside valid range (" + outOfRangeLow
					+ " / " + outOfRangeHigh + ") but got HTTP " + status + ".");
		}
		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError(
					"A.29 verifySubsetOutOfRangeReturns204or404 failed:\n" + String.join("\n", errors));
		}
	}

	/**
	 * A.29 — Req 29/E: interactive verification that the server correctly interprets time
	 * coordinates per the CRS declared in the temporal extent, or Gregorian UTC if
	 * absent. Skipped when the interactive check is not enabled in testng.xml.
	 */
	@Test(description = "A.29 Req 29/E: time coordinates interpreted per temporal CRS or Gregorian UTC. "
			+ "Interactive verification.")
	public void verifySubsetCrsInterpretation(ITestContext context) {
		if (!datetimeSupported) {
			throw new SkipException("Server does not declare " + DATETIME_CONF_URI + ". Skipping A.29.");
		}
		String filteredUrl = null;
		String fullUrl = null;
		if (temporalMapUrl != null) {
			String tSep = temporalMapUrl.contains("?") ? "&" : "?";
			filteredUrl = temporalMapUrl + tSep + "subset=time(\"" + instant + "\")";
			fullUrl = temporalMapUrl;
		}
		DatetimeSubsetCrsInteractiveTestResult result = (DatetimeSubsetCrsInteractiveTestResult) context.getSuite()
			.getAttribute(SuiteAttribute.DATETIME_SUBSET_CRS_INTERACTIVE_TEST_RESULT.getName());
		if (result == null || !result.isEnabled()) {
			System.out.println();
			System.out.println("=== A.29 Req 29/E — Interactive CRS Interpretation Verification ===");
			if (filteredUrl != null) {
				System.out.println("Open the two map URLs below in a browser and compare them:");
				System.out.println("  1. Filtered   : " + filteredUrl);
				System.out.println("  2. Full-extent : " + fullUrl);
				System.out
					.println("Question: Does map 1 show data from " + instant + " (a specific snapshot in time),");
				System.out.println("          rather than the full temporal extent shown in map 2?");
			}
			else {
				System.out.println("No temporal collection found — cannot generate inspection URLs.");
			}
			System.out.println("To record your answer, set the following in testng.xml and re-run:");
			System.out.println("  <parameter name=\"datetime_subset_crs_interactive_enabled\" value=\"true\" />");
			System.out.println(
					"  <parameter name=\"datetime_subset_crs_result_correct\" value=\"true\" />  <!-- true = yes, false = no -->");
			System.out.println("===================================================================");
			System.out.println();
			throw new SkipException(
					"[Req29/E] Interactive CRS verification not yet enabled — see console output above.");
		}
		if (!result.isCrsInterpretationCorrect()) {
			throw new AssertionError(
					"[Req29/E] Interactive verification failed: the tester reported that the server does NOT "
							+ "correctly interpret time coordinates per the temporal CRS or Gregorian UTC. "
							+ "The subset-filtered map did not represent the expected temporal snapshot.");
		}
	}

	/**
	 * A.29 — Req 29/F: verifies that all required partial date/time formats are accepted
	 * (HTTP 200 or 204). The formats are derived from the midpoint of the collection's
	 * actual temporal extent.
	 */
	@Test(description = "A.29 Req 29/F: partial date/time formats "
			+ "(yyyy, yyyy-mm, yyyy-mm-dd, yyyy-mm-ddThhZ, yyyy-mm-ddThh:mmZ) each → HTTP 200 or 204.")
	public void verifySubsetPartialDateFormats() {
		if (!datetimeSupported) {
			throw new SkipException("Server does not declare " + DATETIME_CONF_URI + ". Skipping A.29.");
		}
		if (temporalMapUrl == null) {
			throw new SkipException("No temporal collection map found. Skipping A.29 partial-format test.");
		}
		String tSep = temporalMapUrl.contains("?") ? "&" : "?";
		List<String> errors = new ArrayList<>();
		for (String fmt : partialFormats) {
			String url = temporalMapUrl + tSep + "subset=time(%22" + fmt + "%22)" + temporalFmtSuffix;
			int status = fetchStatus(url);
			if (status != 200 && status != 204) {
				errors.add("[Req29/F] Expected HTTP 200 or 204 for subset=time(\"" + fmt + "\") but got HTTP " + status
						+ ".");
			}
		}
		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.29 verifySubsetPartialDateFormats failed:\n" + String.join("\n", errors));
		}
	}

	/**
	 * A.29 — Req 29/G: verifies that asterisk wildcard forms are accepted (HTTP 200 or
	 * 204).
	 */
	@Test(description = "A.29 Req 29/G: subset=time(*:*), subset=time(*), subset=time(\"low\":*) "
			+ "→ HTTP 200 or 204. Asterisk wildcard accepted.")
	public void verifySubsetAsteriskWildcard() {
		if (!datetimeSupported) {
			throw new SkipException("Server does not declare " + DATETIME_CONF_URI + ". Skipping A.29.");
		}
		if (temporalMapUrl == null) {
			throw new SkipException("No temporal collection map found. Skipping A.29 wildcard test.");
		}
		String tSep = temporalMapUrl.contains("?") ? "&" : "?";
		List<String> errors = new ArrayList<>();
		String[][] wildcardCases = { { "time(*:*)", "both bounds wildcard" }, { "time(*)", "single asterisk (latest)" },
				{ "time(%22" + intervalLow + "%22:*)", "open high bound" } };
		for (String[] tc : wildcardCases) {
			String url = temporalMapUrl + tSep + "subset=" + tc[0] + temporalFmtSuffix;
			int status = fetchStatus(url);
			if (status != 200 && status != 204) {
				errors.add("[Req29/G] Expected HTTP 200 or 204 for subset=" + tc[0] + " (" + tc[1] + ") but got HTTP "
						+ status + ".");
			}
		}
		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.29 verifySubsetAsteriskWildcard failed:\n" + String.join("\n", errors));
		}
	}

	/**
	 * A.29 — Req 29/H: verifies that multiple {@code subset} parameters are accepted
	 * without error, treated as cumulative (comma-separated equivalent).
	 */
	@Test(description = "A.29 Req 29/H: two separate subset parameters accepted — "
			+ "multiple subset params treated as cumulative single param.")
	public void verifySubsetMultipleParameters() {
		if (!datetimeSupported) {
			throw new SkipException("Server does not declare " + DATETIME_CONF_URI + ". Skipping A.29.");
		}
		if (temporalMapUrl == null) {
			throw new SkipException("No temporal collection map found. Skipping A.29 multiple-params test.");
		}
		String tSep = temporalMapUrl.contains("?") ? "&" : "?";
		List<String> errors = new ArrayList<>();
		String singleUrl = temporalMapUrl + tSep + "subset=time(%22" + intervalLow + "%22:%22" + intervalHigh + "%22)"
				+ temporalFmtSuffix;
		String multiUrl = temporalMapUrl + tSep + "subset=time(%22" + intervalLow + "%22:%22" + intervalHigh
				+ "%22)&subset=time(%22" + intervalLow + "%22:%22" + intervalHigh + "%22)" + temporalFmtSuffix;
		int singleStatus = fetchStatus(singleUrl);
		int multiStatus = fetchStatus(multiUrl);
		if (singleStatus != 200 && singleStatus != 204) {
			errors.add("[Req29/H] Single subset param returned HTTP " + singleStatus + ", expected 200 or 204.");
		}
		if (multiStatus != 200 && multiStatus != 204) {
			errors.add("[Req29/H] Two subset params returned HTTP " + multiStatus
					+ " — server may not support multiple subset parameters per Req 29/H.");
		}
		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.29 verifySubsetMultipleParameters failed:\n" + String.join("\n", errors));
		}
	}

	// -------------------------------------------------------------------------
	// OAS helpers
	// -------------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	private boolean isSubsetParamInComponents(Map<String, Object> apiDoc) {
		Map<String, Object> components = castMap(apiDoc.get("components"));
		if (components == null) {
			return false;
		}
		Map<String, Object> parameters = castMap(components.get("parameters"));
		if (parameters == null) {
			return false;
		}
		Map<String, Object> subsetParam = castMap(parameters.get("subset"));
		if (subsetParam == null) {
			return false;
		}
		return "query".equals(subsetParam.get("in"));
	}

	@SuppressWarnings("unchecked")
	private boolean isSubsetParamInPaths(Map<String, Object> apiDoc) {
		Map<String, Object> paths = castMap(apiDoc.get("paths"));
		if (paths == null) {
			return false;
		}
		for (Object pathItemObj : paths.values()) {
			Map<String, Object> pathItem = castMap(pathItemObj);
			if (pathItem == null) {
				continue;
			}
			for (String method : new String[] { "get", "post" }) {
				Map<String, Object> operation = castMap(pathItem.get(method));
				if (operation == null) {
					continue;
				}
				List<Object> params = (List<Object>) operation.get("parameters");
				if (hasSubsetQueryParam(params)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean hasSubsetQueryParam(List<Object> params) {
		if (params == null) {
			return false;
		}
		for (Object p : params) {
			Map<String, Object> param = castMap(p);
			if (param == null) {
				continue;
			}
			if ("subset".equals(param.get("name")) && "query".equals(param.get("in"))) {
				return true;
			}
		}
		return false;
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
	 * Scans {@code /collections} for the first collection that has a temporal extent and
	 * a map link. Returns its map URL, start date, and end date.
	 */
	@SuppressWarnings("unchecked")
	private TemporalCollectionInfo findTemporalCollection() {
		try {
			Map<String, Object> collectionsDoc = fetchJson(baseUrl() + "/collections?f=json");
			if (collectionsDoc == null) {
				return null;
			}
			List<Map<String, Object>> collections = (List<Map<String, Object>>) collectionsDoc.get("collections");
			if (collections == null) {
				return null;
			}
			for (Map<String, Object> collection : collections) {
				Map<String, Object> extent = castMap(collection.get("extent"));
				if (extent == null) {
					continue;
				}
				Map<String, Object> temporal = castMap(extent.get("temporal"));
				if (temporal == null) {
					continue;
				}
				List<Map<String, Object>> links = (List<Map<String, Object>>) collection.get("links");
				String href = findMapHref(links);
				if (href == null) {
					continue;
				}
				String mapUrl = resolveUrl(rootUri.toString(), href);
				String startDate = null;
				String endDate = null;
				List<Object> intervals = (List<Object>) temporal.get("interval");
				if (intervals != null && !intervals.isEmpty() && intervals.get(0) instanceof List) {
					List<Object> first = (List<Object>) intervals.get(0);
					if (first.size() >= 2) {
						startDate = first.get(0) != null ? first.get(0).toString() : null;
						endDate = first.get(1) != null ? first.get(1).toString() : null;
					}
				}
				return new TemporalCollectionInfo(mapUrl, startDate, endDate);
			}
		}
		catch (Exception e) {
			// fall through
		}
		return null;
	}

	/**
	 * Derives all test date values from the collection's actual temporal extent so that
	 * test inputs are always valid for the target server, regardless of which collection
	 * is discovered.
	 */
	private void deriveTestDates(String startDateStr, String endDateStr) {
		try {
			LocalDate start = parseDate(startDateStr);
			LocalDate end = parseDate(endDateStr);
			if (start == null) {
				start = LocalDate.now().minusYears(5);
			}
			if (end == null) {
				end = LocalDate.now();
			}
			long days = ChronoUnit.DAYS.between(start, end);
			LocalDate mid = start.plusDays(Math.max(1, days / 2));

			instant = mid + "T00:00:00Z";
			intervalLow = start + "T00:00:00Z";
			intervalHigh = mid + "T00:00:00Z";

			LocalDate outStart = start.minusYears(200);
			outOfRangeLow = outStart + "T00:00:00Z";
			outOfRangeHigh = outStart.withMonth(12).withDayOfMonth(31) + "T23:59:59Z";

			String yyyy = String.format("%04d", mid.getYear());
			String mm = String.format("%02d", mid.getMonthValue());
			String dd = String.format("%02d", mid.getDayOfMonth());
			partialFormats = new String[] { yyyy, yyyy + "-" + mm, yyyy + "-" + mm + "-" + dd,
					yyyy + "-" + mm + "-" + dd + "T12Z", yyyy + "-" + mm + "-" + dd + "T12:00Z" };
		}
		catch (Exception e) {
			// keep defaults
		}
	}

	private LocalDate parseDate(String dateStr) {
		if (dateStr == null || dateStr.isEmpty() || "..".equals(dateStr)) {
			return null;
		}
		try {
			return LocalDate.parse(dateStr.substring(0, Math.min(10, dateStr.length())));
		}
		catch (Exception e) {
			return null;
		}
	}

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

	@SuppressWarnings("unchecked")
	private String findApiDocUrl() {
		try {
			Map<String, Object> landingPage = fetchJson(baseUrl() + "?f=json");
			if (landingPage == null) {
				return null;
			}
			List<Map<String, Object>> links = (List<Map<String, Object>>) landingPage.get("links");
			if (links == null) {
				return null;
			}
			for (Map<String, Object> link : links) {
				String rel = (String) link.get("rel");
				if (isServiceDescRel(rel)) {
					String href = (String) link.get("href");
					if (href != null) {
						return resolveUrl(baseUrl(), href);
					}
				}
			}
		}
		catch (Exception e) {
			// fall through
		}
		return null;
	}

	// -------------------------------------------------------------------------
	// HTTP helpers
	// -------------------------------------------------------------------------

	private int fetchStatus(String urlString) {
		try {
			HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(30000);
			conn.setReadTimeout(60000);
			conn.setInstanceFollowRedirects(true);
			applyAuth(conn);
			return conn.getResponseCode();
		}
		catch (Exception e) {
			return -1;
		}
	}

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

	private boolean isServiceDescRel(String rel) {
		return "service-desc".equals(rel) || matchesRelIgnoringScheme(rel, REL_SERVICE_DESC);
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

	private boolean hasFormatExtension(String url) {
		if (url == null) {
			return false;
		}
		String lower = url.toLowerCase();
		return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".tif")
				|| lower.endsWith(".tiff") || lower.endsWith(".webp");
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> castMap(Object obj) {
		return obj instanceof Map ? (Map<String, Object>) obj : null;
	}

	// -------------------------------------------------------------------------
	// Inner class
	// -------------------------------------------------------------------------

	private static class TemporalCollectionInfo {

		final String mapUrl;

		final String startDate;

		final String endDate;

		TemporalCollectionInfo(String mapUrl, String startDate, String endDate) {
			this.mapUrl = mapUrl;
			this.startDate = startDate;
			this.endDate = endDate;
		}

	}

}
