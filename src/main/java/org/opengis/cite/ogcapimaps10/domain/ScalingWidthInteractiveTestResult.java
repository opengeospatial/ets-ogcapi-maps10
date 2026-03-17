package org.opengis.cite.ogcapimaps10.domain;

/**
 * Holds the pre-collected result of the interactive (manual) verification for Abstract
 * Test A.13 (width-definition, Req 13/H): whether the server uses an appropriate default
 * width when the {@code width} parameter is omitted.
 */
public class ScalingWidthInteractiveTestResult {

	private final boolean enabled;

	private final boolean widthDefaultAppropriate;

	/**
	 * Creates a new result object.
	 * @param enabled whether the interactive verification was enabled by the tester
	 * @param widthDefaultAppropriate whether the tester confirmed that the server uses an
	 * appropriate default width when the width parameter is omitted
	 */
	public ScalingWidthInteractiveTestResult(boolean enabled, boolean widthDefaultAppropriate) {
		this.enabled = enabled;
		this.widthDefaultAppropriate = widthDefaultAppropriate;
	}

	/**
	 * Returns whether the interactive verification was enabled.
	 * @return {@code true} if enabled
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Returns whether the tester confirmed that the server uses an appropriate default
	 * width when the {@code width} parameter is omitted.
	 * @return {@code true} if the default width was reported as appropriate
	 */
	public boolean isWidthDefaultAppropriate() {
		return widthDefaultAppropriate;
	}

}
