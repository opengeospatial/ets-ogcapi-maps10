package org.opengis.cite.ogcapimaps10.conformance.crs.query.bboxcrs;

import static org.opengis.cite.ogcapimaps10.util.JsonUtils.parseAsString;
import static org.testng.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.opengis.cite.ogcapimaps10.conformance.CommonFixture;
import org.opengis.cite.ogcapimaps10.conformance.SuiteAttribute;
import org.opengis.cite.ogcapimaps10.conformance.crs.query.crs.CoordinateSystem;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;

import io.restassured.path.json.JsonPath;

/**
 * <p>
 * AbstractBBoxCrs class.
 * </p>
 *
 * @author <a href="mailto:goltz@lat-lon.de">Lyn Goltz </a>
 */
public class AbstractBBoxCrs extends CommonFixture {

	/** Constant <code>BBOX_PARAM="bbox"</code> */
	public static final String BBOX_PARAM = "bbox";

	/** Constant <code>BBOX_CRS_PARAM="bbox-crs"</code> */
	public static final String BBOX_CRS_PARAM = "bbox-crs";

	protected Map<String, JsonPath> collectionsResponses;

	protected Map<String, List<CoordinateSystem>> collectionIdToCrs;

	protected Map<String, CoordinateSystem> collectionIdToDefaultCrs;

	/**
	 * <p>
	 * retrieveRequiredInformationFromTestContext.
	 * </p>
	 * @param testContext a {@link org.testng.ITestContext} object
	 */
	@BeforeClass
	public void retrieveRequiredInformationFromTestContext(ITestContext testContext) {
		this.collectionsResponses = (Map<String, JsonPath>) testContext.getSuite()
			.getAttribute(SuiteAttribute.COLLECTION_BY_ID.getName());
		this.collectionIdToCrs = (Map<String, List<CoordinateSystem>>) testContext.getSuite()
			.getAttribute(SuiteAttribute.COLLECTION_CRS_BY_ID.getName());
		this.collectionIdToDefaultCrs = (Map<String, CoordinateSystem>) testContext.getSuite()
			.getAttribute(SuiteAttribute.COLLECTION_DEFAULT_CRS_BY_ID.getName());
	}

	void assertSameFeatures(JsonPath responseWithBBox, JsonPath responseWithoutBBox) {
		List<String> responseWithBBoxIds = parseFeatureIds(responseWithBBox);
		List<String> responseWithoutBBoxIds = parseFeatureIds(responseWithoutBBox);
		assertTrue(responseWithoutBBoxIds.containsAll(responseWithBBoxIds));
	}

	private List<String> parseFeatureIds(JsonPath responseWithBBox) {
		List<Map<String, Object>> features = responseWithBBox.getList("features");
		if (features == null)
			return Collections.emptyList();
		return features.stream().map(feature -> parseAsString(feature.get("id"))).collect(Collectors.toList());
	}

}
