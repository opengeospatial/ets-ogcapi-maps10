package org.opengis.cite.ogcapimaps10.domain;

/**
 * Holds the pre-collected interactive verification results for Abstract Test A.17
 * (display resolution / map-success). Values are populated by the CTL frontend before
 * TestNG runs and read by {@code MapSuccessTest}.
 */
public class MapSuccessInteractiveTestResult {

	private final boolean enabled;

	private final boolean mapSuccessCorrect;

	/**
	 * Constructs a new result object.
	 * @param enabled whether the interactive tests were enabled by the user
	 * @param mapSuccessCorrect whether the user confirmed that maps rendered at
	 * mm-per-pixel=0.14 and mm-per-pixel=0.28 look visually different
	 */
	public MapSuccessInteractiveTestResult(boolean enabled, boolean mapSuccessCorrect) {
		this.enabled = enabled;
		this.mapSuccessCorrect = mapSuccessCorrect;
	}

	/**
	 * Returns whether interactive tests were enabled.
	 * @return true if the user opted in to interactive verification
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Returns whether the user confirmed correct map-success interpretation.
	 * @return true if the user confirmed the two maps look visually different
	 */
	public boolean isMapSuccessCorrect() {
		return mapSuccessCorrect;
	}

}
