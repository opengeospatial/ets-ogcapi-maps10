package org.opengis.cite.ogcapimaps10.conformance.spatialSubsetting;

import static io.restassured.RestAssured.given;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.opengis.cite.ogcapimaps10.OgcApiMaps10;
import org.opengis.cite.ogcapimaps10.conformance.CommonFixture;
import org.opengis.cite.ogcapimaps10.conformance.crs.query.crs.CoordinateSystem;
import org.opengis.cite.ogcapimaps10.exception.UnknownCrsException;
import org.opengis.cite.ogcapimaps10.util.BBox;
import org.opengis.cite.ogcapimaps10.util.GeometryTransformer;
import org.opengis.cite.ogcapimaps10.util.JsonUtils;
import org.opengis.cite.ogcapimaps10.util.RequestLimitFilter;
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;

import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;

import io.restassured.config.HttpClientConfig;
import io.restassured.config.JsonConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.Method;
import io.restassured.path.json.config.JsonPathConfig.NumberReturnType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/**
 * Base fixture for A.7 Conformance Class "Spatial Subsetting" tests involving the
 * {@code center-crs} parameter (Abstract Test A.20 / Requirement 20).
 *
 * <p>
 * In {@code @BeforeClass} this class:
 * <ol>
 * <li>Fetches {@code /conformance} to check whether the server declares the Spatial
 * Subsetting conformance class. If not declared, all tests are skipped.</li>
 * <li>Fetches {@code /collections} and, for each collection that advertises a map link
 * and has a spatial extent, builds a {@link CollectionMapEntry} (map URL, centre point in
 * CRS84, optional storage CRS and centre in that CRS).</li>
 * </ol>
 */
public class AbstractCenterCrs extends CommonFixture {

	/** OGC API map link-relation URI */
	public static final String MAP_REL = "http://www.opengis.net/def/rel/ogc/1.0/map";

	/** CRS84 URI as mandated by Req 20/B */
	public static final String CRS84_URI = "https://www.opengis.net/def/crs/OGC/1.3/CRS84";

	/** CRS84 expressed as a safe CURIE (Req 20/E) */
	public static final String CRS84_CURIE = "[OGC:CRS84]";

	/** An unsupported CRS URI used to provoke 400 responses (§13.5) */
	public static final String UNSUPPORTED_CRS = OgcApiMaps10.UNSUPPORTED_CRS;

	/**
	 * Spatial Subsetting conformance class URI (https variant, as specified in the spec)
	 */
	public static final String SPATIAL_SUBSETTING_CONF_URI = "https://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/spatial-subsetting";

	/**
	 * Spatial Subsetting conformance class URI (http variant, used by some
	 * implementations)
	 */
	public static final String SPATIAL_SUBSETTING_CONF_URI_HTTP = "http://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/spatial-subsetting";

	/**
	 * Primary entry used by tests 20/B, C, E, F and err (first qualifying collection).
	 */
	protected List<CollectionMapEntry> collectionMapEntries = new ArrayList<>();

	/**
	 * Entry used exclusively by test 20/D: the first collection whose storage CRS is
	 * known and differs from CRS84. May be {@code null} when no such collection exists.
	 */
	protected CollectionMapEntry storageCrsEntry = null;

	/** Socket (read) timeout in milliseconds for map endpoint requests. */
	private static final int SOCKET_TIMEOUT_MS = 30000;

	/** TCP connection timeout in milliseconds. */
	private static final int CONNECT_TIMEOUT_MS = 10000;

	// -----------------------------------------------------------------------
	// HTTP client override – adds timeouts absent from CommonFixture.init()
	// -----------------------------------------------------------------------

	/**
	 * Overrides {@link CommonFixture#init()} to attach socket and connection timeouts.
	 * Map endpoints can be slow to respond (large image generation); without a deadline
	 * the test run hangs indefinitely.
	 * @return a {@link RequestSpecification} with timeouts configured
	 */
	@Override
	protected RequestSpecification init() {
		JsonConfig jsonConfig = JsonConfig.jsonConfig().numberReturnType(NumberReturnType.DOUBLE);
		RestAssuredConfig config = RestAssuredConfig.newConfig()
			.httpClient(HttpClientConfig.httpClientConfig().httpClientFactory(() -> {
				DefaultHttpClient client = new DefaultHttpClient();
				HttpConnectionParams.setConnectionTimeout(client.getParams(), CONNECT_TIMEOUT_MS);
				HttpConnectionParams.setSoTimeout(client.getParams(), SOCKET_TIMEOUT_MS);
				return client;
			}))
			.jsonConfig(jsonConfig);
		return given().filters(new RequestLimitFilter(), requestLoggingFilter, responseLoggingFilter)
			.log()
			.all()
			.with()
			.config(config);
	}

	// -----------------------------------------------------------------------
	// Domain type
	// -----------------------------------------------------------------------

	/**
	 * Holds the test data for a single collection's map endpoint.
	 */
	protected static class CollectionMapEntry {

		final String collectionId;

		/** Absolute URL to the map resource. */
		final String mapUrl;

		/** Center point as {@code "lon,lat"} in CRS84. */
		final String centerCrs84;

		/**
		 * Storage (native) CRS URI; {@code null} when unknown or identical to CRS84.
		 */
		final String storageCrs;

		/**
		 * Center point coordinates in the storage CRS (comma-separated); {@code null}
		 * when {@link #storageCrs} is {@code null}.
		 */
		final String centerStorageCrs;

		CollectionMapEntry(String collectionId, String mapUrl, String centerCrs84, String storageCrs,
				String centerStorageCrs) {
			this.collectionId = collectionId;
			this.mapUrl = mapUrl;
			this.centerCrs84 = centerCrs84;
			this.storageCrs = storageCrs;
			this.centerStorageCrs = centerStorageCrs;
		}

	}

	// -----------------------------------------------------------------------
	// Setup
	// -----------------------------------------------------------------------

	/**
	 * Checks the server's conformance declaration and loads per-collection map data.
	 * Throws {@link SkipException} if the Spatial Subsetting conformance class is not
	 * declared.
	 * @param testContext the TestNG test context
	 */
	@BeforeClass
	public void setupSpatialSubsettingData(ITestContext testContext) {
		checkConformanceDeclaration();
		StringBuilder log = new StringBuilder();
		loadCollectionMapData(log);
		writeDiagnosticLog(log);
		if (collectionMapEntries.isEmpty()) {
			throw new SkipException("No collections with both a map endpoint and a spatial extent found at " + rootUri
					+ "/collections?f=json. See target/a20-diagnostic.txt for details.");
		}
	}

	private void writeDiagnosticLog(StringBuilder log) {
		System.out.println("========== A.20 DIAGNOSTIC ==========");
		System.out.print(log.toString());
		System.out.println("=====================================");
	}

	// -----------------------------------------------------------------------
	// Private helpers
	// -----------------------------------------------------------------------

	private void checkConformanceDeclaration() {
		try {
			Response conformanceResponse = init().baseUri(rootUri.toString())
				.when()
				.request(Method.GET, "/conformance");
			if (conformanceResponse.statusCode() != 200)
				return; // Cannot determine – proceed and let individual tests reveal
						// support
			List<String> conformsTo = conformanceResponse.jsonPath().getList("conformsTo");
			if (conformsTo == null)
				return; // Cannot determine – proceed
			boolean declared = conformsTo.contains(SPATIAL_SUBSETTING_CONF_URI)
					|| conformsTo.contains(SPATIAL_SUBSETTING_CONF_URI_HTTP);
			if (!declared) {
				throw new SkipException(
						"Neither " + SPATIAL_SUBSETTING_CONF_URI + " nor " + SPATIAL_SUBSETTING_CONF_URI_HTTP
								+ " is declared in /conformance. Spatial Subsetting tests will be skipped.");
			}
		}
		catch (SkipException e) {
			throw e;
		}
		catch (Exception e) {
			// Cannot reach /conformance – proceed and let tests reveal support
		}
	}

	private void loadCollectionMapData(StringBuilder log) {
		try {
			String collectionsUrl = rootUri + "/collections?f=json";
			Response collectionsResponse = init().baseUri(rootUri.toString())
				.param("f", "json")
				.when()
				.request(Method.GET, "/collections");
			int status = collectionsResponse.statusCode();
			log.append("GET ").append(collectionsUrl).append(" → HTTP ").append(status).append("\n");
			if (status != 200)
				return;
			String body = collectionsResponse.getBody().asString();
			log.append("Response body length: ").append(body.length()).append(" chars\n");
			Map<String, Object> root = new ObjectMapper().readValue(body, Map.class);
			List<Map<String, Object>> collections = (List<Map<String, Object>>) root.get("collections");
			if (collections == null) {
				log.append("Response contained no 'collections' array.\n");
				return;
			}
			log.append("Total collections in response: ").append(collections.size()).append("\n");
			int errors = 0;
			for (Map<String, Object> collection : collections) {
				boolean needPrimary = collectionMapEntries.isEmpty();
				boolean needStorageCrs = storageCrsEntry == null;
				if (!needPrimary && !needStorageCrs)
					break;
				try {
					CollectionMapEntry entry = buildEntry(collection);
					if (entry == null)
						continue;
					if (needPrimary) {
						collectionMapEntries.add(entry);
						log.append("Primary collection selected: '").append(entry.collectionId).append("'\n");
					}
					if (needStorageCrs && entry.storageCrs != null) {
						storageCrsEntry = entry;
						log.append("Storage-CRS collection selected: '").append(entry.collectionId).append("'\n");
					}
				}
				catch (Exception e) {
					errors++;
					log.append("  Error processing collection '")
						.append(collection.get("id"))
						.append("': ")
						.append(e.getMessage())
						.append("\n");
				}
			}
			log.append("Primary entry: ")
				.append(collectionMapEntries.isEmpty() ? "none" : collectionMapEntries.get(0).collectionId)
				.append(", storage-CRS entry: ")
				.append(storageCrsEntry == null ? "none" : storageCrsEntry.collectionId)
				.append(", errors: ")
				.append(errors)
				.append("\n");
		}
		catch (Exception e) {
			log.append("Exception in loadCollectionMapData: ").append(e.getMessage()).append("\n");
		}
	}

	private CollectionMapEntry buildEntry(Map<String, Object> collection) {
		String collectionId = (String) collection.get("id");
		String mapUrl = JsonUtils.findMapUrl(rootUri, collection);
		if (mapUrl == null && collectionId != null) {
			// Fall back to the conventional path /collections/{id}/map
			mapUrl = rootUri.toString().replaceAll("/$", "") + "/collections/" + collectionId + "/map";
		}
		if (mapUrl == null)
			return null;

		BBox bbox = JsonUtils.parseSpatialExtent(collection);
		if (bbox == null)
			return null;

		double centerLon = (bbox.getMinX() + bbox.getMaxX()) / 2.0;
		double centerLat = (bbox.getMinY() + bbox.getMaxY()) / 2.0;
		String centerCrs84 = formatCoord(centerLon) + "," + formatCoord(centerLat);

		String storageCrs = null;
		String centerStorageCrs = null;
		Object storageCrsObj = collection.get("storageCrs");
		if (storageCrsObj instanceof String) {
			String code = (String) storageCrsObj;
			// Only exercise 20/D when the storage CRS differs from CRS84 (http or https
			// variant)
			if (!normalizeScheme(code).equals(normalizeScheme(CRS84_URI))) {
				try {
					CoordinateSystem src = OgcApiMaps10.DEFAULT_CRS;
					CoordinateSystem target = new CoordinateSystem(code);
					GeometryTransformer transformer = new GeometryTransformer(src, target);
					BBox transformed = transformer.transform(bbox);
					double cx = (transformed.getMinX() + transformed.getMaxX()) / 2.0;
					double cy = (transformed.getMinY() + transformed.getMaxY()) / 2.0;
					storageCrs = code;
					centerStorageCrs = formatCoord(cx) + "," + formatCoord(cy);
				}
				catch (Exception ignored) {
					// Storage CRS not supported or transform failed – skip 20/D for this
					// collection
				}
			}
		}

		return new CollectionMapEntry(collectionId, mapUrl, centerCrs84, storageCrs, centerStorageCrs);
	}

	private String formatCoord(double value) {
		NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
		DecimalFormat df = (DecimalFormat) nf;
		df.applyPattern("###.0000000");
		return df.format(value);
	}

	/**
	 * Normalises a URI to its {@code http://} form so that {@code http://} and
	 * {@code https://} variants of the same CRS URI compare equal.
	 * @param uri the URI to normalise
	 * @return the URI with an {@code https://} prefix replaced by {@code http://}
	 */
	protected String normalizeScheme(String uri) {
		if (uri != null && uri.startsWith("https://")) {
			return "http://" + uri.substring("https://".length());
		}
		return uri;
	}

}
