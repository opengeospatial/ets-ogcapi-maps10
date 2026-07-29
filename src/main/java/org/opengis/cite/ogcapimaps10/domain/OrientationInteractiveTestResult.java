package org.opengis.cite.ogcapimaps10.domain;

/**
 * Holds the outcome of the five interactive visual verifications for Abstract Test A.37
 * (orientation parameter). Set by {@code SuiteFixtureListener} and consumed by
 * {@code OrientationDefinitionTest}.
 *
 * <p>
 * Each field corresponds to one interactive row from the assertion table:
 * <ul>
 * <li>{@code row5RotationCcw} — Req 37/A: Map B (orientation=45) appears rotated 45° CCW
 * relative to Map A.</li>
 * <li>{@code row7DefaultEqualsZero} — Req 37/B: Map with no orientation parameter is
 * visually identical to orientation=0.</li>
 * <li>{@code row8PivotCenter} — Req 37/C: rotation pivot is the center of the map extent
 * (no bbox).</li>
 * <li>{@code row9PivotBboxCenter} — Req 37/C: rotation pivot is the geographic center of
 * the bbox (only if spatial subsetting is supported).</li>
 * <li>{@code row11NoEmptyCorners} — Req 37/D: rotated bbox map has no empty/transparent
 * corners (only if spatial subsetting is supported).</li>
 * </ul>
 */
public class OrientationInteractiveTestResult {

	private final boolean enabled;

	private final boolean row5RotationCcw;

	private final boolean row7DefaultEqualsZero;

	private final boolean row8PivotCenter;

	private final boolean row9PivotBboxCenter;

	private final boolean row11NoEmptyCorners;

	public OrientationInteractiveTestResult(boolean enabled, boolean row5RotationCcw, boolean row7DefaultEqualsZero,
			boolean row8PivotCenter, boolean row9PivotBboxCenter, boolean row11NoEmptyCorners) {
		this.enabled = enabled;
		this.row5RotationCcw = row5RotationCcw;
		this.row7DefaultEqualsZero = row7DefaultEqualsZero;
		this.row8PivotCenter = row8PivotCenter;
		this.row9PivotBboxCenter = row9PivotBboxCenter;
		this.row11NoEmptyCorners = row11NoEmptyCorners;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public boolean isRow5RotationCcw() {
		return row5RotationCcw;
	}

	public boolean isRow7DefaultEqualsZero() {
		return row7DefaultEqualsZero;
	}

	public boolean isRow8PivotCenter() {
		return row8PivotCenter;
	}

	public boolean isRow9PivotBboxCenter() {
		return row9PivotBboxCenter;
	}

	public boolean isRow11NoEmptyCorners() {
		return row11NoEmptyCorners;
	}

}
