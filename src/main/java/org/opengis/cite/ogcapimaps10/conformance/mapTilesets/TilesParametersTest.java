package org.opengis.cite.ogcapimaps10.conformance.mapTilesets;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.opengis.cite.ogcapimaps10.domain.InteractiveTestResult;
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
	 *
	 * Test method:
	 * Given: a geospatial data resource conforming to this Standard, to "Map Tilesets", to OGC API — Tiles, and to Maps requirements classes introducing parameters relevant for map tiles
	 * When: retrieving the map tiles with parameters for the background, display resolution, spatial subsetting (only for subset and subset-crs parameters, and only if a vertical dimension is available), general subsetting, and scaling requirements classes
	 * Then:
	 * - assert that tiles responses reflect the relevant map parameters used for the requests
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
		// 1. Scaling Requirements Class - Automated Verification
		// ============================================================

		// 1.1 Test width parameter
		try {
			Response widthResponse = init().accept("image/*").queryParam("width", TEST_WIDTH).when().get(tileUrl);

			if (widthResponse.getStatusCode() != 200) {
				errors.add(
						String.format("[Scaling/width] Expected status 200 but got %d", widthResponse.getStatusCode()));
			}
			else {
				String contentType = widthResponse.getContentType();
				if (contentType == null || !contentType.startsWith("image/")) {
					errors.add(String.format("[Scaling/width] Expected image content type but got: %s", contentType));
				}
				else {
					BufferedImage image = readImage(widthResponse.asByteArray());
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

		// 1.2 Test height parameter
		try {
			Response heightResponse = init().accept("image/*").queryParam("height", TEST_HEIGHT).when().get(tileUrl);

			if (heightResponse.getStatusCode() != 200) {
				errors.add(String.format("[Scaling/height] Expected status 200 but got %d",
						heightResponse.getStatusCode()));
			}
			else {
				BufferedImage image = readImage(heightResponse.asByteArray());
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

		// 1.3 Test width + height combined
		try {
			Response combinedResponse = init().accept("image/*")
				.queryParam("width", ALTERNATIVE_WIDTH)
				.queryParam("height", ALTERNATIVE_HEIGHT)
				.when()
				.get(tileUrl);

			if (combinedResponse.getStatusCode() != 200) {
				errors.add(String.format("[Scaling/width+height] Expected status 200 but got %d",
						combinedResponse.getStatusCode()));
			}
			else {
				BufferedImage image = readImage(combinedResponse.asByteArray());
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

		// ============================================================
		// 2. Background Requirements Class - Interactive Verification
		// ============================================================

		InteractiveTestResult interactiveResult = null;
		try {
			interactiveResult = getInteractiveTestResult(context);
		}
		catch (SkipException e) {
			// Interactive tests not enabled, skip these checks
		}

		if (interactiveResult != null && interactiveResult.isEnabled()) {
			// 2.1 Test bgcolor parameter
			if (!interactiveResult.isBgcolorCorrect()) {
				errors.add("[Background/bgcolor] Interactive verification failed: "
						+ "Tile does not display correct background color");
			}

			// 2.2 Test transparent parameter
			if (!interactiveResult.isTransparentCorrect()) {
				errors.add("[Background/transparent] Interactive verification failed: "
						+ "Tile does not display correct transparency");
			}

			// ============================================================
			// 3. Display Resolution Requirements Class - Interactive Verification
			// ============================================================

			// 3.1 Test mm-per-pixel parameter
			if (!interactiveResult.isDisplayResolutionCorrect()) {
				errors.add("[Display Resolution/mm-per-pixel] Interactive verification failed: "
						+ "Tile does not display at correct resolution/detail level");
			}

			// ============================================================
			// 4. Spatial Subsetting Requirements Class - Interactive Verification
			// ============================================================

			// 4.1 Test subset parameter
			if (!interactiveResult.isSubsetCorrect()) {
				errors.add("[Spatial Subsetting/subset] Interactive verification failed: "
						+ "Tile does not correctly show the specified subset area");
			}
		}

		// ============================================================
		// Final Assertion
		// ============================================================

		if (!errors.isEmpty()) {
			StringBuilder message = new StringBuilder();
			message.append("Tiles-parameters verification failed with ").append(errors.size()).append(" error(s):\n");
			for (int i = 0; i < errors.size(); i++) {
				message.append("  ").append(i + 1).append(". ").append(errors.get(i)).append("\n");
			}
			throw new AssertionError(message.toString());
		}
	}

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
