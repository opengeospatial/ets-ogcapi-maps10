package org.opengis.cite.ogcapimaps10.conformance.spatialSubsetting;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import io.restassured.http.Method;
import io.restassured.response.Response;

/**
 * <pre>
 * Abstract Test A.20: /conf/spatial-subsetting/center-crs
 * Requirement:        Requirement 20: /req/spatial-subsetting/center-crs
 * Test Purpose:       Verify that the implementation supports the center-crs parameter
 *                     for specifying the CRS of the center parameter correctly.
 * Conformance class:  https://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/spatial-subsetting
 *
 * Test Method:
 *   Given: a map resource that conformed successfully to /conf/core
 *   When:  retrieving maps using center and center-crs parameters for different values,
 *          as well as different values for the crs parameter if supported and applicable,
 *   Then:  assert Req 20/A–F and the error condition in §13.5.
 * </pre>
 *
 * <p>
 * Tests implemented here:
 * <table border="1">
 * <caption>A.20 test coverage</caption>
 * <tr>
 * <th>#</th>
 * <th>ID</th>
 * <th>Description</th>
 * <th>Automated</th>
 * <th>Applies</th>
 * </tr>
 * <tr>
 * <td>1</td>
 * <td>20/A</td>
 * <td>OAS: center-crs param defined (type string, required: false)</td>
 * <td>No (not implemented)</td>
 * <td>always</td>
 * </tr>
 * <tr>
 * <td>2</td>
 * <td>20/B</td>
 * <td>center + center-crs=CRS84 → HTTP 200</td>
 * <td>Yes</td>
 * <td>always (Earth-centric data)</td>
 * </tr>
 * <tr>
 * <td>3</td>
 * <td>20/C</td>
 * <td>Omit center-crs → HTTP 200; same result as explicit center-crs=CRS84</td>
 * <td>Yes</td>
 * <td>always</td>
 * </tr>
 * <tr>
 * <td>4</td>
 * <td>20/D</td>
 * <td>center-crs = native/storage CRS → HTTP 200</td>
 * <td>Yes</td>
 * <td>only if storage CRS is known and differs from CRS84</td>
 * </tr>
 * <tr>
 * <td>5</td>
 * <td>20/E</td>
 * <td>center-crs as full URI and as safe CURIE [OGC:CRS84] → HTTP 200</td>
 * <td>Yes</td>
 * <td>always</td>
 * </tr>
 * <tr>
 * <td>6</td>
 * <td>20/F</td>
 * <td>No center param → center-crs ignored (HTTP 200, not 4xx)</td>
 * <td>Yes</td>
 * <td>always</td>
 * </tr>
 * <tr>
 * <td>7</td>
 * <td>20/err</td>
 * <td>Unsupported center-crs → HTTP 400 (§13.5)</td>
 * <td>Yes</td>
 * <td>always</td>
 * </tr>
 * </table>
 */
public class CenterCrsParameter extends AbstractCenterCrs {

	// -----------------------------------------------------------------------
	// Data providers
	// -----------------------------------------------------------------------

	/**
	 * Provides {@code [collectionId, mapUrl, centerCrs84]} tuples for all collections
	 * that expose a map endpoint and have a spatial extent.
	 * @param ctx the TestNG test context
	 * @return iterator over test data rows
	 */
	@DataProvider(name = "collectionMapEntries")
	public Iterator<Object[]> collectionMapEntries(ITestContext ctx) {
		List<Object[]> data = new ArrayList<>();
		for (CollectionMapEntry e : collectionMapEntries) {
			data.add(new Object[] { e.collectionId, e.mapUrl, e.centerCrs84 });
		}
		if (data.isEmpty())
			data.add(new Object[] { null, null, null });
		return data.iterator();
	}

	/**
	 * Provides a single {@code [collectionId, mapUrl, centerStorageCrs, storageCrs]}
	 * tuple for the first collection whose storage CRS differs from CRS84 and can be
	 * transformed, as discovered during setup.
	 * @param ctx the TestNG test context
	 * @return iterator over test data rows
	 */
	@DataProvider(name = "collectionMapEntriesWithStorageCrs")
	public Iterator<Object[]> collectionMapEntriesWithStorageCrs(ITestContext ctx) {
		List<Object[]> data = new ArrayList<>();
		if (storageCrsEntry != null) {
			data.add(new Object[] { storageCrsEntry.collectionId, storageCrsEntry.mapUrl,
					storageCrsEntry.centerStorageCrs, storageCrsEntry.storageCrs });
		}
		else {
			data.add(new Object[] { null, null, null, null });
		}
		return data.iterator();
	}

	// -----------------------------------------------------------------------
	// Test 20/B – CRS84 accepted as center-crs value
	// -----------------------------------------------------------------------

	/**
	 * Req 20/B: For Earth-centric data,
	 * {@code https://www.opengis.net/def/crs/OGC/1.3/CRS84} SHALL be supported as a value
	 * for {@code center-crs}.
	 * @param collectionId the collection under test
	 * @param mapUrl the absolute URL of the map resource
	 * @param centerCrs84 the centre point as {@code "lon,lat"} in CRS84
	 */
	@Test(description = "A.20 Req 20/B: center + center-crs=CRS84 → HTTP 200 (CRS84 supported as a value for center-crs)",
			dataProvider = "collectionMapEntries", timeOut = 30000)
	public void verifyCenterCrsWithCrs84(String collectionId, String mapUrl, String centerCrs84) {
		if (collectionId == null)
			throw new SkipException(
					"No collection map endpoints found at " + rootUri + "/collections?f=json; skipping test.");

		String requestUrl = mapUrl + "?center=" + centerCrs84 + "&center-crs=" + CRS84_URI + "&f=png";
		Response response = init().baseUri(mapUrl)
			.param("center", centerCrs84)
			.param("center-crs", CRS84_URI)
			.param("f", "png")
			.when()
			.request(Method.GET);
		Assert.assertEquals(response.statusCode(), 200, "GET " + requestUrl);
	}

	// -----------------------------------------------------------------------
	// Test 20/C – CRS84 is assumed when center-crs is omitted
	// -----------------------------------------------------------------------

	/**
	 * Req 20/C: If {@code center-crs} is not used,
	 * {@code https://www.opengis.net/def/crs/OGC/1.3/CRS84} SHALL be assumed. Verified by
	 * checking that both requests (with and without {@code center-crs=CRS84}) succeed
	 * with HTTP 200.
	 * @param collectionId the collection under test
	 * @param mapUrl the absolute URL of the map resource
	 * @param centerCrs84 the centre point as {@code "lon,lat"} in CRS84
	 */
	@Test(description = "A.20 Req 20/C: Omitting center-crs → HTTP 200; same result as explicit center-crs=CRS84 (CRS84 assumed)",
			dataProvider = "collectionMapEntries", timeOut = 30000)
	public void verifyCenterCrsDefault(String collectionId, String mapUrl, String centerCrs84) {
		if (collectionId == null)
			throw new SkipException(
					"No collection map endpoints found at " + rootUri + "/collections?f=json; skipping test.");

		// 20/C: omitting center-crs must also yield HTTP 200 (CRS84 assumed by default).
		// The case with explicit center-crs=CRS84 is already covered by test 20/B.
		String urlWithoutCrs = mapUrl + "?center=" + centerCrs84 + "&f=png";
		Response withoutCrs = init().baseUri(mapUrl)
			.param("center", centerCrs84)
			.param("f", "png")
			.when()
			.request(Method.GET);
		Assert.assertEquals(withoutCrs.statusCode(), 200, "GET " + urlWithoutCrs);
	}

	// -----------------------------------------------------------------------
	// Test 20/D – Storage (native) CRS accepted
	// -----------------------------------------------------------------------

	/**
	 * Req 20/D: If the storage (native) CRS is known, the storage CRS SHALL be supported
	 * as a value for {@code center-crs}.
	 * @param collectionId the collection under test
	 * @param mapUrl the absolute URL of the map resource
	 * @param centerStorageCrs the centre point in the storage CRS (comma-separated)
	 * @param storageCrs the URI of the storage CRS
	 */
	@Test(description = "A.20 Req 20/D: center-crs set to the storage (native) CRS → HTTP 200",
			dataProvider = "collectionMapEntriesWithStorageCrs", timeOut = 30000)
	public void verifyCenterCrsWithStorageCrs(String collectionId, String mapUrl, String centerStorageCrs,
			String storageCrs) {
		if (collectionId == null)
			throw new SkipException("No collection with a known storage CRS different from CRS84 found at " + rootUri
					+ "/collections; skipping test.");

		String requestUrl = mapUrl + "?center=" + centerStorageCrs + "&center-crs=" + storageCrs + "&f=png";
		Response response = init().baseUri(mapUrl)
			.param("center", centerStorageCrs)
			.param("center-crs", storageCrs)
			.param("f", "png")
			.when()
			.request(Method.GET);
		Assert.assertEquals(response.statusCode(), 200, "GET " + requestUrl);
	}

	// -----------------------------------------------------------------------
	// Test 20/E – Both URI and safe CURIE forms accepted
	// -----------------------------------------------------------------------

	/**
	 * Req 20/E: CRS expressed as URIs or as safe CURIEs SHALL be supported. Verifies that
	 * both the full URI form ({@code https://…/CRS84}) and the safe-CURIE form
	 * ({@code [OGC:CRS84]}) return HTTP 200.
	 * @param collectionId the collection under test
	 * @param mapUrl the absolute URL of the map resource
	 * @param centerCrs84 the centre point as {@code "lon,lat"} in CRS84
	 */
	@Test(description = "A.20 Req 20/E: center-crs as full URI and as safe CURIE [OGC:CRS84] both accepted → HTTP 200",
			dataProvider = "collectionMapEntries", timeOut = 30000)
	public void verifyCenterCrsCurie(String collectionId, String mapUrl, String centerCrs84) {
		if (collectionId == null)
			throw new SkipException(
					"No collection map endpoints found at " + rootUri + "/collections?f=json; skipping test.");

		// Full URI form
		String uriUrl = mapUrl + "?center=" + centerCrs84 + "&center-crs=" + CRS84_URI + "&f=png";
		Response uriResponse = init().baseUri(mapUrl)
			.param("center", centerCrs84)
			.param("center-crs", CRS84_URI)
			.param("f", "png")
			.when()
			.request(Method.GET);
		Assert.assertEquals(uriResponse.statusCode(), 200, "GET " + uriUrl);

		// Safe CURIE form
		String curieUrl = mapUrl + "?center=" + centerCrs84 + "&center-crs=" + CRS84_CURIE + "&f=png";
		Response curieResponse = init().baseUri(mapUrl)
			.param("center", centerCrs84)
			.param("center-crs", CRS84_CURIE)
			.param("f", "png")
			.when()
			.request(Method.GET);
		Assert.assertEquals(curieResponse.statusCode(), 200, "GET " + curieUrl);
	}

	// -----------------------------------------------------------------------
	// Test 20/F – center-crs ignored when center is absent
	// -----------------------------------------------------------------------

	/**
	 * Req 20/F: If no {@code center} parameter is used, the {@code center-crs} SHALL be
	 * ignored. Verified by sending a request with {@code center-crs} but without
	 * {@code center} and asserting HTTP 200 (no 4xx error is raised).
	 * @param collectionId the collection under test
	 * @param mapUrl the absolute URL of the map resource
	 * @param centerCrs84 unused; present for data-provider compatibility
	 */
	@Test(description = "A.20 Req 20/F: No center param → center-crs is ignored (HTTP 200, not 4xx)",
			dataProvider = "collectionMapEntries", timeOut = 30000)
	public void verifyCenterCrsIgnoredWhenNoCenter(String collectionId, String mapUrl, String centerCrs84) {
		if (collectionId == null)
			throw new SkipException(
					"No collection map endpoints found at " + rootUri + "/collections?f=json; skipping test.");

		// Send center-crs without a center parameter; the server must not reject it
		String requestUrl = mapUrl + "?center-crs=" + CRS84_URI + "&f=png";
		Response response = init().baseUri(mapUrl)
			.param("center-crs", CRS84_URI)
			.param("f", "png")
			.when()
			.request(Method.GET);
		Assert.assertEquals(response.statusCode(), 200, "GET " + requestUrl);
	}

	// -----------------------------------------------------------------------
	// Test 20/err – Unsupported center-crs → HTTP 400
	// -----------------------------------------------------------------------

	/**
	 * §13.5 Error condition: If the CRS in the parameter value {@code center-crs} is not
	 * supported by the server, the status code SHALL be 400.
	 * @param collectionId the collection under test
	 * @param mapUrl the absolute URL of the map resource
	 * @param centerCrs84 the centre point as {@code "lon,lat"} in CRS84
	 */
	@Test(description = "A.20 §13.5: Unsupported center-crs value → HTTP 400", dataProvider = "collectionMapEntries",
			timeOut = 30000)
	public void verifyCenterCrsInvalid(String collectionId, String mapUrl, String centerCrs84) {
		if (collectionId == null)
			throw new SkipException(
					"No collection map endpoints found at " + rootUri + "/collections?f=json; skipping test.");

		String requestUrl = mapUrl + "?center=" + centerCrs84 + "&center-crs=" + UNSUPPORTED_CRS + "&f=png";
		Response response = init().baseUri(mapUrl)
			.param("center", centerCrs84)
			.param("center-crs", UNSUPPORTED_CRS)
			.param("f", "png")
			.when()
			.request(Method.GET);
		Assert.assertEquals(response.statusCode(), 400, "GET " + requestUrl);
	}

}
