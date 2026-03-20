package org.opengis.cite.ogcapimaps10.domain;

/**
 * Wraps the results from interactive tests for JPEG XL content verification.
 *
 * <p>
 * This class holds the boolean results of interactive tests that require manual
 * verification by the user for the JPEG XL conformance class (/conf/jpegxl/content):
 * </p>
 * <ul>
 * <li>Part B: Whether the JPEG XL is a color image correctly representing geospatial
 * features or coverage values</li>
 * <li>Part C: Whether maps representing parts of the same resource follow the same
 * portrayal rules</li>
 * </ul>
 */
public class JpegXlInteractiveTestResult {

	private final boolean enabled;

	private final boolean colorsRepresentFeatures;

	private final boolean portrayalConsistent;

	/**
	 * Constructs a JpegXlInteractiveTestResult with the specified test results.
	 * @param enabled true if interactive tests were enabled by the user
	 * @param colorsRepresentFeatures true if the JPEG XL is a color image correctly
	 * representing geospatial features (Part B)
	 * @param portrayalConsistent true if maps of the same resource follow the same
	 * portrayal rules (Part C)
	 */
	public JpegXlInteractiveTestResult(boolean enabled, boolean colorsRepresentFeatures, boolean portrayalConsistent) {
		this.enabled = enabled;
		this.colorsRepresentFeatures = colorsRepresentFeatures;
		this.portrayalConsistent = portrayalConsistent;
	}

	/**
	 * Returns whether interactive tests were enabled by the user.
	 * @return true if the user checked the "Run interactive tests" checkbox
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Returns whether the JPEG XL is a color image correctly representing geospatial
	 * features or coverage values (Part B).
	 * @return true if the interactive verification passed
	 */
	public boolean isColorsRepresentFeatures() {
		return colorsRepresentFeatures;
	}

	/**
	 * Returns whether maps of the same resource follow the same portrayal rules (Part C).
	 * @return true if the interactive verification passed
	 */
	public boolean isPortrayalConsistent() {
		return portrayalConsistent;
	}

}
