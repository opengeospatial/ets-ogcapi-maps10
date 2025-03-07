package org.opengis.cite.ogcapimaps10.conformance.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.opengis.cite.ogcapimaps10.conformance.CommonFixture;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;


/**
 * A.1.1. Abstract Test for Requirement Map Operation
 *
 */
public class MapOperation extends CommonFixture {

	/**
	 * <pre>
	 * Abstract test A.1
	 *
	 * Identifier: /conf/core/map-op
	 * Requirement: Requirement 1: /req/core/map-op
	 * Test purpose: Verify that the implementation supports the map retrieval operation.
	 *
	 * Test Method:
	 * Given: a geospatial data resource conforming to the Maps API Standard, with an API path including …/map… or discovered following a link with relation type [ogc-rel:map]
	 * When: performing a GET operation on the /map resource with a supported media type specified in the Accept: header (e.g., Accept: image/png,image/jpeg)
	 * Then: - assert that the implementation supports retrieving map resources at one or more …/map URL.
	 * </pre>
	 */
	@Test(description = "Implements A.1.1.  Abstract Test for Requirement Map Operation (Requirement /req/core/map-op)")
	public void verifyMapRetrievalOperation() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		String apiUrl = rootUri.toString()+ "/collections?f=json";

		Map<String, Object> data = objectMapper.readValue(new URL(apiUrl), Map.class);

		List<Map<String, Object>> collectionsList = (List<Map<String, Object>>) data.get("collections");

		for (Map<String, Object> collection : collectionsList) {
			 if (!collection.get("id").equals("mapserver_world_map")) {
			 continue;
			 }

			List<Map<String, Object>> collectionLinks = (List<Map<String, Object>>) collection.get("links");
			Map<String, Object> relMap = findLinkByRel(collectionLinks, "http://www.opengis.net/def/rel/ogc/1.0/map");

			String mapUrl = (String) relMap.get("href");
			URL url = new URL(mapUrl);

			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");

			// Get response headers
			Map<String, List<String>> headers = connection.getHeaderFields();
			List<String> contentTypeList = headers.get("Content-Type");

			boolean result = false;
			for (String contentType : contentTypeList) {
				if (contentType.startsWith("image/")) {
					result = true;
				}
			}

			Assert.assertTrue(result, "Test failed: Expected Content-Type to be an image format (image/*), but received: " + contentTypeList);

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
