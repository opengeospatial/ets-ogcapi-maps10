package org.opengis.cite.ogcapimaps10.domain;

/**
 * Wraps the results from interactive tests for TIFF content verification.
 *
 * <p>
 * This class holds the boolean results of interactive tests that require manual
 * verification by the user for the TIFF conformance class (/conf/tiff/content):
 * </p>
 * <ul>
 * <li>Part C: Whether maps representing parts of the same resource follow the same
 * portrayal rules or represent data with the same reference and units of measure</li>
 * </ul>
 *
 * <p>
 * Note: Part B (palette/RGB color model) is fully automated and does not require
 * interactive verification.
 * </p>
 */
public class TiffInteractiveTestResult {

	private final boolean enabled;

	private final boolean portrayalConsistent;

	/**
	 * Constructs a TiffInteractiveTestResult with the specified test results.
	 * @param enabled true if interactive tests were enabled by the user
	 * @param portrayalConsistent true if maps of the same resource follow the same
	 * portrayal rules (Part C)
	 */
	public TiffInteractiveTestResult(boolean enabled, boolean portrayalConsistent) {
		this.enabled = enabled;
		this.portrayalConsistent = portrayalConsistent;
	}

	/**
	 * Returns whether interactive tests were enabled by the user.
	 * @return true if the user checked the "Run interactive tests" checkbox
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Returns whether maps of the same resource follow the same portrayal rules or
	 * represent data with the same reference and units of measure (Part C).
	 * @return true if the interactive verification passed
	 */
	public boolean isPortrayalConsistent() {
		return portrayalConsistent;
	}

}
