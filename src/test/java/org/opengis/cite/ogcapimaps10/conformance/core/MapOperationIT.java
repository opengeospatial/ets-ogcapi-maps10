package org.opengis.cite.ogcapimaps10.conformance.core;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;

import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.ISuite;

/**
 * Integration Test for MapOperation
 */
public class MapOperationIT {

	private static ITestContext testContext;
	private static ISuite suite;
	private static URI rootUri;

	@BeforeClass
	public static void initTestFixture() throws Exception {
		testContext = mock(ITestContext.class);
		suite = mock(ISuite.class);
		when(testContext.getSuite()).thenReturn(suite);

		rootUri = new URI("https://demo.pygeoapi.io/stable");
		when(suite.getAttribute("iut")).thenReturn(rootUri);
	}

	@Test
	public void testMapOperation() throws Exception {
		MapOperation mapOperationTest = new MapOperation();
		mapOperationTest.initCommonFixture(testContext);

		mapOperationTest.verifyMapRetrievalOperation();
	}

}
