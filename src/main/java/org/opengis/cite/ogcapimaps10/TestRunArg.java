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
	 * Whether interactive map-success tests are enabled (true/false).
	 */
	MAP_SUCCESS_INTERACTIVE_ENABLED,

	/**
	 * Whether the user confirmed that the map rendered with mm-per-pixel=0.14 looks
	 * visually different from the map rendered with mm-per-pixel=0.28 (true/false).
	 */
	MAP_SUCCESS_CORRECT;

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
