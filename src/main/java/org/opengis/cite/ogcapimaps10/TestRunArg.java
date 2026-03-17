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
	 * Whether the interactive verification for A.14 default height (Req 14/H) was enabled
	 * by the tester.
	 */
	SCALING_HEIGHT_INTERACTIVE_ENABLED,

	/**
	 * Whether the tester confirmed that the server uses an appropriate default height
	 * when the height parameter is omitted (Req 14/H).
	 */
	SCALING_HEIGHT_DEFAULT_APPROPRIATE;

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
