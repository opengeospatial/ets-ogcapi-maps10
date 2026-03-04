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
	 * Whether interactive tests for PNG content were enabled by the user. true if the
	 * user checked the "Run PNG interactive tests" checkbox, false otherwise.
	 */
	PNG_INTERACTIVE_TESTS_ENABLED,

	/**
	 * Result of interactive test for PNG Part B: whether the colors of the PNG correctly
	 * represent geospatial features or coverage values. true if passed, false otherwise.
	 */
	PNG_COLORS_REPRESENT_FEATURES,

	/**
	 * Result of interactive test for PNG Part D: whether maps representing parts of the
	 * same resource follow the same portrayal rules. true if passed, false otherwise.
	 */
	PNG_PORTRAYAL_CONSISTENT;

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
