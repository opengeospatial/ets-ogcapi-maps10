package org.opengis.cite.ogcapimaps10.conformance.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.opengis.cite.ogcapimaps10.conformance.CommonFixture;
import org.opengis.cite.ogcapimaps10.util.MapUtil;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * A.1.2. Abstract Test for Requirement Map Response
 */
public class MapResponse extends CommonFixture {

	protected int noOfCollections = 10;

	@BeforeClass
	public void initParameters(ITestContext context) {
		Object param = context.getCurrentXmlTest().getParameter("noOfCollections");
		if (param != null) {
			this.noOfCollections = Integer.parseInt(param.toString());
		}
	}

	/**
	 * <pre>
	 * Abstract test A.2
	 *
	 * Identifier: /conf/core/map-response
	 * Requirement: Requirement 2: /req/core/map-response
	 * Test purpose: Verify that the implementation’s response for the map retrieval operation is correct.
	 *
	 * Test method:
	 * Given: a map resource that was successfully retrieved for Abstract test A.1: /conf/core/map-op
	 * When: retrieving that resource for the map operation
	 * Then:
	 * - assert that the response has an HTTP status code 200,
	 * - assert that the map response is in the storage CRS specified in the description of the geospatial resource,
	 *   or https://www.opengis.net/def/crs/OGC/1.3/CRS84 if none is specified,
	 * - assert that the headers of the response include the Content-Crs header with the URI or the safe CURIEs
	 *   of the CRS used to render the map, except if the content is in the CRS84,
	 * - assert that the headers include a Content-Bbox header with the actual geospatial boundary of the rendered map,
	 * - assert Content-Bbox coordinates are in the response CRS and contain four comma-separated numbers,
	 * - assert if the geospatial resource has a temporal aspect, the headers include a Content-Datetime header
	 *   with the actual datetime instant or interval,
	 * - assert Content-Datetime uses the notation time-instant/time-instant if representing an interval,
	 * - assert Content-Datetime follows RFC 3339, allowing additional formats (yyyy, yyyy-mm, yyyy-mm-dd, etc.),
	 * - assert the body contains a map encoded in the negotiated format.
	 * </pre>
	 */
	@Test(description = "Implements A.1.2. Abstract Test for Requirement Map Response (Requirement /req/core/map-response)")
	public void verifyMapResponse() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		String apiUrl = rootUri.toString() + "/collections";
		HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
		connection.setRequestMethod("GET");
		connection.setRequestProperty("Accept", "application/json");

		Map<String, Object> data = objectMapper.readValue(connection.getInputStream(), Map.class);
		List<Map<String, Object>> collectionsList = (List<Map<String, Object>>) data.get("collections");

		int limit = Math.min(noOfCollections, collectionsList.size());

		for (int i = 0; i < limit; i++) {
			Map<String, Object> collection = collectionsList.get(i);
			List<Map<String, Object>> collectionLinks = (List<Map<String, Object>>) collection.get("links");
			Map<String, Object> relMap = findLinkByRel(collectionLinks, "http://www.opengis.net/def/rel/ogc/1.0/map");

			if (relMap == null) {
				continue;
			}

			String mapUrl = (String) relMap.get("href");

			URI uri = new URI(mapUrl);
			if (!uri.isAbsolute()) {
				uri = rootUri.resolve(uri);
			}
			URL url = uri.toURL();

			HttpURLConnection mapConnection = (HttpURLConnection) url.openConnection();
			mapConnection.setRequestMethod("GET");

			int responseCode = mapConnection.getResponseCode();
			Assert.assertEquals(responseCode, 200, "HTTP response code must be 200");

			String contentCrs = mapConnection.getHeaderField("Content-Crs");
			String contentBbox = mapConnection.getHeaderField("Content-Bbox");
			String contentDatetime = mapConnection.getHeaderField("Content-Datetime");

			Assert.assertNotNull(contentBbox, "Content-Bbox header must be present.");
			Assert.assertEquals(contentBbox.split(",").length, 4,
					"Content-Bbox must have four comma-separated numbers.");

			if (contentCrs != null && !contentCrs.equals("https://www.opengis.net/def/crs/OGC/1.3/CRS84")) {
				Assert.assertTrue(contentCrs.startsWith("http"), "Content-Crs header must be valid URI or CURIE.");
			}

			if (contentDatetime != null) {
				Assert.assertFalse(contentDatetime.isEmpty(), "Content-Datetime header must not be empty.");
			}
		}
	}

	public static Map<String, Object> findLinkByRel(List<Map<String, Object>> links, String expectedRel) {
		if (links == null) {
			return null;
		}
		for (Map<String, Object> link : links) {
			Object rel = link.get("rel");
			if (expectedRel.equals(rel)) {
				return link;
			}
		}
		return null;
	}

}
