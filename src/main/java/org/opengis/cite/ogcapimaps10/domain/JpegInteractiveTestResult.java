package org.opengis.cite.ogcapimaps10.domain;

/**
 * Wraps the results from interactive tests for JPEG content verification.
 *
 * <p>
 * This class holds the boolean results of interactive tests that require manual
 * verification by the user for the JPEG conformance class (/conf/jpeg/content):
 * </p>
 * <ul>
 * <li>Part B: Whether the colors of the JPEG correctly represent geospatial features
 * and/or coverage values</li>
 * <li>Part C: Whether maps representing parts of the same resource follow the same
 * portrayal rules</li>
 * </ul>
 */
public class JpegInteractiveTestResult {

	private final boolean enabled;

	private final boolean colorsRepresentFeatures;

	private final boolean portrayalConsistent;

	/**
	 * Constructs a JpegInteractiveTestResult with the specified test results.
	 * @param enabled true if interactive tests were enabled by the user
	 * @param colorsRepresentFeatures true if the JPEG colors correctly represent
	 * geospatial features (Part B)
	 * @param portrayalConsistent true if maps of the same resource follow the same
	 * portrayal rules (Part C)
	 */
	public JpegInteractiveTestResult(boolean enabled, boolean colorsRepresentFeatures, boolean portrayalConsistent) {
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
	 * Returns whether the JPEG colors correctly represent geospatial features and/or
	 * coverage values (Part B).
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
