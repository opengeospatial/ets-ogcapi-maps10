package org.opengis.cite.ogcapimaps10.conformance.mapTilesets;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.ISuite;

/**
 * Integration Test for DescLinks
 */
public class DescLinksIT {

	private static ITestContext testContext;

	private static ISuite suite;

	private static URI rootUri;

	@BeforeClass
	public static void initTestFixture() throws Exception {
		testContext = mock(ITestContext.class);
		suite = mock(ISuite.class);
		when(testContext.getSuite()).thenReturn(suite);

		rootUri = new URI("https://maps.gnosis.earth/ogcapi");
		when(suite.getAttribute("iut")).thenReturn(rootUri);
	}

	@Test
	public void testVerifyTilesetsLink_Success_Mock() throws Exception {
		Map<String, Object> mockCollectionsData = new HashMap<>();
		List<Map<String, Object>> collections = new ArrayList<>();
		Map<String, Object> collection = new HashMap<>();
		collection.put("id", "mock-collection");

		List<Map<String, Object>> links = new ArrayList<>();
		Map<String, Object> link = new HashMap<>();
		link.put("rel", "https://www.opengis.net/def/rel/ogc/1.0/tilesets-map");
		link.put("href", "http://mock/tiles");
		links.add(link);

		collection.put("links", links);
		collections.add(collection);
		mockCollectionsData.put("collections", collections);

		DescLinks descLinks = new DescLinks() {
			@Override
			protected Map<String, Object> fetchResource(String requestUrl) throws Exception {
				if (requestUrl.endsWith("/collections")) {
					return mockCollectionsData;
				}
				return null;
			}
		};

		descLinks.initCommonFixture(testContext);

		descLinks.verifyTilesetsLink();
	}

}