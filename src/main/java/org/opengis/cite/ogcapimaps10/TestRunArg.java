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
	 * Whether interactive tests for tiles-parameters were enabled by the user. true if
	 * the user checked the "Run interactive tests" checkbox, false otherwise.
	 */
	INTERACTIVE_TESTS_ENABLED,

	/**
	 * Result of interactive test for bgcolor parameter on tiles. true if the tile
	 * displayed the correct background color, false otherwise.
	 */
	TILES_BGCOLOR_CORRECT,

	/**
	 * Result of interactive test for transparent parameter on tiles. true if the tile
	 * displayed correct transparency, false otherwise.
	 */
	TILES_TRANSPARENT_CORRECT,

	/**
	 * Result of interactive test for mm-per-pixel (display resolution) parameter on
	 * tiles. true if the tile displayed at the correct resolution, false otherwise.
	 */
	TILES_DISPLAY_RESOLUTION_CORRECT,

	/**
	 * Result of interactive test for subset parameter on tiles. true if the tile
	 * correctly showed the specified subset area, false otherwise.
	 */
	TILES_SUBSET_CORRECT,

	/**
	 * The TileMatrixSet to use for tile requests. Supported values are "WebMercatorQuad"
	 * and "WorldCRS84Quad". Default is "WebMercatorQuad".
	 * @see <a href="https://docs.ogc.org/is/17-083r4/17-083r4.html#toc49">OGC Two
	 * Dimensional Tile Matrix Set and Tile Set Metadata</a>
	 */
	TILE_MATRIX_SET;

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
