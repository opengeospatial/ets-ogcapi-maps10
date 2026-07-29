package org.opengis.cite.ogcapimaps10.listener;

import static java.util.logging.Level.WARNING;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.logging.Level;

import org.opengis.cite.ogcapimaps10.TestRunArg;
import org.opengis.cite.ogcapimaps10.conformance.SuiteAttribute;
import org.opengis.cite.ogcapimaps10.domain.JpegInteractiveTestResult;
import org.opengis.cite.ogcapimaps10.domain.OrientationInteractiveTestResult;
import org.opengis.cite.ogcapimaps10.domain.PngInteractiveTestResult;
import org.opengis.cite.ogcapimaps10.domain.TiffInteractiveTestResult;
import org.opengis.cite.ogcapimaps10.util.ClientUtils;
import org.opengis.cite.ogcapimaps10.util.TestSuiteLogger;
import org.opengis.cite.ogcapimaps10.util.URIUtils;
import org.testng.ISuite;
import org.testng.ISuiteListener;

import jakarta.ws.rs.client.Client;

/**
 * A listener that performs various tasks before and after a test suite is run, usually
 * concerned with maintaining a shared test suite fixture. Since this listener is loaded
 * using the ServiceLoader mechanism, its methods will be called before those of other
 * suite listeners listed in the test suite definition and before any annotated
 * configuration methods.
 *
 * Attributes set on an ISuite instance are not inherited by constituent test group
 * contexts (ITestContext). However, suite attributes are still accessible from lower
 * contexts.
 *
 * @see org.testng.ISuite ISuite interface
 */
public class SuiteFixtureListener implements ISuiteListener {

	/** {@inheritDoc} */
	@Override
	public void onStart(ISuite suite) {
		processSuiteParameters(suite);
		registerClientComponent(suite);
	}

	/** {@inheritDoc} */
	@Override
	public void onFinish(ISuite suite) {
		if (null != System.getProperty("deleteSubjectOnFinish")) {
			deleteTempFiles(suite);
			System.getProperties().remove("deleteSubjectOnFinish");
		}
	}

	/**
	 * Processes test suite arguments and sets suite attributes accordingly. The entity
	 * referenced by the {@link TestRunArg#IUT iut} argument is retrieved and written to a
	 * File that is set as the value of the suite attribute
	 * {@link SuiteAttribute#TEST_SUBJ_FILE testSubjectFile}.
	 * @param suite An ISuite object representing a TestNG test suite.
	 */
	void processSuiteParameters(ISuite suite) {
		Map<String, String> params = suite.getXmlSuite().getParameters();
		TestSuiteLogger.log(Level.CONFIG, "Suite parameters\n" + params.toString());
		String iutParam = params.get(TestRunArg.IUT.toString());
		if ((null == iutParam) || iutParam.isEmpty()) {
			throw new IllegalArgumentException("Required test run parameter not found: " + TestRunArg.IUT.toString());
		}
		URI iutRef = URI.create(iutParam.trim());
		suite.setAttribute(SuiteAttribute.IUT.getName(), iutRef);
		File entityFile;
		try {
			entityFile = URIUtils.dereferenceURI(iutRef);
		}
		catch (IOException iox) {
			throw new RuntimeException("Failed to dereference resource located at " + iutRef, iox);
		}
		TestSuiteLogger.log(Level.FINE, String.format("Wrote test subject to file: %s (%d bytes)",
				entityFile.getAbsolutePath(), entityFile.length()));
		suite.setAttribute(SuiteAttribute.TEST_SUBJ_FILE.getName(), entityFile);

		String noOfCollections = params.get(TestRunArg.NOOFCOLLECTIONS.toString());
		try {
			if (noOfCollections != null) {
				int noOfCollectionsInt = Integer.parseInt(noOfCollections);
				suite.setAttribute(SuiteAttribute.NO_OF_COLLECTIONS.getName(), noOfCollectionsInt);
			}
		}
		catch (NumberFormatException e) {
			TestSuiteLogger.log(WARNING, String.format("Could not parse parameter %s: %s. Expected is a valid integer",
					TestRunArg.NOOFCOLLECTIONS, noOfCollections));
		}

		// Parse and set TileMatrixSet parameter (default to WebMercatorQuad)
		String tileMatrixSet = params.get(TestRunArg.TILE_MATRIX_SET.toString());
		if (tileMatrixSet == null || tileMatrixSet.isEmpty()) {
			tileMatrixSet = "WebMercatorQuad";
		}
		suite.setAttribute(SuiteAttribute.TILE_MATRIX_SET.getName(), tileMatrixSet);
		TestSuiteLogger.log(Level.CONFIG, "Using TileMatrixSet: " + tileMatrixSet);

		// Parse and set PNG interactive test results
		PngInteractiveTestResult pngInteractiveTestResult = parsePngInteractiveTestResults(params);
		suite.setAttribute(SuiteAttribute.PNG_INTERACTIVE_TEST_RESULT.getName(), pngInteractiveTestResult);

		// Parse and set JPEG interactive test results
		JpegInteractiveTestResult jpegInteractiveTestResult = parseJpegInteractiveTestResults(params);
		suite.setAttribute(SuiteAttribute.JPEG_INTERACTIVE_TEST_RESULT.getName(), jpegInteractiveTestResult);

		// Parse and set TIFF interactive test results
		TiffInteractiveTestResult tiffInteractiveTestResult = parseTiffInteractiveTestResults(params);
		suite.setAttribute(SuiteAttribute.TIFF_INTERACTIVE_TEST_RESULT.getName(), tiffInteractiveTestResult);

		parseOrientationInteractiveResults(suite);
	}

	/**
	 * Parses PNG interactive test results from the test run parameters.
	 * @param params The test run parameters map.
	 * @return A PngInteractiveTestResult containing the parsed boolean results.
	 */
	private PngInteractiveTestResult parsePngInteractiveTestResults(Map<String, String> params) {
		boolean enabled = parseBooleanParam(params, TestRunArg.PNG_INTERACTIVE_TESTS_ENABLED);
		boolean colorsRepresentFeatures = parseBooleanParam(params, TestRunArg.PNG_COLORS_REPRESENT_FEATURES);
		boolean portrayalConsistent = parseBooleanParam(params, TestRunArg.PNG_PORTRAYAL_CONSISTENT);
		return new PngInteractiveTestResult(enabled, colorsRepresentFeatures, portrayalConsistent);
	}

	/**
	 * Parses JPEG interactive test results from the test run parameters.
	 * @param params The test run parameters map.
	 * @return A JpegInteractiveTestResult containing the parsed boolean results.
	 */
	private JpegInteractiveTestResult parseJpegInteractiveTestResults(Map<String, String> params) {
		boolean enabled = parseBooleanParam(params, TestRunArg.JPEG_INTERACTIVE_TESTS_ENABLED);
		boolean colorsRepresentFeatures = parseBooleanParam(params, TestRunArg.JPEG_COLORS_REPRESENT_FEATURES);
		boolean portrayalConsistent = parseBooleanParam(params, TestRunArg.JPEG_PORTRAYAL_CONSISTENT);
		return new JpegInteractiveTestResult(enabled, colorsRepresentFeatures, portrayalConsistent);

	}

	/**
	 * Parses TIFF interactive test results from the test run parameters.
	 * @param params The test run parameters map.
	 * @return A TiffInteractiveTestResult containing the parsed boolean results.
	 */
	private TiffInteractiveTestResult parseTiffInteractiveTestResults(Map<String, String> params) {
		boolean enabled = parseBooleanParam(params, TestRunArg.TIFF_INTERACTIVE_TESTS_ENABLED);
		boolean portrayalConsistent = parseBooleanParam(params, TestRunArg.TIFF_PORTRAYAL_CONSISTENT);
		return new TiffInteractiveTestResult(enabled, portrayalConsistent);
	}

	/**
	 * Parses a boolean value from the parameters map.
	 * @param params The parameters map.
	 * @param arg The TestRunArg key to look up.
	 * @return The parsed boolean value, or false if not present.
	 */
	private boolean parseBooleanParam(Map<String, String> params, TestRunArg arg) {
		String value = params.get(arg.toString());
		return value != null && Boolean.parseBoolean(value);

	}

	/**
	 * Parses the interactive test parameters for A.37 orientation verification (Req
	 * 37/A–D) and stores the result as a suite attribute.
	 * @param suite the TestNG suite instance
	 */
	private void parseOrientationInteractiveResults(ISuite suite) {
		Map<String, String> params = suite.getXmlSuite().getParameters();
		String enabledStr = params.get(TestRunArg.ORIENTATION_INTERACTIVE_ENABLED.toString());
		boolean enabled = "true".equalsIgnoreCase(enabledStr);
		boolean row5 = false;
		boolean row7 = false;
		boolean row8 = false;
		boolean row9 = false;
		boolean row11 = false;
		if (enabled) {
			row5 = "true".equalsIgnoreCase(params.get(TestRunArg.ORIENTATION_RESULT_ROW5_ROTATION_CCW.toString()));
			row7 = "true"
				.equalsIgnoreCase(params.get(TestRunArg.ORIENTATION_RESULT_ROW7_DEFAULT_EQUALS_ZERO.toString()));
			row8 = "true".equalsIgnoreCase(params.get(TestRunArg.ORIENTATION_RESULT_ROW8_PIVOT_CENTER.toString()));
			row9 = "true".equalsIgnoreCase(params.get(TestRunArg.ORIENTATION_RESULT_ROW9_PIVOT_BBOX_CENTER.toString()));
			row11 = "true"
				.equalsIgnoreCase(params.get(TestRunArg.ORIENTATION_RESULT_ROW11_NO_EMPTY_CORNERS.toString()));
		}
		OrientationInteractiveTestResult result = new OrientationInteractiveTestResult(enabled, row5, row7, row8, row9,
				row11);
		suite.setAttribute(SuiteAttribute.ORIENTATION_INTERACTIVE_TEST_RESULT.getName(), result);
	}

	/**
	 * A client component is added to the suite fixture as the value of the
	 * {@link SuiteAttribute#CLIENT} attribute; it may be subsequently accessed via the
	 * {@link org.testng.ITestContext#getSuite()} method.
	 * @param suite The test suite instance.
	 */
	void registerClientComponent(ISuite suite) {
		Client client = ClientUtils.buildClient();
		if (null != client) {
			suite.setAttribute(SuiteAttribute.CLIENT.getName(), client);
		}
	}

	/**
	 * Deletes temporary files created during the test run if TestSuiteLogger is enabled
	 * at the INFO level or higher (they are left intact at the CONFIG level or lower).
	 * @param suite The test suite.
	 */
	void deleteTempFiles(ISuite suite) {
		if (TestSuiteLogger.isLoggable(Level.CONFIG)) {
			return;
		}
		File testSubjFile = (File) suite.getAttribute(SuiteAttribute.TEST_SUBJ_FILE.getName());
		if (testSubjFile.exists()) {
			testSubjFile.delete();
		}
	}

}
