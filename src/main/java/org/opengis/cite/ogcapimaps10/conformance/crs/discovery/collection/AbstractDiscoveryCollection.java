package org.opengis.cite.ogcapifeatures10.conformance.crs.discovery.collection;

import io.restassured.path.json.JsonPath;
import org.opengis.cite.ogcapifeatures10.conformance.SuiteAttribute;
import org.testng.ITestContext;
import org.testng.annotations.DataProvider;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * AbstractDiscoveryCollection class.
 * </p>
 *
 * @author <a href="mailto:goltz@lat-lon.de">Lyn Goltz </a>
 */
public class AbstractDiscoveryCollection {

	/**
	 * <p>
	 * collectionIdAndJson.
	 * </p>
	 * @param testContext a {@link org.testng.ITestContext} object
	 * @return a {@link java.util.Iterator} object
	 */
	@DataProvider(name = "collectionIdAndJson")
	public Iterator<Object[]> collectionIdAndJson(ITestContext testContext) {
		Map<String, JsonPath> collectionsResponses = (Map<String, JsonPath>) testContext.getSuite()
			.getAttribute(SuiteAttribute.COLLECTION_BY_ID.getName());
		List<Object[]> collectionsData = new ArrayList<>();
		try {
			for (Map.Entry<String, JsonPath> collection : collectionsResponses.entrySet()) {
				collectionsData.add(new Object[] { collection.getKey(), collection.getValue() });
			}
		}
		catch (Exception e) {
			collectionsData.add(new Object[] { null, null });
		}
		return collectionsData.iterator();
	}

}
