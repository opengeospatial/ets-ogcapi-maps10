package org.opengis.cite.ogcapimaps10.conformance.datasetmap;

import static io.restassured.http.Method.GET;
import static org.opengis.cite.ogcapimaps10.EtsAssert.assertValidCrsIdentifier;
import static org.opengis.cite.ogcapimaps10.OgcApiMaps10.DEFAULT_CRS_CODE;

import java.util.List;
import java.util.regex.Pattern;

import org.opengis.cite.ogcapimaps10.conformance.CommonFixture;
import org.opengis.cite.ogcapimaps10.conformance.crs.query.crs.CoordinateSystem;
import org.testng.Assert;
import org.testng.annotations.Test;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

/**
 * A.14.3. Abstract Test for Requirement dataset description CRS.
 *
 * <pre>
 * Abstract test A.51
 *
 * Identifier: /conf/dataset-map/desc-crs
 * Requirement: Requirement 51: /req/dataset-map/desc-crs
 * Test purpose: Verify that the implementation describes the supported CRS correctly
 * in its landing page resource.
 * </pre>
 */
public class DatasetMapDescCrs extends CommonFixture {

	private static final String DEFAULT_CRS_CODE_HTTPS = "https://www.opengis.net/def/crs/OGC/1.3/CRS84";

	private static final Pattern SAFE_CURIE = Pattern.compile("\\[[A-Za-z][A-Za-z0-9_.-]*:[^\\s\\[\\]]+\\]");

	/**
	 * <pre>
	 * Abstract test A.51
	 *
	 * Identifier: /conf/dataset-map/desc-crs
	 * Requirement: Requirement 51: /req/dataset-map/desc-crs
	 * Test purpose: Verify that CRS metadata in the landing page describes the
	 *               dataset-level CRS support.
	 * </pre>
	 */
	@Test(description = "Implements A.14.3. Abstract Test for Requirement dataset description CRS "
			+ "(Requirement /req/dataset-map/desc-crs)")
	public void verifyDatasetDescriptionCrs() {
		Response response = init().baseUri(rootUri.toString()).accept("application/json").when().request(GET);
		Assert.assertTrue(response.statusCode() >= 200 && response.statusCode() < 300,
				"Landing page JSON response must have a successful HTTP status code.");

		JsonPath json;
		try {
			json = response.jsonPath();
		}
		catch (RuntimeException e) {
			Assert.fail("Landing page response must be valid JSON.", e);
			return;
		}

		List<Object> crs = json.getList("crs");
		Assert.assertNotNull(crs, "Landing page JSON response must include a dataset-level 'crs' array.");
		Assert.assertFalse(crs.isEmpty(), "Landing page JSON response 'crs' array must not be empty.");
		for (Object crsValue : crs) {
			assertValidUriOrSafeCurie(crsValue, "Landing page 'crs' array contains an invalid CRS identifier: '%s'.");
		}

		String storageCrs = valueAsString(json.get("storageCrs"));
		if (storageCrs == null) {
			return;
		}
		assertValidUriOrSafeCurie(storageCrs, "Landing page 'storageCrs' contains an invalid CRS identifier: '%s'.");
		if (isCrs84(storageCrs)) {
			return;
		}

		List<Object> storageCrsBbox = json.getList("extent.spatial.storageCrsBbox");
		Assert.assertNotNull(storageCrsBbox,
				"Landing page must include 'extent.spatial.storageCrsBbox' when 'storageCrs' is not CRS84.");
		assertValidBboxArray(storageCrsBbox, "extent.spatial.storageCrsBbox");
	}

	private static void assertValidUriOrSafeCurie(Object value, String messageTemplate) {
		String crsValue = valueAsString(value);
		Assert.assertNotNull(crsValue, String.format(messageTemplate, value));
		Assert.assertFalse(crsValue.isEmpty(), String.format(messageTemplate, crsValue));
		if (isSafeCurie(crsValue)) {
			return;
		}
		assertValidCrsIdentifier(new CoordinateSystem(crsValue), String.format(messageTemplate, crsValue));
	}

	private static void assertValidBboxArray(List<Object> bboxArray, String propertyPath) {
		Assert.assertFalse(bboxArray.isEmpty(), "Landing page '" + propertyPath + "' array must not be empty.");
		for (Object bbox : bboxArray) {
			Assert.assertTrue(bbox instanceof List,
					"Landing page '" + propertyPath + "' must follow the CRS84 bbox schema: array of bbox arrays.");
			assertValidBbox((List<?>) bbox, propertyPath);
		}
	}

	private static void assertValidBbox(List<?> bbox, String propertyPath) {
		Assert.assertTrue(bbox.size() == 4 || bbox.size() == 6,
				"Landing page '" + propertyPath + "' bbox values must contain either 4 or 6 numeric ordinates.");
		for (Object coordinate : bbox) {
			Assert.assertTrue(coordinate instanceof Number,
					"Landing page '" + propertyPath + "' bbox values must be numeric.");
		}
		double minX = ((Number) bbox.get(0)).doubleValue();
		double minY = ((Number) bbox.get(1)).doubleValue();
		double maxX = ((Number) bbox.get(bbox.size() == 4 ? 2 : 3)).doubleValue();
		double maxY = ((Number) bbox.get(bbox.size() == 4 ? 3 : 4)).doubleValue();
		Assert.assertTrue(minX <= maxX,
				"Landing page '" + propertyPath + "' bbox minimum x must be less than or equal to maximum x.");
		Assert.assertTrue(minY <= maxY,
				"Landing page '" + propertyPath + "' bbox minimum y must be less than or equal to maximum y.");
		if (bbox.size() == 6) {
			double minZ = ((Number) bbox.get(2)).doubleValue();
			double maxZ = ((Number) bbox.get(5)).doubleValue();
			Assert.assertTrue(minZ <= maxZ,
					"Landing page '" + propertyPath + "' bbox minimum z must be less than or equal to maximum z.");
		}
	}

	private static boolean isSafeCurie(String value) {
		return SAFE_CURIE.matcher(value).matches();
	}

	private static boolean isCrs84(String value) {
		return DEFAULT_CRS_CODE.equals(value) || DEFAULT_CRS_CODE_HTTPS.equals(value);
	}

	private static String valueAsString(Object value) {
		return value instanceof String ? (String) value : null;
	}

}
