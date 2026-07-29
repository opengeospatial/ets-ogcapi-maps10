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
	TIFF_PORTRAYAL_CONSISTENT,

	/**
	 * Whether the interactive verification for A.37 orientation (Req 37/A-D) was enabled
	 * by the tester.
	 */
	ORIENTATION_INTERACTIVE_ENABLED,

	/**
	 * Row 5 (Req 37/A): tester confirmed Map B (orientation=45) appears rotated 45
	 * degrees CCW relative to Map A (default north-up).
	 */
	ORIENTATION_RESULT_ROW5_ROTATION_CCW,

	/**
	 * Row 7 (Req 37/B): tester confirmed the map with no orientation parameter appears
	 * visually identical to orientation=0.
	 */
	ORIENTATION_RESULT_ROW7_DEFAULT_EQUALS_ZERO,

	/**
	 * Row 8 (Req 37/C): tester confirmed the rotation pivot is the center of the map
	 * extent when no bbox is specified.
	 */
	ORIENTATION_RESULT_ROW8_PIVOT_CENTER,

	/**
	 * Row 9 (Req 37/C): tester confirmed the rotation pivot is the geographic center of
	 * the specified bbox (only applicable when spatial subsetting is supported).
	 */
	ORIENTATION_RESULT_ROW9_PIVOT_BBOX_CENTER,

	/**
	 * Row 11 (Req 37/D): tester confirmed the rotated bbox map has no empty or
	 * transparent corners (only applicable when spatial subsetting is supported).
	 */
	ORIENTATION_RESULT_ROW11_NO_EMPTY_CORNERS;

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
