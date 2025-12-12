package org.opengis.cite.ogcapimaps10.conformance.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.opengis.cite.ogcapimaps10.conformance.CommonFixture;
import org.opengis.cite.ogcapimaps10.conformance.SuiteAttribute;
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
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

/**
 * A.1.2. Abstract Test for Requirement Map Response
 */
public class MapResponse extends CommonFixture {

	protected int noOfCollections;

	@BeforeClass
	public void initParameters(ITestContext context) {
		Object noOfCollectionsAttr = context.getSuite().getAttribute(SuiteAttribute.NO_OF_COLLECTIONS.getName());
		if (noOfCollectionsAttr != null) {
			this.noOfCollections = (Integer) noOfCollectionsAttr;
		}
		else {
			this.noOfCollections = 10;
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

		Map<String, Object> data = objectMapper.readValue(connection.getInputStream(),
				new TypeReference<Map<String, Object>>() {
				});

		Object collectionsObj = data.get("collections");
		if (!(collectionsObj instanceof List)) {
			Assert.fail("Response 'collections' is not a valid list.");
		}

		List<Map<String, Object>> collectionsList = ((List<?>) collectionsObj).stream()
			.filter(item -> item instanceof Map)
			.map(item -> (Map<String, Object>) item)
			.toList();

		int limit = Math.min(noOfCollections, collectionsList.size());
		final String defaultCrs = "https://www.opengis.net/def/crs/OGC/1.3/CRS84";

		boolean validatedAny = false;

		for (int i = 0; i < limit; i++) {
			Map<String, Object> collection = collectionsList.get(i);

			Object linksObj = collection.get("links");
			if (!(linksObj instanceof List)) {
				continue;
			}

			List<Map<String, Object>> collectionLinks = ((List<?>) linksObj).stream()
				.filter(item -> item instanceof Map)
				.map(item -> (Map<String, Object>) item)
				.toList();

			Map<String, Object> relMap = findLinkByRel(collectionLinks, "http://www.opengis.net/def/rel/ogc/1.0/map");

			if (relMap == null) {
				continue;
			}

			validatedAny = true;

			String mapUrl = (String) relMap.get("href");
			if (mapUrl == null || mapUrl.isEmpty()) {
				continue;
			}

			String storageCrs = getStorageCrs(collection);
			boolean hasTemporalAspect = hasTemporalAspect(collection);

			URI uri = new URI(mapUrl);
			if (!uri.isAbsolute()) {
				uri = rootUri.resolve(uri);
			}
			URL url = uri.toURL();

			HttpURLConnection mapConnection = (HttpURLConnection) url.openConnection();
			mapConnection.setRequestMethod("GET");

			Object typeObj = relMap.get("type");
			if (typeObj instanceof String) {
				mapConnection.setRequestProperty("Accept", (String) typeObj);
			}

			// HTTP Status 200
			int responseCode = mapConnection.getResponseCode();
			Assert.assertEquals(responseCode, 200, "HTTP response code must be 200");

			// Get response headers
			String contentCrs = mapConnection.getHeaderField("Content-Crs");
			String contentBbox = mapConnection.getHeaderField("Content-Bbox");
			String contentDatetime = mapConnection.getHeaderField("Content-Datetime");
			String contentType = mapConnection.getHeaderField("Content-Type");

			// Storage CRS Validation
			String responseCrs = (contentCrs != null && !contentCrs.isEmpty()) ? contentCrs : defaultCrs;
			String normalizedResponseCrs = normalizeCrs(responseCrs);
			String normalizedDefaultCrs = normalizeCrs(defaultCrs);

			if (storageCrs != null && !storageCrs.isEmpty()) {
				String normalizedStorageCrs = normalizeCrs(storageCrs);
				Assert.assertEquals(normalizedResponseCrs, normalizedStorageCrs,
						"Map response CRS must match the storage CRS specified for the geospatial resource (http/https differences are ignored).");
			}
			else {
				Assert.assertEquals(normalizedResponseCrs, normalizedDefaultCrs,
						"Map response CRS must be CRS84 when no storage CRS is specified (http/https differences are ignored).");
			}

			// Content-Crs Header
			if (!defaultCrs.equals(responseCrs)) {
				Assert.assertNotNull(contentCrs,
						"Content-Crs header must be present when the response CRS is not CRS84.");
				Assert.assertFalse(contentCrs.isEmpty(),
						"Content-Crs header must not be empty when the response CRS is not CRS84.");
				Assert.assertTrue(isValidCrsIdentifier(contentCrs),
						"Content-Crs header must be a URI or a safe CURIE.");
			}

			// Content-Bbox Header
			Assert.assertNotNull(contentBbox, "Content-Bbox header must be present.");
			String[] bboxParts = contentBbox.split(",");
			Assert.assertEquals(bboxParts.length, 4,
					"Content-Bbox must have four comma-separated numbers (lower-left and upper-right corners).");
			for (String part : bboxParts) {
				try {
					Double.parseDouble(part.trim());
				}
				catch (NumberFormatException e) {
					Assert.fail("Content-Bbox values must be valid numbers in the response CRS: " + contentBbox);
				}
			}

			// Content-Datetime Header
			if (hasTemporalAspect) {
				Assert.assertNotNull(contentDatetime,
						"Content-Datetime header must be present when the geospatial resource has a temporal aspect.");
				Assert.assertFalse(contentDatetime.isEmpty(),
						"Content-Datetime header must not be empty when the geospatial resource has a temporal aspect.");
				assertValidContentDatetime(contentDatetime);
			}
			else if (contentDatetime != null && !contentDatetime.isEmpty()) {
				assertValidContentDatetime(contentDatetime);
			}

			// Content-Type
			Assert.assertNotNull(contentType, "Content-Type header must be present for map responses.");
			Assert.assertFalse(contentType.isEmpty(), "Content-Type header must not be empty for map responses.");
			if (typeObj instanceof String) {
				String expectedType = (String) typeObj;
				Assert.assertTrue(contentType.startsWith(expectedType),
						"Map response must be encoded in the negotiated format. Expected starting with '" + expectedType
								+ "' but was '" + contentType + "'.");
			}
		}
		Assert.assertTrue(validatedAny,
				"Test Skipped: No map resources found in the collections to verify. Please check the target server.");
	}

	private static String getStorageCrs(Map<String, Object> collection) {
		Object storageCrsObj = collection.get("storageCrs");
		String storageCrs = extractCrsFromObject(storageCrsObj);
		if (storageCrs != null && !storageCrs.isEmpty()) {
			return storageCrs;
		}
		Object crsObj = collection.get("crs");
		return extractCrsFromObject(crsObj);
	}

	private static String extractCrsFromObject(Object crsObj) {
		if (crsObj instanceof String) {
			return (String) crsObj;
		}
		if (crsObj instanceof List<?>) {
			List<?> list = (List<?>) crsObj;
			if (!list.isEmpty() && list.get(0) instanceof String) {
				return (String) list.get(0);
			}
		}
		return null;
	}

	private static boolean hasTemporalAspect(Map<String, Object> collection) {
		Object extentObj = collection.get("extent");
		if (extentObj instanceof Map<?, ?> extent) {
			return extent.containsKey("temporal");
		}
		return false;
	}

	private static boolean isValidCrsIdentifier(String crs) {
		if (crs == null || crs.isEmpty()) {
			return false;
		}

		if (crs.startsWith("http://") || crs.startsWith("https://")) {
			return true;
		}

		return crs.matches("[A-Za-z0-9._-]+:[^\\s]+");
	}

	private static void assertValidContentDatetime(String contentDatetime) {
		// Datetime Interval Notation (time-instant/time-instant)
		String[] parts = contentDatetime.split("/");
		if (parts.length == 1) {
			Assert.assertTrue(isValidTimeInstant(parts[0].trim()),
					"Content-Datetime must be a valid time instant as defined in RFC 3339 or the allowed reduced forms.");
		}
		else if (parts.length == 2) {
			String start = parts[0].trim();
			String end = parts[1].trim();
			Assert.assertFalse(start.isEmpty() || end.isEmpty(),
					"Content-Datetime interval must use 'time-instant/time-instant' notation with both start and end instants.");
			Assert.assertTrue(isValidTimeInstant(start), "Start instant of Content-Datetime interval must be valid.");
			Assert.assertTrue(isValidTimeInstant(end), "End instant of Content-Datetime interval must be valid.");
		}
		else {
			Assert.fail("Content-Datetime representing an interval must use 'time-instant/time-instant' notation.");
		}
	}

	private static boolean isValidTimeInstant(String value) {
		if (value == null || value.isEmpty()) {
			return false;
		}

		// Datetime Format (RFC 3339)
		try {
			OffsetDateTime.parse(value);
			return true;
		}
		catch (DateTimeParseException e) {
			// Check allowed reduced formats
		}

		// yyyy
		if (value.matches("\\d{4}")) {
			return true;
		}
		// yyyy-mm
		if (value.matches("\\d{4}-\\d{2}")) {
			return true;
		}
		// yyyy-mm-dd
		if (value.matches("\\d{4}-\\d{2}-\\d{2}")) {
			return true;
		}
		// yyyy-mm-ddThhZ or yyyy-mm-ddThh±hh:mm
		if (value.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}(Z|[+-]\\d{2}:\\d{2})")) {
			return true;
		}
		// yyyy-mm-ddThh:mmZ or yyyy-mm-ddThh:mm±hh:mm
		if (value.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(Z|[+-]\\d{2}:\\d{2})")) {
			return true;
		}

		return false;
	}

	private static String normalizeCrs(String crs) {
		if (crs == null) {
			return null;
		}
		String normalized = crs.trim();
		if (normalized.startsWith("http://")) {
			normalized = normalized.substring("http://".length());
		}
		else if (normalized.startsWith("https://")) {
			normalized = normalized.substring("https://".length());
		}
		return normalized;
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
