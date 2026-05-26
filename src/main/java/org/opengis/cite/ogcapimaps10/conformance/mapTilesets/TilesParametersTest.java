package org.opengis.cite.ogcapimaps10.conformance.mapTilesets;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.Test;

import io.restassured.response.Response;

/**
 * A.2.2. Abstract Test for Requirement tiles-parameters
 *
 * <pre>
 * Abstract test A.5
 *
 * Identifier:    /conf/tilesets/tiles-parameters
 * Requirement:   Requirement 5: /req/tilesets/tiles-parameters
 * Test purpose:  Verify that the implementation supports relevant parameters for map tilesets
 *
 * Test method:
 * Given: a geospatial data resource conforming to this Standard, to "Map Tilesets",
 *        to OGC API - Tiles, and to Maps requirements classes introducing parameters
 *        relevant for map tiles
 * When:  retrieving the map tiles with parameters for the background, display resolution,
 *        spatial subsetting (only for subset and subset-crs parameters, and only if a
 *        vertical dimension is available), general subsetting, and scaling requirements classes
 * Then:
 * - assert that tiles responses reflect the relevant map parameters used for the requests
 *
 * NOTE: This is an integration test that verifies the tiles endpoint accepts parameters
 *       from other requirements classes. Detailed parameter validation is handled by each
 *       requirements class's own conformance tests (e.g., A.6-A.10 for Background).
 * </pre>
 */
public class TilesParametersTest extends TilesParametersFixture {

	private static final int TEST_WIDTH = 512;

	private static final int TEST_HEIGHT = 512;

	private static final int ALTERNATIVE_WIDTH = 256;

	private static final int ALTERNATIVE_HEIGHT = 256;

	/**
	 * <pre>
	 * Abstract test A.5
	 *
	 * Identifier: /conf/tilesets/tiles-parameters
	 * Requirement: Requirement 5: /req/tilesets/tiles-parameters
	 * Test purpose: Verify that the implementation supports relevant parameters for map tilesets
	 * </pre>
	 */
	@Test(description = "Implements A.2.2. Abstract Test for Requirement tiles-parameters (Requirement /req/tilesets/tiles-parameters)")
	public void verifyTilesParameters(ITestContext context) {
		List<String> errors = new ArrayList<>();

		String tilesetUrl = getFirstTilesetUrl();
		if (tilesetUrl == null) {
			throw new SkipException("No tileset URL available for testing tiles-parameters.");
		}

		String tileUrl = findFirstAvailableTile(tilesetUrl);
		if (tileUrl == null) {
			throw new SkipException("No tile URL available for testing tiles-parameters.");
		}

		// ============================================================
		// 1. Scaling Requirements Class (Req 13, 14, 15)
		// - width, height: verify pixel dimensions
		// - scale-denominator: verify tiles endpoint accepts the parameter
		// ============================================================

		if (hasConformance("/conf/scaling")) {
			// 1.1 Test width parameter
			verifyScalingWidth(tileUrl, errors);

			// 1.2 Test height parameter
			verifyScalingHeight(tileUrl, errors);

			// 1.3 Test width + height combined
			verifyScalingWidthHeight(tileUrl, errors);

			// 1.4 Test scale-denominator parameter
			verifyParameterAccepted(tileUrl, "scale-denominator", "50000000", "[Scaling/scale-denominator]", errors);
		}

		// ============================================================
		// 2. Background Requirements Class (Req 6, 7, 8, 9)
		// - bgcolor, transparent: bytes comparison (response must differ)
		// - void-color, void-transparent: HTTP 200 + image/*
		// (effect depends on CRS having void areas)
		// ============================================================

		if (hasConformance("/conf/background")) {
			verifyResponseDiffers(tileUrl, "bgcolor", "0xFF0000", "[Background/bgcolor]", errors);
			verifyResponseDiffers(tileUrl, "transparent", "true", "[Background/transparent]", errors);
			verifyConditionalParameter(tileUrl, "void-color", "00FF00", "[Background/void-color]", errors);
			verifyConditionalParameter(tileUrl, "void-transparent", "true", "[Background/void-transparent]", errors);
		}

		// ============================================================
		// 3. Display Resolution Requirements Class (Req 16)
		// - mm-per-pixel
		// - Verify tiles endpoint accepts this parameter
		// ============================================================

		if (hasConformance("/conf/display-resolution")) {
			verifyParameterAccepted(tileUrl, "mm-per-pixel", "0.56", "[Display Resolution/mm-per-pixel]", errors);
		}

		// ============================================================
		// 4. Spatial Subsetting Requirements Class (Req 22, 19)
		// - Only subset and subset-crs, only if vertical dimension available
		// - Verify tiles endpoint accepts these parameters
		// ============================================================

		if (hasConformance("/conf/spatial-subsetting")) {
			verifyConditionalParameter(tileUrl, "subset", "h(0:100)", "[Spatial Subsetting/subset]", errors);
			verifyConditionalParameter(tileUrl, "subset-crs", "http://www.opengis.net/def/crs/OGC/1.3/CRS84",
					"[Spatial Subsetting/subset-crs]", errors);
		}

		// ============================================================
		// 5. General Subsetting Requirements Class (Req 34)
		// - subset for additional dimensions (non-spatial, non-temporal)
		// - Verify tiles endpoint accepts this parameter
		// ============================================================

		if (hasConformance("/conf/general-subsetting")) {
			verifyConditionalParameter(tileUrl, "subset", "pressure(500:1000)", "[General Subsetting/subset]", errors);
		}

		// ============================================================
		// Final Assertion
		// ============================================================

		// Clear binary image data from response logging before assertion.
		// Binary content in the response stream causes EARL report XML to be truncated.
		clearMessages();

		if (!errors.isEmpty()) {
			StringBuilder message = new StringBuilder();
			message.append("Tiles-parameters verification failed with ").append(errors.size()).append(" error(s):\n");
			for (int i = 0; i < errors.size(); i++) {
				message.append("  ").append(i + 1).append(". ").append(errors.get(i)).append("\n");
			}
			throw new AssertionError(message.toString());
		}
	}

	// ============================================================
	// Scaling verification with pixel dimension checks
	// ============================================================

	private void verifyScalingWidth(String tileUrl, List<String> errors) {
		try {
			Response response = init().accept("image/*").queryParam("width", TEST_WIDTH).when().get(tileUrl);

			if (response.getStatusCode() != 200) {
				errors.add(String.format("[Scaling/width] Expected status 200 but got %d", response.getStatusCode()));
			}
			else {
				String contentType = response.getContentType();
				if (contentType == null || !contentType.startsWith("image/")) {
					errors.add(String.format("[Scaling/width] Expected image content type but got: %s", contentType));
				}
				else {
					BufferedImage image = readImage(response.asByteArray());
					if (image == null) {
						errors.add("[Scaling/width] Failed to read image from response");
					}
					else if (image.getWidth() != TEST_WIDTH) {
						errors.add(String.format("[Scaling/width] Image width mismatch. Expected %d but got %d",
								TEST_WIDTH, image.getWidth()));
					}
				}
			}
		}
		catch (Exception e) {
			errors.add("[Scaling/width] Exception: " + e.getMessage());
		}
	}

	private void verifyScalingHeight(String tileUrl, List<String> errors) {
		try {
			Response response = init().accept("image/*").queryParam("height", TEST_HEIGHT).when().get(tileUrl);

			if (response.getStatusCode() != 200) {
				errors.add(String.format("[Scaling/height] Expected status 200 but got %d", response.getStatusCode()));
			}
			else {
				BufferedImage image = readImage(response.asByteArray());
				if (image == null) {
					errors.add("[Scaling/height] Failed to read image from response");
				}
				else if (image.getHeight() != TEST_HEIGHT) {
					errors.add(String.format("[Scaling/height] Image height mismatch. Expected %d but got %d",
							TEST_HEIGHT, image.getHeight()));
				}
			}
		}
		catch (Exception e) {
			errors.add("[Scaling/height] Exception: " + e.getMessage());
		}
	}

	private void verifyScalingWidthHeight(String tileUrl, List<String> errors) {
		try {
			Response response = init().accept("image/*")
				.queryParam("width", ALTERNATIVE_WIDTH)
				.queryParam("height", ALTERNATIVE_HEIGHT)
				.when()
				.get(tileUrl);

			if (response.getStatusCode() != 200) {
				errors.add(String.format("[Scaling/width+height] Expected status 200 but got %d",
						response.getStatusCode()));
			}
			else {
				BufferedImage image = readImage(response.asByteArray());
				if (image == null) {
					errors.add("[Scaling/width+height] Failed to read image from response");
				}
				else {
					if (image.getWidth() != ALTERNATIVE_WIDTH) {
						errors.add(String.format("[Scaling/width+height] Image width mismatch. Expected %d but got %d",
								ALTERNATIVE_WIDTH, image.getWidth()));
					}
					if (image.getHeight() != ALTERNATIVE_HEIGHT) {
						errors.add(String.format("[Scaling/width+height] Image height mismatch. Expected %d but got %d",
								ALTERNATIVE_HEIGHT, image.getHeight()));
					}
				}
			}
		}
		catch (Exception e) {
			errors.add("[Scaling/width+height] Exception: " + e.getMessage());
		}
	}

	// ============================================================
	// Bytes comparison verification
	// ============================================================

	/**
	 * Verifies that a parameter is reflected by the tiles endpoint by comparing the
	 * response bytes with and without the parameter. If the responses are identical, the
	 * parameter is likely being ignored. This is used for parameters whose effect is
	 * clearly visible (e.g., bgcolor, transparent).
	 * @param tileUrl The tile URL to test.
	 * @param paramName The query parameter name.
	 * @param paramValue The query parameter value.
	 * @param label The label for error messages.
	 * @param errors The list to collect error messages.
	 */
	private void verifyResponseDiffers(String tileUrl, String paramName, String paramValue, String label,
			List<String> errors) {
		try {
			// Get default tile (without parameter)
			Response defaultResponse = init().accept("image/*").when().get(tileUrl);
			if (defaultResponse.getStatusCode() != 200) {
				errors.add(String.format("%s Default tile request failed with status %d", label,
						defaultResponse.getStatusCode()));
				return;
			}
			byte[] defaultBytes = defaultResponse.asByteArray();

			// Get tile with parameter
			Response paramResponse = init().accept("image/*").queryParam(paramName, paramValue).when().get(tileUrl);

			if (paramResponse.getStatusCode() != 200) {
				errors.add(String.format("%s Expected status 200 but got %d", label, paramResponse.getStatusCode()));
				return;
			}

			String contentType = paramResponse.getContentType();
			if (contentType == null || !contentType.startsWith("image/")) {
				errors.add(String.format("%s Expected image content type but got: %s", label, contentType));
				return;
			}

			byte[] paramBytes = paramResponse.asByteArray();

			if (Arrays.equals(defaultBytes, paramBytes)) {
				errors.add(String.format(
						"%s Response with %s=%s is identical to default response — parameter may be ignored", label,
						paramName, paramValue));
			}
		}
		catch (Exception e) {
			errors.add(String.format("%s Exception: %s", label, e.getMessage()));
		}
	}

	// ============================================================
	// Generic parameter acceptance verification
	// ============================================================

	/**
	 * Verifies that the tiles endpoint accepts a given parameter by checking that the
	 * response is HTTP 200 with an image content type. This is used for parameters whose
	 * effect depends on data/CRS conditions and may not always produce a visibly
	 * different response. Detailed parameter behavior validation is handled by each
	 * requirement class's own conformance tests.
	 * @param tileUrl The tile URL to test.
	 * @param paramName The query parameter name.
	 * @param paramValue The query parameter value.
	 * @param label The label for error messages (e.g., "[Background/bgcolor]").
	 * @param errors The list to collect error messages.
	 */
	private void verifyParameterAccepted(String tileUrl, String paramName, String paramValue, String label,
			List<String> errors) {
		try {
			Response response = init().accept("image/*").queryParam(paramName, paramValue).when().get(tileUrl);

			int statusCode = response.getStatusCode();
			if (statusCode != 200) {
				errors.add(String.format("%s Expected status 200 but got %d", label, statusCode));
				return;
			}

			String contentType = response.getContentType();
			if (contentType == null || !contentType.startsWith("image/")) {
				errors.add(String.format("%s Expected image content type but got: %s", label, contentType));
			}
		}
		catch (Exception e) {
			errors.add(String.format("%s Exception: %s", label, e.getMessage()));
		}
	}

	// ============================================================
	// Conditional parameter verification
	// ============================================================

	/**
	 * Verifies a parameter that depends on data conditions (e.g., vertical dimension must
	 * be available for spatial subsetting, extra dimensions for general subsetting). HTTP
	 * 400 is treated as "not applicable" (the server does not have the required
	 * data/dimension) and is silently skipped, not counted as a failure. Only unexpected
	 * errors (e.g., 500) are reported as failures.
	 * @param tileUrl The tile URL to test.
	 * @param paramName The query parameter name.
	 * @param paramValue The query parameter value.
	 * @param label The label for error messages.
	 * @param errors The list to collect error messages.
	 */
	private void verifyConditionalParameter(String tileUrl, String paramName, String paramValue, String label,
			List<String> errors) {
		try {
			Response response = init().accept("image/*").queryParam(paramName, paramValue).when().get(tileUrl);

			int statusCode = response.getStatusCode();

			// 400 = server does not support this dimension/parameter value — skip
			if (statusCode == 400) {
				return;
			}

			if (statusCode != 200) {
				errors.add(String.format("%s Expected status 200 but got %d", label, statusCode));
				return;
			}

			String contentType = response.getContentType();
			if (contentType == null || !contentType.startsWith("image/")) {
				errors.add(String.format("%s Expected image content type but got: %s", label, contentType));
			}
		}
		catch (Exception e) {
			errors.add(String.format("%s Exception: %s", label, e.getMessage()));
		}
	}

	// ============================================================
	// Helper methods
	// ============================================================

	/**
	 * Finds the first available tile from a tileset.
	 * @param tilesetUrl The tileset URL.
	 * @return The first available tile URL, or null if not found.
	 */
	private String findFirstAvailableTile(String tilesetUrl) {
		String tileTemplate = getTileUrlTemplate(tilesetUrl);
		if (tileTemplate != null) {
			return tileTemplate.replace("{tileMatrixSetId}", getTileMatrixSet())
				.replace("{tileMatrix}", "0")
				.replace("{tileRow}", "0")
				.replace("{tileCol}", "0");
		}
		return tilesetUrl + "/" + getTileMatrixSet() + "/0/0/0.png";
	}

	/**
	 * Reads an image from byte array.
	 * @param imageBytes The image bytes.
	 * @return The BufferedImage, or null if reading fails.
	 */
	private BufferedImage readImage(byte[] imageBytes) {
		try {
			return ImageIO.read(new ByteArrayInputStream(imageBytes));
		}
		catch (IOException e) {
			return null;
		}
	}

}
