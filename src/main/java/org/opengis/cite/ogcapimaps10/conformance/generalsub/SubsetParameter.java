package org.opengis.cite.ogcapimaps10.conformance.generalsub;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.opengis.cite.ogcapimaps10.conformance.CommonFixture;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import io.restassured.specification.RequestSpecification;

/**
 * Implements Abstract Test A.34: /conf/general-subsetting/subset
 *
 * <p>
 * Test Purpose: Verify that the implementation correctly supports the {@code subset}
 * query parameter for map resources that declare the general-subsetting conformance class
 * (Requirement 34: /req/general-subsetting/subset).
 *
 * <p>
 * Assertions verified:
 * <ul>
 * <li>[Req34/A-valid-range] A {@code subset=dim(low:high)} request with values within the
 * declared {@code interval} returns HTTP 200 and {@code Content-Type: image/*}.</li>
 * <li>[Req34/A-valid-point] A {@code subset=dim(value)} request with a value within the
 * declared {@code interval} returns HTTP 200 and {@code Content-Type: image/*}.</li>
 * <li>[Req34/A-invalid-dim] A {@code subset} request using an unknown dimension name
 * returns HTTP 400.</li>
 * <li>[Req34/A-out-of-range] A {@code subset} request with values entirely outside the
 * declared {@code interval} returns HTTP 400 or HTTP 404.</li>
 * </ul>
 */
public class SubsetParameter extends CommonFixture {

	private static final String GENERAL_SUBSETTING_CONF_URI = "https://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/general-subsetting";

	private static final Set<String> STANDARD_EXTENT_KEYS = new HashSet<>(Arrays.asList("spatial", "temporal"));

	private static final String MAP_REL_HTTPS = "https://www.opengis.net/def/rel/ogc/1.0/map";

	private static final String MAP_REL_HTTP = "http://www.opengis.net/def/rel/ogc/1.0/map";

	private static final String MAP_REL_COMPACT = "ogc-rel:map";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private boolean generalSubsettingSupported;

	/** One entry per (collection, dimension, mapUrl) triple that can be tested. */
	private final List<CollectionTarget> targets = new ArrayList<>();

	// -------------------------------------------------------------------------
	// Setup
	// -------------------------------------------------------------------------

	/**
	 * Discovers whether the server declares the general-subsetting conformance class,
	 * collects additional dimensions with a parseable interval, and resolves the map
	 * endpoint URL for each qualifying collection.
	 */
	@BeforeClass
	public void discoverTargets() {
		System.out.println("[A.34] IUT: " + rootUri);
		generalSubsettingSupported = isGeneralSubsettingDeclared();
		if (!generalSubsettingSupported) {
			System.out.println("[A.34] Server does not declare " + GENERAL_SUBSETTING_CONF_URI + " — will skip.");
			return;
		}
		collectTargets();
		System.out.println("[A.34] Found " + targets.size() + " testable target(s).");
	}

	// -------------------------------------------------------------------------
	// Tests
	// -------------------------------------------------------------------------

	/**
	 * <pre>
	 * Abstract test A.34 - Case 1
	 *
	 * Identifier: /conf/general-subsetting/subset
	 * Requirement: Requirement 34: /req/general-subsetting/subset
	 * Test purpose: Verify that a subset=dim(low:high) request within the declared interval
	 *               returns HTTP 200 and Content-Type: image/*
	 *
	 * Given: a map resource declaring the general-subsetting conformance class
	 * When: sending GET {mapUrl}?subset=dim(low:high) with low and high within interval
	 * Then:
	 * - assert HTTP 200
	 * - assert Content-Type starts with "image/"
	 * </pre>
	 */
	@Test(description = "A.34 Req 34/A (valid range): subset=dim(low:high) within the declared interval "
			+ "returns HTTP 200 and Content-Type: image/* " + "(Requirement /req/general-subsetting/subset).")
	public void verifyValidRangeSubset() {
		skipIfNotApplicable();
		List<String> errors = new ArrayList<>();
		for (CollectionTarget t : targets) {
			String subset = validRangeSubset(t);
			try {
				io.restassured.response.Response response = initWithAuth().accept("image/*")
					.queryParam("subset", subset)
					.when()
					.get(t.mapUrl);
				int status = response.getStatusCode();
				if (status != 200) {
					errors.add("[" + t.collectionId + " / " + t.dimensionName + "] subset=" + subset
							+ " expected 200 but got " + status);
				}
				else {
					String ct = response.getContentType();
					if (ct == null || !ct.startsWith("image/")) {
						errors.add("[" + t.collectionId + " / " + t.dimensionName + "] subset=" + subset
								+ " expected image/* Content-Type but got: " + ct);
					}
				}
			}
			catch (Exception e) {
				errors.add("[" + t.collectionId + " / " + t.dimensionName + "] subset=" + subset + " exception: "
						+ e.getMessage());
			}
		}
		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.34 verifyValidRangeSubset failed:\n" + String.join("\n", errors));
		}
	}

	/**
	 * <pre>
	 * Abstract test A.34 - Case 2
	 *
	 * Identifier: /conf/general-subsetting/subset
	 * Requirement: Requirement 34: /req/general-subsetting/subset
	 * Test purpose: Verify that a subset=dim(value) point request within the declared
	 *               interval returns HTTP 200 and Content-Type: image/*
	 *
	 * Given: a map resource declaring the general-subsetting conformance class
	 * When: sending GET {mapUrl}?subset=dim(value) with value at the midpoint of the interval
	 * Then:
	 * - assert HTTP 200
	 * - assert Content-Type starts with "image/"
	 * </pre>
	 */
	@Test(description = "A.34 Req 34/A (valid point): subset=dim(value) at the midpoint of the declared interval "
			+ "returns HTTP 200 and Content-Type: image/* " + "(Requirement /req/general-subsetting/subset).")
	public void verifyValidPointSubset() {
		skipIfNotApplicable();
		List<String> errors = new ArrayList<>();
		for (CollectionTarget t : targets) {
			String subset = validPointSubset(t);
			try {
				io.restassured.response.Response response = initWithAuth().accept("image/*")
					.queryParam("subset", subset)
					.when()
					.get(t.mapUrl);
				int status = response.getStatusCode();
				if (status != 200) {
					errors.add("[" + t.collectionId + " / " + t.dimensionName + "] subset=" + subset
							+ " expected 200 but got " + status);
				}
				else {
					String ct = response.getContentType();
					if (ct == null || !ct.startsWith("image/")) {
						errors.add("[" + t.collectionId + " / " + t.dimensionName + "] subset=" + subset
								+ " expected image/* Content-Type but got: " + ct);
					}
				}
			}
			catch (Exception e) {
				errors.add("[" + t.collectionId + " / " + t.dimensionName + "] subset=" + subset + " exception: "
						+ e.getMessage());
			}
		}
		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.34 verifyValidPointSubset failed:\n" + String.join("\n", errors));
		}
	}

	/**
	 * <pre>
	 * Abstract test A.34 - Case 3
	 *
	 * Identifier: /conf/general-subsetting/subset
	 * Requirement: Requirement 34: /req/general-subsetting/subset
	 * Test purpose: Verify that a subset parameter using an unknown dimension name returns
	 *               HTTP 400
	 *
	 * Given: a map resource declaring the general-subsetting conformance class
	 * When: sending GET {mapUrl}?subset=nonexistent_dim_xyz(0:100)
	 * Then:
	 * - assert HTTP 400
	 * </pre>
	 */
	@Test(description = "A.34 Req 34/A (invalid dimension): subset=nonexistent_dim_xyz(0:100) " + "returns HTTP 400 "
			+ "(Requirement /req/general-subsetting/subset).")
	public void verifyInvalidDimensionName() {
		skipIfNotApplicable();
		List<String> errors = new ArrayList<>();
		String subset = "nonexistent_dim_xyz(0:100)";
		for (CollectionTarget t : targets) {
			try {
				io.restassured.response.Response response = initWithAuth().accept("image/*")
					.queryParam("subset", subset)
					.when()
					.get(t.mapUrl);
				int status = response.getStatusCode();
				if (status != 400) {
					errors.add("[" + t.collectionId + " / " + t.dimensionName + "] subset=" + subset
							+ " expected 400 but got " + status);
				}
			}
			catch (Exception e) {
				errors.add("[" + t.collectionId + " / " + t.dimensionName + "] subset=" + subset + " exception: "
						+ e.getMessage());
			}
		}
		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.34 verifyInvalidDimensionName failed:\n" + String.join("\n", errors));
		}
	}

	/**
	 * <pre>
	 * Abstract test A.34 - Case 4
	 *
	 * Identifier: /conf/general-subsetting/subset
	 * Requirement: Requirement 34: /req/general-subsetting/subset
	 * Test purpose: Verify that a subset range entirely outside the declared interval returns
	 *               HTTP 400 or HTTP 404
	 *
	 * Given: a map resource declaring the general-subsetting conformance class
	 * When: sending GET {mapUrl}?subset=dim(lo:hi) where lo and hi are entirely below
	 *       the declared interval
	 * Then:
	 * - assert HTTP 400 or HTTP 404 (a 200 with an image is a conformance failure)
	 * </pre>
	 */
	@Test(description = "A.34 Req 34/A (out-of-range): subset=dim(lo:hi) entirely outside the declared interval "
			+ "returns HTTP 400 or HTTP 404 " + "(Requirement /req/general-subsetting/subset).")
	public void verifyOutOfRangeSubset() {
		skipIfNotApplicable();
		List<String> errors = new ArrayList<>();
		for (CollectionTarget t : targets) {
			String subset = outOfRangeSubset(t);
			try {
				io.restassured.response.Response response = initWithAuth().accept("image/*")
					.queryParam("subset", subset)
					.when()
					.get(t.mapUrl);
				int status = response.getStatusCode();
				if (status != 400 && status != 404) {
					errors.add("[" + t.collectionId + " / " + t.dimensionName + "] subset=" + subset
							+ " expected 400 or 404 but got " + status);
				}
			}
			catch (Exception e) {
				errors.add("[" + t.collectionId + " / " + t.dimensionName + "] subset=" + subset + " exception: "
						+ e.getMessage());
			}
		}
		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.34 verifyOutOfRangeSubset failed:\n" + String.join("\n", errors));
		}
	}

	// -------------------------------------------------------------------------
	// Discovery helpers
	// -------------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	private boolean isGeneralSubsettingDeclared() {
		try {
			Map<String, Object> conformance = fetchJson(baseUrl() + "/conformance?f=json");
			if (conformance == null) {
				return false;
			}
			List<String> conformsTo = (List<String>) conformance.get("conformsTo");
			return conformsTo != null && conformsTo.stream()
				.anyMatch(u -> u != null && normalizeScheme(u).equals(normalizeScheme(GENERAL_SUBSETTING_CONF_URI)));
		}
		catch (Exception e) {
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	private void collectTargets() {
		try {
			Map<String, Object> collectionsDoc = fetchJson(baseUrl() + "/collections?f=json");
			if (collectionsDoc == null) {
				return;
			}
			List<Map<String, Object>> collections = (List<Map<String, Object>>) collectionsDoc.get("collections");
			if (collections == null) {
				return;
			}
			for (Map<String, Object> collection : collections) {
				String collectionId = collection.get("id") != null ? collection.get("id").toString() : "(unknown)";
				Map<String, Object> extent = castMap(collection.get("extent"));
				if (extent == null) {
					continue;
				}
				for (Map.Entry<String, Object> entry : extent.entrySet()) {
					String key = entry.getKey();
					if (STANDARD_EXTENT_KEYS.contains(key) || !(entry.getValue() instanceof Map)) {
						continue;
					}
					Map<String, Object> dimMap = castMap(entry.getValue());
					double[] interval = parseInterval(dimMap);
					if (interval == null) {
						continue;
					}
					String mapUrl = resolveMapUrl(collectionId);
					if (mapUrl == null) {
						System.out.println(
								"[A.34] No map link for collection=" + collectionId + " — skipping dimension " + key);
						continue;
					}
					targets.add(new CollectionTarget(collectionId, key, interval[0], interval[1], mapUrl));
					System.out
						.println("[A.34] Target: collection=" + collectionId + " dimension=" + key + " map=" + mapUrl);
				}
			}
		}
		catch (Exception e) {
			System.err.println("[A.34] Error collecting targets: " + e.getMessage());
		}
	}

	/** Parses {@code interval[0][0]} and {@code interval[0][1]} from a dimension Map. */
	private double[] parseInterval(Map<String, Object> dimMap) {
		try {
			Object intervalVal = dimMap.get("interval");
			if (!(intervalVal instanceof List)) {
				return null;
			}
			List<?> outer = (List<?>) intervalVal;
			if (outer.isEmpty() || !(outer.get(0) instanceof List)) {
				return null;
			}
			List<?> inner = (List<?>) outer.get(0);
			if (inner.size() < 2 || inner.get(0) == null || inner.get(1) == null) {
				return null;
			}
			double lo = ((Number) inner.get(0)).doubleValue();
			double hi = ((Number) inner.get(1)).doubleValue();
			return new double[] { lo, hi };
		}
		catch (Exception e) {
			return null;
		}
	}

	/**
	 * Fetches the individual collection description and returns the resolved href of its
	 * map link, or {@code null} if not found.
	 */
	@SuppressWarnings("unchecked")
	private String resolveMapUrl(String collectionId) {
		try {
			Map<String, Object> col = fetchJson(baseUrl() + "/collections/" + collectionId + "?f=json");
			if (col == null) {
				return null;
			}
			List<Map<String, Object>> links = (List<Map<String, Object>>) col.get("links");
			return findMapLinkHref(links);
		}
		catch (Exception e) {
			return null;
		}
	}

	private String findMapLinkHref(List<Map<String, Object>> links) {
		if (links == null) {
			return null;
		}
		for (Map<String, Object> link : links) {
			Object relObj = link.get("rel");
			if (!(relObj instanceof String)) {
				continue;
			}
			String rel = (String) relObj;
			if (MAP_REL_HTTPS.equals(rel) || MAP_REL_HTTP.equals(rel) || MAP_REL_COMPACT.equals(rel)) {
				Object hrefObj = link.get("href");
				if (hrefObj instanceof String && !((String) hrefObj).isEmpty()) {
					return rootUri.resolve((String) hrefObj).toString();
				}
			}
		}
		return null;
	}

	// -------------------------------------------------------------------------
	// Subset value helpers
	// -------------------------------------------------------------------------

	private String fmtVal(double v) {
		return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
	}

	private String validRangeSubset(CollectionTarget t) {
		double lo = t.intervalLow + 0.3 * (t.intervalHigh - t.intervalLow);
		double hi = t.intervalLow + 0.7 * (t.intervalHigh - t.intervalLow);
		return t.dimensionName + "(" + fmtVal(lo) + ":" + fmtVal(hi) + ")";
	}

	private String validPointSubset(CollectionTarget t) {
		double mid = (t.intervalLow + t.intervalHigh) / 2.0;
		return t.dimensionName + "(" + fmtVal(mid) + ")";
	}

	private String outOfRangeSubset(CollectionTarget t) {
		double width = t.intervalHigh - t.intervalLow;
		double lo = t.intervalLow - 2 * width;
		double hi = t.intervalLow - width;
		return t.dimensionName + "(" + fmtVal(lo) + ":" + fmtVal(hi) + ")";
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

	/** Returns a {@link RequestSpecification} with Basic auth applied if configured. */
	private RequestSpecification initWithAuth() {
		RequestSpecification spec = init();
		if (basicAuthHeader != null) {
			spec = spec.header("Authorization", basicAuthHeader);
		}
		return spec;
	}

	// -------------------------------------------------------------------------
	// Utility helpers
	// -------------------------------------------------------------------------

	private String baseUrl() {
		String url = rootUri.toString();
		return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
	}

	private String normalizeScheme(String uri) {
		if (uri != null && uri.startsWith("https://")) {
			return "http://" + uri.substring("https://".length());
		}
		return uri;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> castMap(Object obj) {
		return obj instanceof Map ? (Map<String, Object>) obj : null;
	}

	private void skipIfNotApplicable() {
		if (!generalSubsettingSupported) {
			throw new SkipException("Server does not declare " + GENERAL_SUBSETTING_CONF_URI + ". Skipping A.34.");
		}
		if (targets.isEmpty()) {
			throw new SkipException("No collections with additional dimensions and a map link found at " + rootUri
					+ ". Skipping A.34.");
		}
	}

	// -------------------------------------------------------------------------
	// Inner class
	// -------------------------------------------------------------------------

	private static class CollectionTarget {

		final String collectionId;

		final String dimensionName;

		final double intervalLow;

		final double intervalHigh;

		final String mapUrl;

		CollectionTarget(String collectionId, String dimensionName, double intervalLow, double intervalHigh,
				String mapUrl) {
			this.collectionId = collectionId;
			this.dimensionName = dimensionName;
			this.intervalLow = intervalLow;
			this.intervalHigh = intervalHigh;
			this.mapUrl = mapUrl;
		}

	}

}
