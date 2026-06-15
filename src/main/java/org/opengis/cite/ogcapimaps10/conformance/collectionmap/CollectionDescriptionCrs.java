package org.opengis.cite.ogcapimaps10.conformance.collectionmap;

import static io.restassured.http.Method.GET;
import static org.opengis.cite.ogcapimaps10.EtsAssert.assertValidCrsIdentifier;
import static org.opengis.cite.ogcapimaps10.OgcApiMaps10.DEFAULT_CRS_CODE;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.opengis.cite.ogcapimaps10.conformance.CommonDataFixture;
import org.opengis.cite.ogcapimaps10.conformance.crs.query.crs.CoordinateSystem;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

/**
 * A.13.2. Abstract Test for Requirement collection description CRS.
 *
 */
public class CollectionDescriptionCrs extends CommonDataFixture {

	private static final String DEFAULT_CRS_CODE_HTTPS = "https://www.opengis.net/def/crs/OGC/1.3/CRS84";

	private static final Pattern SAFE_CURIE = Pattern.compile("\\[[A-Za-z][A-Za-z0-9_.-]*:[^\\s\\[\\]]+\\]");

	/**
	 * <pre>
	 * Abstract test A.47
	 *
	 * Identifier: /conf/collection-map/desc-crs
	 * Requirement: Requirement 47: /req/collection-map/desc-crs
	 * Test purpose: Verify that the implementation describes the supported CRS correctly in its collection description resources
	 *
	 * Test Method:
	 * Given: an API implementation conforming to OGC API-Common-Part 2: Geospatial Data “Collections” conformance class
	 * When: retrieving the JSON representation of the description for that collection
	 * Then:
	 * - assert that the crs property contains URIs or safe CURIEs for the CRSs supported by the server for that collection,
	 * - assert that a non-CRS84 native CRS is identified by the storageCrs property,
	 * - assert that a non-CRS84 storageCrs is accompanied by extent.spatial.storageCrsBbox.
	 * </pre>
	 */
	@Test(description = "Implements A.13.2. Abstract Test for Requirement collection description CRS "
			+ "(Requirement /req/collection-map/desc-crs)")
	public void verifyCollectionDescriptionCrs() {
		Response collectionsResponse = init().baseUri(rootUri.toString()).accept("application/json").when().request(GET, collectionResource("collections"));
		assertSuccessfulJsonResponse(collectionsResponse, "Collections resource");

		List<Map<String, Object>> collections = collectionsResponse.jsonPath().getList("collections");
		Assert.assertNotNull(collections, "Collections JSON response must include a 'collections' array.");
		if (collections.isEmpty()) {
			throw new SkipException("No collections are available to test Requirement /req/collection-map/desc-crs.");
		}

		int limit = Math.min(noOfCollections, collections.size());
		for (int index = 0; index < limit; index++) {
			String collectionId = valueAsString(collections.get(index).get("id"));
			Assert.assertNotNull(collectionId, "Collection at index " + index + " must include an 'id' value.");
			Assert.assertFalse(collectionId.isEmpty(),
					"Collection at index " + index + " must include a non-empty 'id' value.");

			Response descriptionResponse = init().accept("application/json")
				.when()
				.get(collectionResource("collections/{collectionId}"), collectionId);
			String context = "Collection description for '" + collectionId + "'";
			assertSuccessfulJsonResponse(descriptionResponse, context);
			verifyCrsMetadata(descriptionResponse.jsonPath(), collectionId);
		}
	}

	private void verifyCrsMetadata(JsonPath collectionDescription, String collectionId) {
		List<Object> crs = collectionDescription.getList("crs");
		Assert.assertNotNull(crs, "Collection description for '" + collectionId + "' must include a 'crs' array.");
		Assert.assertFalse(crs.isEmpty(),
				"Collection description for '" + collectionId + "' must include a non-empty 'crs' array.");
		for (Object crsValue : crs) {
			assertValidUriOrSafeCurie(crsValue, collectionId, "crs");
		}

		Object storageCrsValue = collectionDescription.get("storageCrs");
		if (storageCrsValue == null) {
			return;
		}
		assertValidUriOrSafeCurie(storageCrsValue, collectionId, "storageCrs");

		String storageCrs = valueAsString(storageCrsValue);
		if (isCrs84(storageCrs)) {
			return;
		}

		List<Object> storageCrsBbox = collectionDescription.getList("extent.spatial.storageCrsBbox");
		Assert.assertNotNull(storageCrsBbox, "Collection description for '" + collectionId
				+ "' must include 'extent.spatial.storageCrsBbox' when 'storageCrs' is not CRS84.");
		assertValidBboxArray(storageCrsBbox, collectionId);
	}

	private static void assertValidUriOrSafeCurie(Object value, String collectionId, String propertyName) {
		String crsValue = valueAsString(value);
		String message = "Collection description for '" + collectionId + "' property '" + propertyName
				+ "' contains an invalid CRS identifier: '" + value + "'.";
		Assert.assertNotNull(crsValue, message);
		Assert.assertFalse(crsValue.isEmpty(), message);
		if (SAFE_CURIE.matcher(crsValue).matches()) {
			return;
		}
		assertValidCrsIdentifier(new CoordinateSystem(crsValue), message);
	}

	private static void assertValidBboxArray(List<Object> bboxArray, String collectionId) {
		String propertyPath = "extent.spatial.storageCrsBbox";
		Assert.assertFalse(bboxArray.isEmpty(),
				"Collection description for '" + collectionId + "' property '" + propertyPath + "' must not be empty.");

		Integer dimension = null;
		for (Object bboxValue : bboxArray) {
			Assert.assertTrue(bboxValue instanceof List, "Collection description for '" + collectionId + "' property '"
					+ propertyPath + "' must follow the CRS84 bbox schema: an array of bbox arrays.");
			List<?> bbox = (List<?>) bboxValue;
			Assert.assertTrue(bbox.size() == 4 || bbox.size() == 6, "Collection description for '" + collectionId
					+ "' property '" + propertyPath + "' bbox values must contain 4 or 6 numeric ordinates.");
			if (dimension == null) {
				dimension = bbox.size();
			}
			else {
				Assert.assertEquals(bbox.size(), dimension.intValue(), "Collection description for '" + collectionId
						+ "' property '" + propertyPath + "' must use the same dimension for every bbox.");
			}
			assertValidBbox(bbox, collectionId, propertyPath);
		}
	}

	private static void assertValidBbox(List<?> bbox, String collectionId, String propertyPath) {
		for (Object coordinate : bbox) {
			Assert.assertTrue(coordinate instanceof Number, "Collection description for '" + collectionId
					+ "' property '" + propertyPath + "' bbox values must be numeric.");
		}

		double minX = ((Number) bbox.get(0)).doubleValue();
		double minY = ((Number) bbox.get(1)).doubleValue();
		double maxX = ((Number) bbox.get(bbox.size() == 4 ? 2 : 3)).doubleValue();
		double maxY = ((Number) bbox.get(bbox.size() == 4 ? 3 : 4)).doubleValue();
		Assert.assertTrue(minX <= maxX, "Collection description for '" + collectionId + "' property '" + propertyPath
				+ "' bbox minimum x must be less than or equal to maximum x.");
		Assert.assertTrue(minY <= maxY, "Collection description for '" + collectionId + "' property '" + propertyPath
				+ "' bbox minimum y must be less than or equal to maximum y.");
		if (bbox.size() == 6) {
			double minZ = ((Number) bbox.get(2)).doubleValue();
			double maxZ = ((Number) bbox.get(5)).doubleValue();
			Assert.assertTrue(minZ <= maxZ, "Collection description for '" + collectionId + "' property '"
					+ propertyPath + "' bbox minimum z must be less than or equal to maximum z.");
		}
	}

	private void assertSuccessfulJsonResponse(Response response, String resourceName) {
		Assert.assertTrue(response.statusCode() >= 200 && response.statusCode() < 300, resourceName
				+ " must return a successful HTTP status code, but returned " + response.statusCode() + ".");
		try {
			response.jsonPath();
		}
		catch (RuntimeException e) {
			Assert.fail(resourceName + " response must be valid JSON.", e);
		}
	}

	private String collectionResource(String relativePath) {
		String base = rootUri.toString();
		if (!base.endsWith("/")) {
			base = base + "/";
		}
		return base + relativePath;
	}

	private static boolean isCrs84(String value) {
		return DEFAULT_CRS_CODE.equals(value) || DEFAULT_CRS_CODE_HTTPS.equals(value);
	}

	private static String valueAsString(Object value) {
		return value instanceof String ? (String) value : null;
	}

}
