package org.opengis.cite.ogcapimaps10.domain;

/**
 * Holds the pre-collected interactive verification results for Abstract Test A.16
 * (display resolution / mm-per-pixel). Values are populated by the CTL frontend before
 * TestNG runs and read by {@code MmPerPixelDefinitionTest}.
 */
public class DisplayResolutionInteractiveTestResult {

	private final boolean enabled;

	private final boolean mmPerPixelCorrect;

	private final boolean mmPerPixelDefaultCorrect;

	/**
	 * Constructs a new result object.
	 * @param enabled whether the interactive tests were enabled by the user
	 * @param mmPerPixelCorrect whether the user confirmed that maps at different
	 * mm-per-pixel values look visually different
	 * @param mmPerPixelDefaultCorrect whether the user confirmed that a map without the
	 * mm-per-pixel parameter looks the same as a map with mm-per-pixel=0.28
	 */
	public DisplayResolutionInteractiveTestResult(boolean enabled, boolean mmPerPixelCorrect,
			boolean mmPerPixelDefaultCorrect) {
		this.enabled = enabled;
		this.mmPerPixelCorrect = mmPerPixelCorrect;
		this.mmPerPixelDefaultCorrect = mmPerPixelDefaultCorrect;
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

	/**
	 * Returns whether the user confirmed that the server assumes 0.28 mm/pixel by
	 * default.
	 * @return true if the user confirmed that the map without mm-per-pixel looks the same
	 * as the map with mm-per-pixel=0.28
	 */
	public boolean isMmPerPixelDefaultCorrect() {
		return mmPerPixelDefaultCorrect;
	}

}