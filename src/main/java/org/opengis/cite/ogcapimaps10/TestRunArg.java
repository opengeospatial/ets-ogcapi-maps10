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
	 * Whether the interactive verification for A.15 scale-denominator (Req 15/B, C, G)
	 * was enabled by the tester.
	 */
	SCALING_SCALE_DENOMINATOR_INTERACTIVE_ENABLED,

	/**
	 * Whether the tester confirmed that the server correctly interprets the
	 * scale-denominator parameter (Req 15/B, C, G).
	 */
	SCALING_SCALE_DENOMINATOR_RESULT_APPROPRIATE;

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
