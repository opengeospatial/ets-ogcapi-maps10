package org.opengis.cite.ogcapimaps10.conformance.core;

import org.testng.ISuite;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.net.URI;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration Test for MapOperation
 */
public class MapResponseIT {

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
	public void testMapResponse() throws Exception {
		MapResponse mapResponseTest = new MapResponse();
		mapResponseTest.initCommonFixture(testContext);

		mapResponseTest.verifyMapResponse();
	}

}
