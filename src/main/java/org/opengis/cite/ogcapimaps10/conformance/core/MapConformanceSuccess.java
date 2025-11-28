package org.opengis.cite.ogcapimaps10.conformance.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.opengis.cite.ogcapimaps10.conformance.CommonFixture;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

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
 *   https://www.opengis.net/spec/ogcapi-maps-1/1.0/req/core
 * </pre>
 */
public class MapConformanceSuccess extends CommonFixture {

	private static final String REQUIRED_CORE_URI = "https://www.opengis.net/spec/ogcapi-maps-1/1.0/req/core";

	@Test(description = "Implements A.1.3. Abstract Test for Requirement Map Conformance Success (Requirement /req/core/conformance-success)")
	public void verifyConformanceClassPresence() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		String conformanceUrl = rootUri.toString() + "/conformance";

		URL url = new URL(conformanceUrl);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");

		connection.setRequestProperty("Accept", "application/json");

		InputStream responseStream = connection.getInputStream();
		Map<String, Object> responseMap = objectMapper.readValue(responseStream, Map.class);

		Object conformsToObj = responseMap.get("conformsTo");
		Assert.assertNotNull(conformsToObj, "'conformsTo' is missing in response.");

		List<?> conformsToList = (List<?>) conformsToObj;

		boolean found = false;
		for (Object o : conformsToList) {
			if (o != null && o.toString().trim().equals(REQUIRED_CORE_URI)) {
				found = true;
				break;
			}
		}

		if (!found) {
			System.out.println("Returned conformsTo entries:");
			for (Object o : conformsToList) {
				System.out.println(" - " + o);
			}
		}

		Assert.assertTrue(found, "The required conformance class 'req/core' is not declared.");
	}

}