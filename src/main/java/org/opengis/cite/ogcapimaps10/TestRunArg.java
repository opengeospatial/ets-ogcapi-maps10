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
	 * Whether the interactive verification for A.13 default width (Req 13/H) was enabled
	 * by the tester.
	 */
	SCALING_WIDTH_INTERACTIVE_ENABLED,

	/**
	 * Whether the tester confirmed that the server uses an appropriate default width when
	 * the width parameter is omitted (Req 13/H).
	 */
	SCALING_WIDTH_DEFAULT_APPROPRIATE;

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
