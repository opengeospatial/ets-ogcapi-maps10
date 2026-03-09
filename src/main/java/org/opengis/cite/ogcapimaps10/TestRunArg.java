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
	 * Whether interactive tests were enabled by the user. true if the user checked the
	 * "Run interactive tests" checkbox.
	 */
	INTERACTIVE_TESTS_ENABLED,

	/**
	 * Result of interactive test for A.12 collections-response (Req 12B). true if the
	 * user confirmed that two maps with reversed collection order look visually different
	 * (i.e., rendering order is respected).
	 */
	COLLECTIONS_RESPONSE_CORRECT;

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
