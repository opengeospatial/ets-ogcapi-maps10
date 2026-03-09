package org.opengis.cite.ogcapimaps10.domain;

/**
 * Holds the results of interactive (manual) verification tests.
 *
 * <p>
 * Interactive tests require a human to visually inspect map responses and confirm that
 * the server behaves correctly. The results are collected by the CTL frontend before the
 * TestNG suite runs and passed in as suite parameters.
 * </p>
 */
public class InteractiveTestResult {

	private final boolean enabled;

	private final boolean collectionsResponseCorrect;

	/**
	 * Constructs an InteractiveTestResult with the specified results.
	 * @param enabled true if interactive tests were enabled by the user
	 * @param collectionsResponseCorrect true if the user confirmed that two maps with
	 * reversed collection order look visually different (Req 12B)
	 */
	public InteractiveTestResult(boolean enabled, boolean collectionsResponseCorrect) {
		this.enabled = enabled;
		this.collectionsResponseCorrect = collectionsResponseCorrect;
	}

	/**
	 * Returns whether interactive tests were enabled by the user.
	 * @return true if the user checked the "Run interactive tests" checkbox
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Returns whether the A.12 collections-response interactive test passed (Req 12B).
	 * @return true if the user confirmed that rendering order is respected
	 */
	public boolean isCollectionsResponseCorrect() {
		return collectionsResponseCorrect;
	}

}