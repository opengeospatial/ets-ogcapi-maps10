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
	 * Whether interactive tests for JPEG XL content were enabled by the user. true if the
	 * user checked the "Run JPEG XL interactive tests" checkbox, false otherwise.
	 */
	JPEGXL_INTERACTIVE_TESTS_ENABLED,

	/**
	 * Result of interactive test for JPEG XL Part B: whether the JPEG XL is a color image
	 * correctly representing geospatial features or coverage values. true if passed,
	 * false otherwise.
	 */
	JPEGXL_COLORS_REPRESENT_FEATURES,

	/**
	 * Result of interactive test for JPEG XL Part C: whether maps representing parts of
	 * the same resource follow the same portrayal rules. true if passed, false otherwise.
	 */
	JPEGXL_PORTRAYAL_CONSISTENT;

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
