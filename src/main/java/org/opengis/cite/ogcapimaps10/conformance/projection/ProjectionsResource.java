package org.opengis.cite.ogcapimaps10.conformance.projection;

import static io.restassured.http.Method.GET;
import static org.opengis.cite.ogcapimaps10.conformance.RequirementClass.PROJECTION;
import static org.opengis.cite.ogcapimaps10.conformance.SuiteAttribute.REQUIREMENTCLASSES;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.opengis.cite.ogcapimaps10.conformance.CommonFixture;
import org.opengis.cite.ogcapimaps10.conformance.RequirementClass;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

/**
 * A.12.6. Abstract Test for Requirement /projectionsAndDatums resource.
 *
 * <pre>
 * Abstract test A.44
 *
 * Identifier:  /conf/projection/projections-resource
 * Requirement: Requirement 44: /req/projection/projections-resource
 * Test purpose: Verify that the implementation supports retrieving the list of
 *               available projection operation methods, their parameters, and the
 *               list of available datums at /projectionsAndDatums.
 * </pre>
 */
public class ProjectionsResource extends CommonFixture {

	private static final String PROJECTIONS_AND_DATUMS_PATH = "projectionsAndDatums";

	/**
	 * Skips this optional conformance class when it is not declared by the
	 * implementation.
	 * @param testContext The current TestNG context containing suite attributes.
	 */
	@BeforeClass
	@SuppressWarnings("unchecked")
	public void verifyProjectionConformanceClass(ITestContext testContext) {
		List<RequirementClass> requirementClasses = (List<RequirementClass>) testContext.getSuite()
			.getAttribute(REQUIREMENTCLASSES.getName());
		if (requirementClasses == null || !requirementClasses.contains(PROJECTION)) {
			throw new SkipException("The implementation does not declare conformance class "
					+ "https://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/projection. "
					+ "Abstract test A.44 is not applicable.");
		}
	}

	/**
	 * Verifies that a GET request to {@code /projectionsAndDatums} provides a JSON
	 * representation.
	 */
	@Test(description = "Implements A.12.6. Abstract Test for Requirement /projectionsAndDatums resource "
			+ "(Requirement /req/projection/projections-resource)")
	public void verifyProjectionsAndDatumsResource() {
		URI resourceUri = projectionsAndDatumsUri();
		Response response = init().baseUri(resourceUri.toString()).accept(ContentType.JSON).when().request(GET);

		int statusCode = response.statusCode();
		Assert.assertTrue(statusCode >= 200 && statusCode < 300,
				"GET " + resourceUri + " must return a successful HTTP status code, but returned " + statusCode + ".");

		String contentType = response.contentType();
		Assert.assertTrue(contentType != null && ContentType.JSON.matches(contentType), "GET " + resourceUri
				+ " must provide a JSON representation, but returned Content-Type '" + contentType + "'.");

		JsonPath json;
		try {
			json = response.jsonPath();
		}
		catch (RuntimeException e) {
			Assert.fail("GET " + resourceUri + " must return a valid JSON representation.", e);
			return;
		}

		Object document;
		try {
			document = json.get("$");
		}
		catch (RuntimeException e) {
			Assert.fail("GET " + resourceUri + " must return a valid JSON representation.", e);
			return;
		}
		Assert.assertTrue(document instanceof Map,
				"GET " + resourceUri + " must return a JSON object as the top-level value.");
	}

	private URI projectionsAndDatumsUri() {
		String baseUri = rootUri.toString();
		if (!baseUri.endsWith("/")) {
			baseUri += "/";
		}
		return URI.create(baseUri).resolve(PROJECTIONS_AND_DATUMS_PATH);
	}

}
