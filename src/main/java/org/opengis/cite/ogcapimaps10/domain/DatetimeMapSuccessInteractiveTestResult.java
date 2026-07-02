package org.opengis.cite.ogcapimaps10.domain;

/**
 * Wraps the results from interactive tests for datetime map-success verification.
 *
 * <p>
 * This class holds the boolean result of the interactive test that requires manual
 * verification by the user for the datetime conformance class
 * (/conf/datetime/map-success):
 * </p>
 * <ul>
 * <li>Whether the map returned for a specific subset=time(...) request visually reflects
 * data from the requested time period, not a default or arbitrary time</li>
 * </ul>
 */
public class DatetimeMapSuccessInteractiveTestResult {

	private final boolean enabled;

	private final boolean temporallyConsistent;

	/**
	 * Constructs a DatetimeMapSuccessInteractiveTestResult with the specified test
	 * results.
	 * @param enabled true if interactive tests were enabled by the user
	 * @param temporallyConsistent true if the map content visually reflects the requested
	 * datetime
	 */
	public DatetimeMapSuccessInteractiveTestResult(boolean enabled, boolean temporallyConsistent) {
		this.enabled = enabled;
		this.temporallyConsistent = temporallyConsistent;
	}

	/**
	 * Returns whether interactive tests were enabled by the user.
	 * @return true if the user checked the "Run datetime interactive tests" checkbox
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Returns whether the map content visually reflects the requested datetime.
	 * @return true if the interactive verification passed
	 */
	public boolean isTemporallyConsistent() {
		return temporallyConsistent;
	}

}
