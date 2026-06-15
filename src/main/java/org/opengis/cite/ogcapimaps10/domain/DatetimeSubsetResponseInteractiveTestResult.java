package org.opengis.cite.ogcapimaps10.domain;

/**
 * Holds the pre-collected result of the interactive (manual) verification for Abstract
 * Test A.30 (subset-response, Req 30/A): whether the server correctly applies the
 * temporal filter so that only data within the subset time bounds is returned.
 */
public class DatetimeSubsetResponseInteractiveTestResult {

	private final boolean enabled;

	private final boolean filterAppliedCorrect;

	/**
	 * Creates a new result object.
	 * @param enabled whether the interactive verification was enabled by the tester
	 * @param filterAppliedCorrect whether the tester confirmed that the subset-filtered
	 * map visually differs from the full-extent map, proving the temporal filter was
	 * applied
	 */
	public DatetimeSubsetResponseInteractiveTestResult(boolean enabled, boolean filterAppliedCorrect) {
		this.enabled = enabled;
		this.filterAppliedCorrect = filterAppliedCorrect;
	}

	/**
	 * Returns whether the interactive verification was enabled.
	 * @return {@code true} if enabled
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Returns whether the tester confirmed that the subset-filtered map differs from the
	 * full-extent map.
	 * @return {@code true} if the temporal filter was reported as correctly applied
	 */
	public boolean isFilterAppliedCorrect() {
		return filterAppliedCorrect;
	}

}
