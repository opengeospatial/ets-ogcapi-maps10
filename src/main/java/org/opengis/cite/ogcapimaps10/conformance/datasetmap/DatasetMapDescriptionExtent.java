package org.opengis.cite.ogcapimaps10.conformance.datasetmap;

import static io.restassured.http.Method.GET;

import java.util.Map;

import org.opengis.cite.ogcapimaps10.conformance.CommonFixture;
import org.testng.Assert;
import org.testng.annotations.Test;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

/**
 * A.14.2. Abstract Test for Requirement dataset description extent.
 *
 * <pre>
 * Abstract test A.50
 *
 * Identifier: /conf/dataset-map/desc-extent
 * Requirement: Requirement 50: /req/dataset-map/desc-extent
 * Test purpose: Verify that the implementation describes the extent of the dataset
 * correctly from the landing page.
 * </pre>
 */
public class DatasetMapDescriptionExtent extends CommonFixture {

	/**
	 * <pre>
	 * Abstract test A.50
	 *
	 * Identifier: /conf/dataset-map/desc-extent
	 * Requirement: Requirement 50: /req/dataset-map/desc-extent
	 * Test purpose: Verify that the landing page extent provides the CRS of the
	 *               dataset extent.
	 * </pre>
	 */
	@Test(description = "Implements A.14.2. Abstract Test for Requirement dataset description extent "
			+ "(Requirement /req/dataset-map/desc-extent)")
	public void verifyLandingPageExtentCrs() {
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

		Object extentObj = json.get("extent");
		Assert.assertTrue(extentObj instanceof Map, "Landing page JSON response must include an 'extent' object.");

		Map<String, Object> extent = (Map<String, Object>) extentObj;
		Object spatialObj = extent.get("spatial");
		Assert.assertTrue(spatialObj instanceof Map,
				"Landing page 'extent' object must include a 'spatial' object following the collection extent schema.");

		Map<String, Object> spatial = (Map<String, Object>) spatialObj;
		Object crsObj = spatial.get("crs");
		Assert.assertTrue(crsObj instanceof String,
				"Landing page 'extent.spatial' object must include a string 'crs' property.");

		String crs = ((String) crsObj).trim();
		Assert.assertFalse(crs.isEmpty(), "Landing page 'extent.spatial.crs' value must not be empty.");
	}

}
