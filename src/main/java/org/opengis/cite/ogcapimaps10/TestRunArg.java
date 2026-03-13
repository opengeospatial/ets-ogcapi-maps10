package org.opengis.cite.ogcapimaps10;

/**
 * An enumerated type defining all recognized test run arguments.
 */
public enum TestRunArg {

	/**
	 * An absolute URI that refers to a representation of the test subject or metadata
	 * about it.
	 */
	IUT,

	/**
	 * The number of collections to test (a value less or equal to 0 means all
	 * collections).
	 */
	NOOFCOLLECTIONS,

	/**
	 * Whether interactive tests for JPEG content were enabled by the user. true if the
	 * user checked the "Run JPEG interactive tests" checkbox, false otherwise.
	 */
	JPEG_INTERACTIVE_TESTS_ENABLED,

	/**
	 * Result of interactive test for JPEG Part B: whether the colors of the JPEG
	 * correctly represent geospatial features and/or coverage values. true if passed,
	 * false otherwise.
	 */
	JPEG_COLORS_REPRESENT_FEATURES,

	/**
	 * Result of interactive test for JPEG Part C: whether maps representing parts of the
	 * same resource follow the same portrayal rules. true if passed, false otherwise.
	 */
	JPEG_PORTRAYAL_CONSISTENT;

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
