package org.opengis.cite.ogcapimaps10.domain;

/**
 * Wraps the results from interactive tests for collections response verification (A.36).
 *
 * <p>
 * This class holds the boolean results of interactive tests that require manual
 * verification by the user for the Collections Selection conformance class
 * (/conf/collections-selection):
 * </p>
 * <ul>
 * <li>Req 36/A: Whether the filtered map (collections=id1) visually differs from the
 * default map (no collections parameter), proving only the specified collection is
 * rendered.</li>
 * <li>Req 36/B: Whether the forward-order map (collections=id1,id2) visually differs from
 * the reverse-order map (collections=id2,id1), proving the server respects left-to-right
 * draw order.</li>
 * </ul>
 */
public class CollectionsResponseInteractiveTestResult {

	private final boolean enabled;

	private final boolean filterAppliedCorrect;

	private final boolean drawOrderCorrect;

	/**
	 * Constructs a CollectionsResponseInteractiveTestResult with the specified results.
	 * @param enabled true if interactive tests were enabled by the user
	 * @param filterAppliedCorrect true if the tester confirmed the filtered map visually
	 * differs from the default map (Req 36/A)
	 * @param drawOrderCorrect true if the tester confirmed the forward-order map differs
	 * from the reverse-order map in layer stacking (Req 36/B)
	 */
	public CollectionsResponseInteractiveTestResult(boolean enabled, boolean filterAppliedCorrect,
			boolean drawOrderCorrect) {
		this.enabled = enabled;
		this.filterAppliedCorrect = filterAppliedCorrect;
		this.drawOrderCorrect = drawOrderCorrect;
	}

	/**
	 * Returns whether interactive tests were enabled by the user.
	 * @return true if the user checked the interactive verification checkbox
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Returns whether the tester confirmed the filtered map differs from the default map
	 * (Req 36/A).
	 * @return true if the interactive verification for Req 36/A passed
	 */
	public boolean isFilterAppliedCorrect() {
		return filterAppliedCorrect;
	}

	/**
	 * Returns whether the tester confirmed the forward-order map differs from the
	 * reverse-order map in draw order (Req 36/B).
	 * @return true if the interactive verification for Req 36/B passed
	 */
	public boolean isDrawOrderCorrect() {
		return drawOrderCorrect;
	}

}
