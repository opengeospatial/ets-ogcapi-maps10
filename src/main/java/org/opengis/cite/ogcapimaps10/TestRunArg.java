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
	 * Whether interactive tests for TIFF content were enabled by the user. true if the
	 * user checked the "Run TIFF interactive tests" checkbox, false otherwise.
	 */
	TIFF_INTERACTIVE_TESTS_ENABLED,

	/**
	 * Result of interactive test for TIFF Part C: whether maps representing parts of the
	 * same resource follow the same portrayal rules or represent data with the same
	 * reference and units of measure. true if passed, false otherwise.
	 */
	TIFF_PORTRAYAL_CONSISTENT;

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
