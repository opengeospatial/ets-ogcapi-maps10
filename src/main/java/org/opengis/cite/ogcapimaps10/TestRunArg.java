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
	 * Whether the interactive filter-application verification for A.30 Req 30/A is
	 * enabled. Set to {@code true} to activate; omit or set to {@code false} to skip.
	 */
	DATETIME_SUBSET_RESPONSE_INTERACTIVE_ENABLED,

	/**
	 * The tester's answer for A.30 Req 30/A: {@code true} if the subset-filtered map
	 * visually differs from the full-extent map (proving the temporal filter was
	 * applied), {@code false} otherwise. Only read when
	 * {@link #DATETIME_SUBSET_RESPONSE_INTERACTIVE_ENABLED} is {@code true}.
	 */
	DATETIME_SUBSET_RESPONSE_RESULT_CORRECT;

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
