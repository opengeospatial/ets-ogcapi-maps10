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

/**
 * Implements Abstract Test A.33: /conf/general-subsetting/uniform-additional-dimensions
 *
 * <p>
 * Test Purpose: Verify that the implementation describes additional dimensions (beyond
 * {@code spatial} and {@code temporal}) in a uniform manner (Requirement 33:
 * /req/general-subsetting/uniform-additional-dimensions).
 *
 * <p>
 * Assertions verified:
 * <ul>
 * <li>[Req33/A-interval] Each additional dimension in a collection's {@code extent} has
 * an {@code interval} property using a double-nested array format
 * {@code [[low, high]]}.</li>
 * <li>[Req33/A-definition] Each additional dimension object has a {@code definition}
 * property holding a URI string that identifies its semantic meaning.</li>
 * <li>[Req33/A-uom] When a {@code uom} field is present on an additional dimension, its
 * value is a non-empty string (e.g., {@code "hPa"}, {@code "m"}).</li>
 * <li>[Req33/A-grid] When a {@code grid} field is present on an additional dimension, its
 * value is an object containing a {@code cellsCount} integer.</li>
 * </ul>
 */
public class UniformAdditionalDimensions extends CommonFixture {

	private static final String GENERAL_SUBSETTING_CONF_URI = "https://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/general-subsetting";

	private static final Set<String> STANDARD_EXTENT_KEYS = new HashSet<>(Arrays.asList("spatial", "temporal"));

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private boolean generalSubsettingSupported;

	/** All additional-dimension descriptors discovered across all collections. */
	private final List<AdditionalDimensionInfo> discoveredDimensions = new ArrayList<>();

	// -------------------------------------------------------------------------
	// Setup
	// -------------------------------------------------------------------------

	/**
	 * Discovers whether the server declares the general-subsetting conformance class and
	 * collects all additional dimensions found in collection extent descriptions.
	 */
	@BeforeClass
	public void discoverTargets() {
		System.out.println("[A.33] IUT: " + rootUri);
		generalSubsettingSupported = isGeneralSubsettingDeclared();
		if (!generalSubsettingSupported) {
			System.out.println("[A.33] Server does not declare " + GENERAL_SUBSETTING_CONF_URI + " — will skip.");
			return;
		}
		collectAdditionalDimensions();
		System.out
			.println("[A.33] Found " + discoveredDimensions.size() + " additional dimension(s) across collections.");
	}

	// -------------------------------------------------------------------------
	// Tests
	// -------------------------------------------------------------------------

	/**
	 * <pre>
	 * Abstract test A.33 - Case 1
	 *
	 * Identifier: /conf/general-subsetting/uniform-additional-dimensions
	 * Requirement: Requirement 33: /req/general-subsetting/uniform-additional-dimensions
	 * Test purpose: Verify that each additional dimension in a collection's extent has
	 *               an interval property with double-nested array format [[low, high]]
	 *
	 * Given: a map resource declaring the general-subsetting conformance class
	 * When: retrieving /collections and inspecting each collection's extent
	 * Then:
	 * - assert that each additional dimension object contains an "interval" key,
	 * - assert that the value is a list (outer array) whose first element is a
	 *   2-element list [low, high] (inner array) with both elements non-null.
	 * </pre>
	 */
	@Test(description = "A.33 Req 33/A (interval): each additional dimension in extent has "
			+ "an interval property using double-nested array format [[low, high]] "
			+ "(Requirement /req/general-subsetting/uniform-additional-dimensions).")
	public void verifyIntervalProperty() {
		if (!generalSubsettingSupported) {
			throw new SkipException("Server does not declare " + GENERAL_SUBSETTING_CONF_URI + ". Skipping A.33.");
		}
		if (discoveredDimensions.isEmpty()) {
			throw new SkipException(
					"No collections with additional dimensions found at " + rootUri + ". Skipping A.33 interval test.");
		}
		List<String> errors = new ArrayList<>();
		for (AdditionalDimensionInfo dim : discoveredDimensions) {
			Map<String, Object> dimMap = castMap(dim.dimensionObj);
			if (dimMap == null) {
				errors.add(
						"[" + dim.collectionId + " / " + dim.dimensionName + "] Dimension value is not a JSON object.");
				continue;
			}
			Object intervalVal = dimMap.get("interval");
			if (intervalVal == null) {
				errors.add("[" + dim.collectionId + " / " + dim.dimensionName
						+ "] Missing required \"interval\" property (Req 33/A).");
				continue;
			}
			if (!(intervalVal instanceof List)) {
				errors.add("[" + dim.collectionId + " / " + dim.dimensionName
						+ "] \"interval\" is not an array — expected [[low, high]] (Req 33/A).");
				continue;
			}
			List<?> outer = (List<?>) intervalVal;
			if (outer.isEmpty()) {
				errors.add("[" + dim.collectionId + " / " + dim.dimensionName
						+ "] \"interval\" array is empty — expected [[low, high]] (Req 33/A).");
				continue;
			}
			Object firstElem = outer.get(0);
			if (!(firstElem instanceof List)) {
				errors.add("[" + dim.collectionId + " / " + dim.dimensionName
						+ "] \"interval[0]\" is not an array — expected [low, high] (Req 33/A).");
				continue;
			}
			List<?> inner = (List<?>) firstElem;
			if (inner.size() < 2) {
				errors.add("[" + dim.collectionId + " / " + dim.dimensionName + "] \"interval[0]\" has " + inner.size()
						+ " element(s) — expected exactly 2 [low, high] (Req 33/A).");
			}
		}
		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.33 verifyIntervalProperty failed:\n" + String.join("\n", errors));
		}
	}

	/**
	 * <pre>
	 * Abstract test A.33 - Case 2
	 *
	 * Identifier: /conf/general-subsetting/uniform-additional-dimensions
	 * Requirement: Requirement 33: /req/general-subsetting/uniform-additional-dimensions
	 * Test purpose: Verify that each additional dimension in a collection's extent has
	 *               a definition property holding a URI string
	 *
	 * Given: a map resource declaring the general-subsetting conformance class
	 * When: retrieving /collections and inspecting each collection's extent
	 * Then:
	 * - assert that each additional dimension object contains a "definition" key,
	 * - assert that the value is a non-empty string (a URI identifying the semantic
	 *   meaning of the dimension).
	 * </pre>
	 */
	@Test(description = "A.33 Req 33/A (definition): each additional dimension in extent has "
			+ "a definition property holding a URI string "
			+ "(Requirement /req/general-subsetting/uniform-additional-dimensions).")
	public void verifyDefinitionProperty() {
		if (!generalSubsettingSupported) {
			throw new SkipException("Server does not declare " + GENERAL_SUBSETTING_CONF_URI + ". Skipping A.33.");
		}
		if (discoveredDimensions.isEmpty()) {
			throw new SkipException("No collections with additional dimensions found at " + rootUri
					+ ". Skipping A.33 definition test.");
		}
		List<String> errors = new ArrayList<>();
		for (AdditionalDimensionInfo dim : discoveredDimensions) {
			Map<String, Object> dimMap = castMap(dim.dimensionObj);
			if (dimMap == null) {
				continue;
			}
			Object defVal = dimMap.get("definition");
			if (defVal == null) {
				errors.add("[" + dim.collectionId + " / " + dim.dimensionName
						+ "] Missing required \"definition\" property — SHALL contain a URI (Req 33/A).");
				continue;
			}
			if (!(defVal instanceof String) || ((String) defVal).isEmpty()) {
				errors.add("[" + dim.collectionId + " / " + dim.dimensionName
						+ "] \"definition\" is not a non-empty string — expected a URI (Req 33/A). Got: " + defVal);
			}
		}
		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.33 verifyDefinitionProperty failed:\n" + String.join("\n", errors));
		}
	}

	/**
	 * <pre>
	 * Abstract test A.33 - Case 3
	 *
	 * Identifier: /conf/general-subsetting/uniform-additional-dimensions
	 * Requirement: Requirement 33: /req/general-subsetting/uniform-additional-dimensions
	 * Test purpose: When a uom field is present on an additional dimension, verify its
	 *               value is a non-empty string
	 *
	 * Given: a map resource declaring the general-subsetting conformance class
	 * When: retrieving /collections and finding additional dimensions that include a uom key
	 * Then:
	 * - assert that the uom value is a non-empty string (e.g., "hPa", "m").
	 * Skips if no additional dimension with a uom field is found.
	 * </pre>
	 */
	@Test(description = "A.33 Req 33/A (uom): when an additional dimension declares a uom field, "
			+ "the value is a non-empty string "
			+ "(Requirement /req/general-subsetting/uniform-additional-dimensions).")
	public void verifyUomProperty() {
		if (!generalSubsettingSupported) {
			throw new SkipException("Server does not declare " + GENERAL_SUBSETTING_CONF_URI + ". Skipping A.33.");
		}
		if (discoveredDimensions.isEmpty()) {
			throw new SkipException(
					"No collections with additional dimensions found at " + rootUri + ". Skipping A.33 uom test.");
		}
		List<AdditionalDimensionInfo> withUom = new ArrayList<>();
		for (AdditionalDimensionInfo dim : discoveredDimensions) {
			Map<String, Object> dimMap = castMap(dim.dimensionObj);
			if (dimMap != null && dimMap.containsKey("uom")) {
				withUom.add(dim);
			}
		}
		if (withUom.isEmpty()) {
			throw new SkipException(
					"No additional dimension with a \"uom\" field found — test not applicable. Skipping A.33 uom test.");
		}
		List<String> errors = new ArrayList<>();
		for (AdditionalDimensionInfo dim : withUom) {
			Map<String, Object> dimMap = castMap(dim.dimensionObj);
			Object uomVal = dimMap.get("uom");
			if (!(uomVal instanceof String) || ((String) uomVal).isEmpty()) {
				errors.add("[" + dim.collectionId + " / " + dim.dimensionName
						+ "] \"uom\" is present but is not a non-empty string. Got: " + uomVal);
			}
		}
		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.33 verifyUomProperty failed:\n" + String.join("\n", errors));
		}
	}

	/**
	 * <pre>
	 * Abstract test A.33 - Case 4
	 *
	 * Identifier: /conf/general-subsetting/uniform-additional-dimensions
	 * Requirement: Requirement 33: /req/general-subsetting/uniform-additional-dimensions
	 * Test purpose: When a grid field is present on an additional dimension, verify its
	 *               value is an object containing a cellsCount integer
	 *
	 * Given: a map resource declaring the general-subsetting conformance class
	 * When: retrieving /collections and finding additional dimensions that include a grid key
	 * Then:
	 * - assert that the grid value is a JSON object,
	 * - assert that the object contains a "cellsCount" key with an integer value >= 1,
	 * - assert that "coordinates", if present, is an array.
	 * Skips if no additional dimension with a grid field is found.
	 * </pre>
	 */
	@Test(description = "A.33 Req 33/A (grid): when an additional dimension declares a grid field, "
			+ "the value is an object containing a cellsCount integer "
			+ "(Requirement /req/general-subsetting/uniform-additional-dimensions).")
	public void verifyGridProperty() {
		if (!generalSubsettingSupported) {
			throw new SkipException("Server does not declare " + GENERAL_SUBSETTING_CONF_URI + ". Skipping A.33.");
		}
		if (discoveredDimensions.isEmpty()) {
			throw new SkipException(
					"No collections with additional dimensions found at " + rootUri + ". Skipping A.33 grid test.");
		}
		List<AdditionalDimensionInfo> withGrid = new ArrayList<>();
		for (AdditionalDimensionInfo dim : discoveredDimensions) {
			Map<String, Object> dimMap = castMap(dim.dimensionObj);
			if (dimMap != null && dimMap.containsKey("grid")) {
				withGrid.add(dim);
			}
		}
		if (withGrid.isEmpty()) {
			throw new SkipException(
					"No additional dimension with a \"grid\" field found — test not applicable. Skipping A.33 grid test.");
		}
		List<String> errors = new ArrayList<>();
		for (AdditionalDimensionInfo dim : withGrid) {
			Map<String, Object> dimMap = castMap(dim.dimensionObj);
			Object gridVal = dimMap.get("grid");
			Map<String, Object> gridMap = castMap(gridVal);
			if (gridMap == null) {
				errors.add("[" + dim.collectionId + " / " + dim.dimensionName
						+ "] \"grid\" is present but is not a JSON object. Got: "
						+ (gridVal != null ? gridVal.getClass().getSimpleName() : "null"));
				continue;
			}
			Object cellsCount = gridMap.get("cellsCount");
			if (cellsCount == null) {
				errors.add("[" + dim.collectionId + " / " + dim.dimensionName
						+ "] \"grid\" object is missing required \"cellsCount\" property.");
			}
			else if (!(cellsCount instanceof Number) || ((Number) cellsCount).intValue() < 1) {
				errors.add("[" + dim.collectionId + " / " + dim.dimensionName
						+ "] \"grid.cellsCount\" must be an integer >= 1. Got: " + cellsCount);
			}
			Object coords = gridMap.get("coordinates");
			if (coords != null && !(coords instanceof List)) {
				errors.add("[" + dim.collectionId + " / " + dim.dimensionName
						+ "] \"grid.coordinates\", when present, must be an array. Got: "
						+ coords.getClass().getSimpleName());
			}
		}
		clearMessages();
		if (!errors.isEmpty()) {
			throw new AssertionError("A.33 verifyGridProperty failed:\n" + String.join("\n", errors));
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
	private void collectAdditionalDimensions() {
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
					if (!STANDARD_EXTENT_KEYS.contains(key) && entry.getValue() instanceof Map) {
						discoveredDimensions.add(new AdditionalDimensionInfo(collectionId, key, entry.getValue()));
						System.out.println(
								"[A.33] Found additional dimension: collection=" + collectionId + " dimension=" + key);
					}
				}
			}
		}
		catch (Exception e) {
			System.err.println("[A.33] Error collecting additional dimensions: " + e.getMessage());
		}
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

	// -------------------------------------------------------------------------
	// Inner class
	// -------------------------------------------------------------------------

	private static class AdditionalDimensionInfo {

		final String collectionId;

		final String dimensionName;

		final Object dimensionObj;

		AdditionalDimensionInfo(String collectionId, String dimensionName, Object dimensionObj) {
			this.collectionId = collectionId;
			this.dimensionName = dimensionName;
			this.dimensionObj = dimensionObj;
		}

	}

}
