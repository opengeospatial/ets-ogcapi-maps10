package org.opengis.cite.ogcapimaps10.domain;

/**
 * Holds the pre-collected interactive verification results for Abstract Test A.16
 * (display resolution / mm-per-pixel). Values are populated by the CTL frontend before
 * TestNG runs and read by {@code MmPerPixelDefinitionTest}.
 */
public class DisplayResolutionInteractiveTestResult {

	private final boolean enabled;

	private final boolean mmPerPixelCorrect;

	/**
	 * Constructs a new result object.
	 * @param enabled whether the interactive tests were enabled by the user
	 * @param mmPerPixelCorrect whether the user confirmed that the two maps (rendered at
	 * different mm-per-pixel values) look visually different
	 */
	public DisplayResolutionInteractiveTestResult(boolean enabled, boolean mmPerPixelCorrect) {
		this.enabled = enabled;
		this.mmPerPixelCorrect = mmPerPixelCorrect;
	}

	/**
	 * Returns whether interactive tests were enabled.
	 * @return true if the user opted in to interactive verification
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Returns whether the user confirmed correct mm-per-pixel interpretation.
	 * @return true if the user confirmed the two maps look visually different
	 */
	public boolean isMmPerPixelCorrect() {
		return mmPerPixelCorrect;
	}

}