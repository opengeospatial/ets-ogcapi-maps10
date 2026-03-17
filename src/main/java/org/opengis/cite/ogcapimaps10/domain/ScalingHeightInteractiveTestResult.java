package org.opengis.cite.ogcapimaps10.domain;

/**
 * Holds the pre-collected result of the interactive (manual) verification for Abstract
 * Test A.14 (height-definition, Req 14/H): whether the server uses an appropriate default
 * height when the {@code height} parameter is omitted.
 */
public class ScalingHeightInteractiveTestResult {

	private final boolean enabled;

	private final boolean heightDefaultAppropriate;

	/**
	 * Creates a new result object.
	 * @param enabled whether the interactive verification was enabled by the tester
	 * @param heightDefaultAppropriate whether the tester confirmed that the server uses
	 * an appropriate default height when the height parameter is omitted
	 */
	public ScalingHeightInteractiveTestResult(boolean enabled, boolean heightDefaultAppropriate) {
		this.enabled = enabled;
		this.heightDefaultAppropriate = heightDefaultAppropriate;
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
	 * height when the {@code height} parameter is omitted.
	 * @return {@code true} if the default height was reported as appropriate
	 */
	public boolean isHeightDefaultAppropriate() {
		return heightDefaultAppropriate;
	}

}
