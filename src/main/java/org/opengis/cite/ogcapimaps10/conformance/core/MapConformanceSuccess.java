package org.opengis.cite.ogcapimaps10.conformance.core;

import static io.restassured.http.ContentType.JSON;
import static io.restassured.http.Method.GET;
import static org.opengis.cite.ogcapimaps10.conformance.RequirementClass.CORE;
import static org.opengis.cite.ogcapimaps10.conformance.SuiteAttribute.REQUIREMENTCLASSES;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.opengis.cite.ogcapimaps10.conformance.CommonFixture;
import org.opengis.cite.ogcapimaps10.conformance.RequirementClass;
import org.testng.ITestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

/**
 * <pre>
 * Abstract test A.1.3
 *
 * Identifier: /conf/core/conformance-success
 * Requirement: Requirement 3: /req/core/conformance-success
 * Test purpose: For implementations having a mechanism to advertise conformance classes,
 * verify that it reports conformance to this Standard correctly.
 *
 * Test method:
 * Given: a conformance resource in a recognized format, such as the OGC API JSON /conformance resource
 * When: retrieving that resource from the API endpoint
 * Then: assert that the list of conformance classes includes at minimum:
 *   https://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/core
 * or the legacy ATS URI:
 *   https://www.opengis.net/spec/ogcapi-maps-1/1.0/req/core
 * </pre>
 */
public class MapConformanceSuccess extends CommonFixture {

	private static final String REQUIRED_CORE_URI = "https://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/core";

	private static final String LEGACY_REQUIRED_CORE_URI = "https://www.opengis.net/spec/ogcapi-maps-1/1.0/req/core";

	private List<RequirementClass> requirementClasses = new ArrayList<>();

	@AfterClass
	public void storeRequirementClassesInTestContext(ITestContext testContext) {
		testContext.getSuite().setAttribute(REQUIREMENTCLASSES.getName(), this.requirementClasses);
	}

	@Test(description = "Implements A.1.3. Abstract Test for Requirement Map Conformance Success (Requirement /req/core/conformance-success)")
	public void verifyMapConformanceSuccess() {
		String conformanceUrl = buildConformanceUrl();
		Response response = init().baseUri(conformanceUrl).accept(JSON).when().request(GET);
		response.then().statusCode(200);

		this.requirementClasses = parseAndValidateRequirementClasses(response.jsonPath());
		assertTrue(this.requirementClasses.contains(CORE),
				"The required conformance class 'conf/core' or legacy 'req/core' is not declared.");
	}

	List<RequirementClass> parseAndValidateRequirementClasses(JsonPath jsonPath) {
		Object conformsToObj = jsonPath.get("conformsTo");
		assertNotNull(conformsToObj, "Missing member 'conformsTo'.");
		if (!(conformsToObj instanceof List<?>)) {
			throw new AssertionError("Member 'conformsTo' is not an array.");
		}

		List<RequirementClass> parsedRequirementClasses = new ArrayList<>();
		for (Object conformsToEntry : (List<?>) conformsToObj) {
			if (!(conformsToEntry instanceof String)) {
				throw new AssertionError(
						"At least one element array 'conformsTo' is not a string value (" + conformsToEntry + ")");
			}
			String conformanceClass = ((String) conformsToEntry).trim();
			RequirementClass requirementClass = requirementClassByConformanceClass(conformanceClass);
			if (requirementClass != null && !parsedRequirementClasses.contains(requirementClass)) {
				parsedRequirementClasses.add(requirementClass);
			}
		}
		return parsedRequirementClasses;
	}

	private RequirementClass requirementClassByConformanceClass(String conformanceClass) {
		if (REQUIRED_CORE_URI.equals(conformanceClass) || LEGACY_REQUIRED_CORE_URI.equals(conformanceClass)) {
			return CORE;
		}
		return RequirementClass.byConformanceClass(conformanceClass);
	}

	private String buildConformanceUrl() {
		String rootUriString = rootUri.toString();
		if (rootUriString.endsWith("/")) {
			rootUriString = rootUriString.substring(0, rootUriString.length() - 1);
		}
		return rootUriString + "/conformance";
	}

}
