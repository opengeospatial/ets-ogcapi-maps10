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
	 * Whether interactive display-resolution tests are enabled (true/false).
	 */
	DISPLAY_RESOLUTION_INTERACTIVE_ENABLED,

	/**
	 * Whether the user confirmed that maps rendered at different mm-per-pixel values look
	 * visually different (true/false).
	 */
	MM_PER_PIXEL_CORRECT,

	/**
	 * Whether the user confirmed that a map without mm-per-pixel looks the same as a map
	 * with mm-per-pixel=0.28, verifying the default assumption (true/false).
	 */
	MM_PER_PIXEL_DEFAULT_CORRECT;

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
