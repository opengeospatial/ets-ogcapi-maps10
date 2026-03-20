package org.opengis.cite.ogcapimaps10.domain;

/**
 * Holds the pre-collected result of the interactive (manual) verification for Abstract
 * Test A.15 (scale-denominator-definition, Req 15/B-C-G): whether the server correctly
 * interprets the {@code scale-denominator} parameter and, when Subsetting is supported,
 * derives an appropriate bounding box from it.
 */
public class ScalingScaleDenominatorInteractiveTestResult {

	private final boolean enabled;

	private final boolean scaleDenominatorAppropriate;

	/**
	 * Creates a new result object.
	 * @param enabled whether the interactive verification was enabled by the tester
	 * @param scaleDenominatorAppropriate whether the tester confirmed that the server
	 * correctly interprets the scale-denominator parameter
	 */
	public ScalingScaleDenominatorInteractiveTestResult(boolean enabled, boolean scaleDenominatorAppropriate) {
		this.enabled = enabled;
		this.scaleDenominatorAppropriate = scaleDenominatorAppropriate;
	}

	/**
	 * Returns whether the interactive verification was enabled.
	 * @return {@code true} if enabled
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Returns whether the tester confirmed that the server correctly interprets the
	 * {@code scale-denominator} parameter.
	 * @return {@code true} if the scale-denominator behaviour was reported as appropriate
	 */
	public boolean isScaleDenominatorAppropriate() {
		return scaleDenominatorAppropriate;
	}

}
