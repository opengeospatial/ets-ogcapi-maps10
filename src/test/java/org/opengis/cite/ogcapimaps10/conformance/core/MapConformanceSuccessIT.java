package org.opengis.cite.ogcapimaps10.conformance.core;

import static org.testng.Assert.assertTrue;

import java.util.List;

import org.opengis.cite.ogcapimaps10.conformance.RequirementClass;
import org.testng.annotations.Test;

import io.restassured.path.json.JsonPath;

/**
 * Tests for MapConformanceSuccess.
 */
public class MapConformanceSuccessIT {

	@Test
	public void parsesCoreConformanceClass() {
		MapConformanceSuccess conformanceTest = new MapConformanceSuccess();
		JsonPath jsonPath = new JsonPath(
				"{\"conformsTo\":[\"https://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/core\"]}");

		List<RequirementClass> requirementClasses = conformanceTest.parseAndValidateRequirementClasses(jsonPath);

		assertTrue(requirementClasses.contains(RequirementClass.CORE));
	}

	@Test
	public void parsesLegacyCoreRequirementUri() {
		MapConformanceSuccess conformanceTest = new MapConformanceSuccess();
		JsonPath jsonPath = new JsonPath(
				"{\"conformsTo\":[\"https://www.opengis.net/spec/ogcapi-maps-1/1.0/req/core\"]}");

		List<RequirementClass> requirementClasses = conformanceTest.parseAndValidateRequirementClasses(jsonPath);

		assertTrue(requirementClasses.contains(RequirementClass.CORE));
	}

	@Test(expectedExceptions = AssertionError.class)
	public void failsIfConformsToIsMissing() {
		MapConformanceSuccess conformanceTest = new MapConformanceSuccess();
		conformanceTest.parseAndValidateRequirementClasses(new JsonPath("{}"));
	}

	@Test(expectedExceptions = AssertionError.class)
	public void failsIfConformsToEntryIsNotString() {
		MapConformanceSuccess conformanceTest = new MapConformanceSuccess();
		conformanceTest.parseAndValidateRequirementClasses(new JsonPath("{\"conformsTo\":[123]}"));
	}

}
