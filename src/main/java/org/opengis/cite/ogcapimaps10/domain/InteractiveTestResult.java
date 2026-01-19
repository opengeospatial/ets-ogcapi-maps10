package org.opengis.cite.ogcapimaps10.domain;

/**
 * Wraps the results from interactive tests for tiles-parameters verification.
 *
 * <p>
 * This class holds the boolean results of interactive tests that require manual
 * verification by the user, such as background color display, transparency, display
 * resolution, and spatial subsetting.
 * </p>
 */
public class InteractiveTestResult {

	private final boolean enabled;

	private final boolean bgcolorCorrect;

	private final boolean transparentCorrect;

	private final boolean displayResolutionCorrect;

	private final boolean subsetCorrect;

	/**
	 * Constructs an InteractiveTestResult with the specified test results.
	 * @param enabled true if interactive tests were enabled by the user
	 * @param bgcolorCorrect true if the bgcolor parameter test passed
	 * @param transparentCorrect true if the transparent parameter test passed
	 * @param displayResolutionCorrect true if the mm-per-pixel parameter test passed
	 * @param subsetCorrect true if the subset parameter test passed
	 */
	public InteractiveTestResult(boolean enabled, boolean bgcolorCorrect, boolean transparentCorrect,
			boolean displayResolutionCorrect, boolean subsetCorrect) {
		this.enabled = enabled;
		this.bgcolorCorrect = bgcolorCorrect;
		this.transparentCorrect = transparentCorrect;
		this.displayResolutionCorrect = displayResolutionCorrect;
		this.subsetCorrect = subsetCorrect;
	}

	/**
	 * Returns whether interactive tests were enabled by the user.
	 * @return true if the user checked the "Run interactive tests" checkbox
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Returns whether the bgcolor parameter interactive test passed.
	 * @return true if the tile displayed the correct background color
	 */
	public boolean isBgcolorCorrect() {
		return bgcolorCorrect;
	}

	/**
	 * Returns whether the transparent parameter interactive test passed.
	 * @return true if the tile displayed correct transparency
	 */
	public boolean isTransparentCorrect() {
		return transparentCorrect;
	}

	/**
	 * Returns whether the display resolution (mm-per-pixel) interactive test passed.
	 * @return true if the tile displayed at the correct resolution
	 */
	public boolean isDisplayResolutionCorrect() {
		return displayResolutionCorrect;
	}

	/**
	 * Returns whether the subset parameter interactive test passed.
	 * @return true if the tile correctly showed the specified subset area
	 */
	public boolean isSubsetCorrect() {
		return subsetCorrect;
	}

}
