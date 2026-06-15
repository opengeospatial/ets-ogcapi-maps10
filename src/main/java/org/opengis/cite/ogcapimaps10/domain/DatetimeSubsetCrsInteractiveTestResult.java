package org.opengis.cite.ogcapimaps10.domain;

/**
 * Holds the pre-collected result of the interactive (manual) verification for Abstract
 * Test A.29 (subset-definition, Req 29/E): whether the server correctly interprets time
 * coordinates according to the CRS specified in the temporal extent, or Gregorian UTC if
 * not specified.
 */
public class DatetimeSubsetCrsInteractiveTestResult {

	private final boolean enabled;

	private final boolean crsInterpretationCorrect;

	/**
	 * Creates a new result object.
	 * @param enabled whether the interactive verification was enabled by the tester
	 * @param crsInterpretationCorrect whether the tester confirmed that the server
	 * correctly interprets time coordinates per the temporal CRS or Gregorian UTC
	 */
	public DatetimeSubsetCrsInteractiveTestResult(boolean enabled, boolean crsInterpretationCorrect) {
		this.enabled = enabled;
		this.crsInterpretationCorrect = crsInterpretationCorrect;
	}

	/**
	 * Returns whether the interactive verification was enabled.
	 * @return {@code true} if enabled
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Returns whether the tester confirmed that the server correctly interprets time
	 * coordinates per the temporal CRS or Gregorian UTC.
	 * @return {@code true} if CRS interpretation was reported as correct
	 */
	public boolean isCrsInterpretationCorrect() {
		return crsInterpretationCorrect;
	}

}
