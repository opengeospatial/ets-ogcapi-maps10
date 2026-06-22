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
	 * The TileMatrixSet to use for tile requests. Supported values are "WebMercatorQuad"
	 * and "WorldCRS84Quad". Default is "WebMercatorQuad".
	 * @see <a href="https://docs.ogc.org/is/17-083r4/17-083r4.html#toc49">OGC Two
	 * Dimensional Tile Matrix Set and Tile Set Metadata</a>
	 */
	TILE_MATRIX_SET,
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
	PNG_PORTRAYAL_CONSISTENT,

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
	JPEG_PORTRAYAL_CONSISTENT,

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
