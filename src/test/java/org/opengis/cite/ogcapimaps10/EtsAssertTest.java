package org.opengis.cite.ogcapimaps10;

import static org.hamcrest.CoreMatchers.is;
import static org.opengis.cite.ogcapimaps10.EtsAssert.assertCrsHeader;
import static org.opengis.cite.ogcapimaps10.EtsAssert.assertDefaultCrs;
import static org.opengis.cite.ogcapimaps10.EtsAssert.assertDefaultCrsAtFirst;
import static org.opengis.cite.ogcapimaps10.EtsAssert.assertInCrs84;
import static org.opengis.cite.ogcapimaps10.EtsAssert.assertValidCrsIdentifier;
import static org.opengis.cite.ogcapimaps10.OgcApiMaps10.DEFAULT_CRS;
import static org.opengis.cite.ogcapimaps10.util.JsonUtils.parseFeatureGeometry;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.opengis.cite.ogcapimaps10.conformance.crs.query.crs.CoordinateSystem;

import io.restassured.path.json.JsonPath;

/**
 * @author <a href="mailto:goltz@lat-lon.de">Lyn Goltz </a>
 */
public class EtsAssertTest {

	@Test
	public void testAssertTrue() {
		EtsAssert.assertTrue(true, "OK");
	}

	@Test
	public void testAssertFalse() {
		EtsAssert.assertFalse(false, "OK");
	}

	@Test(expected = AssertionError.class)
	public void testAssertTrue_false() {
		EtsAssert.assertTrue(false, "FAIlURE");
	}

	@Test(expected = AssertionError.class)
	public void testAssertFalse_true() {
		EtsAssert.assertFalse(true, "FAIlURE");
	}

	@Test
	public void testAssertValidCrsIdentifier_OGC_URL() {
		assertValidCrsIdentifier(new CoordinateSystem("http://www.opengis.net/def/crs/OGC/1.3/CRS84"), "OK");
	}

	@Test
	public void testAssertValidCrsIdentifier_OGC_URN() {
		assertValidCrsIdentifier(new CoordinateSystem("urn:ogc:def:crs:OGC:1.3:CRS84"), "OK");
	}

	@Test
	public void testAssertValidCrsIdentifier_URL() {
		assertValidCrsIdentifier(new CoordinateSystem("http://www.test.de/crs/4326"), "OK");
	}

	@Test
	public void testAssertValidCrsIdentifier_URN() {
		assertValidCrsIdentifier(new CoordinateSystem("urn:test:crs:CRS84"), "OK");
	}

	@Test(expected = AssertionError.class)
	public void testAssertValidCrsIdentifier_null() {
		assertValidCrsIdentifier(null, "FAIlURE");
	}

	@Test(expected = AssertionError.class)
	public void testAssertValidCrsIdentifier_empty() {
		assertValidCrsIdentifier(new CoordinateSystem(""), "FAIlURE");
	}

	@Test
	public void testAssertDefaultCrs() {
		CoordinateSystem defaultCrs = assertDefaultCrs(
				Arrays.asList("urn:test:crs:CRS84", OgcApiMaps10.DEFAULT_CRS_CODE), "OK");
		Assert.assertThat(defaultCrs, is(OgcApiMaps10.DEFAULT_CRS));
	}

	@Test(expected = AssertionError.class)
	public void testAssertDefaultCrs_Missing() {
		assertDefaultCrs(Arrays.asList("urn:test:crs:CRS84"), "OK");
	}

	@Test
	public void testAssertDefaultCrsAtFirst() {
		assertDefaultCrsAtFirst(Arrays.asList(OgcApiMaps10.DEFAULT_CRS_CODE, "urn:test:crs:CRS84"), "OK");
	}

	@Test(expected = AssertionError.class)
	public void testAssertDefaultCrsAtFirst_firstNotDefault() {
		assertDefaultCrsAtFirst(Arrays.asList("urn:test:crs:CRS84", OgcApiMaps10.DEFAULT_CRS_CODE), "FAIlURE");
	}

	@Test
	public void testAssertCrsHeader() {
		assertCrsHeader("<http://www.opengis.net/def/crs/OGC/1.3/CRS84>",
				new CoordinateSystem("http://www.opengis.net/def/crs/OGC/1.3/CRS84"), "OK");
	}

	@Test(expected = AssertionError.class)
	public void testAssertCrsHeader_UnexpectedCode() {
		assertCrsHeader("<http://www.opengis.net/def/crs/OGC/0/25832>",
				new CoordinateSystem("http://www.opengis.net/def/crs/OGC/1.3/CRS84"), "FAIlURE");
	}

	@Test(expected = AssertionError.class)
	public void testAssertCrsHeader_MissingBracket() {
		assertCrsHeader("http://www.opengis.net/def/crs/OGC/1.3/CRS84",
				new CoordinateSystem("http://www.opengis.net/def/crs/OGC/1.3/CRS84"), "FAIlURE");
	}

	@Test
	public void testAssertInCrs84() throws Exception {
		InputStream collectionItemsJson = EtsAssertTest.class
			.getResourceAsStream("conformance/core/collections/collectionItems-flurstueck.json");
		JsonPath jsonCollectionItem = new JsonPath(collectionItemsJson);
		List<Map<String, Object>> features = jsonCollectionItem.getList("features");
		Map<String, Object> firstFeature = features.get(0);
		Geometry geometry = parseFeatureGeometry(firstFeature, DEFAULT_CRS);
		assertInCrs84(geometry, "OK");
	}

}
