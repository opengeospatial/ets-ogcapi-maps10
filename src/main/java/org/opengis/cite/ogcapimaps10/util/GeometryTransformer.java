package org.opengis.cite.ogcapimaps10.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.CoordinateTransformFactory;
import org.locationtech.proj4j.ProjCoordinate;

/**
 * <p>
 * GeometryTransformer class.
 * </p>
 *
 * @author <a href="mailto:goltz@lat-lon.de">Lyn Goltz </a>
 */
public class GeometryTransformer {

	private final GeometryFactory geometryFactory = new GeometryFactory();

	private final CoordinateTransform transformer;

	private final CoordinateSystem srcCrs;

	private final CoordinateSystem targetCrs;

	/**
	 * <p>
	 * Constructor for GeometryTransformer.
	 * </p>
	 * @param srcCrs source crs, , never <code>null</code>
	 * @param targetCrs target crs, , never <code>null</code>
	 */
	public GeometryTransformer(CoordinateSystem srcCrs, CoordinateSystem targetCrs) {
		this.srcCrs = srcCrs;
		this.targetCrs = targetCrs;
		CoordinateTransformFactory coordinateTransformFactory = new CoordinateTransformFactory();
		CRSFactory crsFactory = new CRSFactory();
		this.transformer = coordinateTransformFactory.createTransform(
				crsFactory.createFromName(srcCrs.getCodeWithAuthority()),
				crsFactory.createFromName(targetCrs.getCodeWithAuthority()));
	}

	/**
	 * <p>
	 * transform.
	 * </p>
	 * @param bbox the bbox to transform, never <code>null</code>
	 * @return the transformed bbox (or the same if srcCrs and targetCrs) are the same,
	 * never <code>null</code>
	 */
	public BBox transform(BBox bbox) {
		if (srcCrs.equals(targetCrs))
			return bbox;
		Coordinate min = new Coordinate(bbox.getMinX(), bbox.getMinY());
		Coordinate max = new Coordinate(bbox.getMaxX(), bbox.getMaxY());
		Coordinate transformedMin = transform(min);
		Coordinate transformedMax = transform(max);
		return new BBox(transformedMin.x, transformedMin.y, transformedMax.x, transformedMax.y, targetCrs);
	}

	/**
	 * <p>
	 * transform.
	 * </p>
	 * @param geometryToTransform a {@link org.locationtech.jts.geom.Geometry} object
	 * @return a {@link org.locationtech.jts.geom.Geometry} object
	 */
	public Geometry transform(Geometry geometryToTransform) {
		if (geometryToTransform == null)
			return null;
		if (geometryToTransform instanceof Point) {
			return transform((Point) geometryToTransform);
		}
		else if (geometryToTransform instanceof Polygon) {
			return transform((Polygon) geometryToTransform);
		}
		else if ((geometryToTransform instanceof LineString)) {
			return transform((LineString) geometryToTransform);
		}
		else if ((geometryToTransform instanceof GeometryCollection)) {
			return transform((GeometryCollection) geometryToTransform);
		}
		throw new IllegalArgumentException("Unsupported geometry type: " + geometryToTransform.getClass());
	}

	/**
	 * <p>
	 * transform.
	 * </p>
	 * @param geometryToTransform a {@link org.locationtech.jts.geom.Point} object
	 * @return a {@link org.locationtech.jts.geom.Point} object
	 */
	public Point transform(Point geometryToTransform) {
		Coordinate coordinate = geometryToTransform.getCoordinate();
		Coordinate transformedCoordinate = transform(coordinate);
		return geometryFactory.createPoint(transformedCoordinate);
	}

	/**
	 * <p>
	 * transform.
	 * </p>
	 * @param geometryToTransform a {@link org.locationtech.jts.geom.LineString} object
	 * @return a {@link org.locationtech.jts.geom.LineString} object
	 */
	public LineString transform(LineString geometryToTransform) {
		Coordinate[] coordinates = geometryToTransform.getCoordinates();
		Coordinate[] transformedCoordinates = transform(coordinates);
		return geometryFactory.createLineString(transformedCoordinates);
	}

	/**
	 * <p>
	 * transform.
	 * </p>
	 * @param geometryToTransform a {@link org.locationtech.jts.geom.Polygon} object
	 * @return a {@link org.locationtech.jts.geom.Polygon} object
	 */
	public Polygon transform(Polygon geometryToTransform) {
		Coordinate[] coordinatesExteriorRing = geometryToTransform.getExteriorRing().getCoordinates();
		Coordinate[] transformedCoordinatesExteriorRing = transform(coordinatesExteriorRing);
		LinearRing exteriorRing = geometryFactory.createLinearRing(transformedCoordinatesExteriorRing);
		LinearRing[] interiorRings = new LinearRing[geometryToTransform.getNumInteriorRing()];
		for (int numInteriorRing = 0; numInteriorRing < geometryToTransform.getNumInteriorRing(); numInteriorRing++) {
			LinearRing interiorRingN = geometryToTransform.getInteriorRingN(numInteriorRing);
			Coordinate[] transformedCoordinatesInteriorRingN = transform(interiorRingN.getCoordinates());
			interiorRings[numInteriorRing] = geometryFactory.createLinearRing(transformedCoordinatesInteriorRingN);
		}
		return geometryFactory.createPolygon(exteriorRing, interiorRings);
	}

	/**
	 * <p>
	 * transform.
	 * </p>
	 * @param geometryToTransform a {@link org.locationtech.jts.geom.GeometryCollection}
	 * object
	 * @return a {@link org.locationtech.jts.geom.Geometry} object
	 */
	public Geometry transform(GeometryCollection geometryToTransform) {
		if (geometryToTransform instanceof MultiPoint) {
			return transform((MultiPoint) geometryToTransform);
		}
		else if (geometryToTransform instanceof MultiLineString) {
			return transform((MultiLineString) geometryToTransform);
		}
		else if (geometryToTransform instanceof MultiPolygon) {
			return transform((MultiPolygon) geometryToTransform);
		}
		throw new IllegalArgumentException("Unsupported geometry type: " + geometryToTransform.getClass());
	}

	/**
	 * <p>
	 * transform.
	 * </p>
	 * @param geometryToTransform a {@link org.locationtech.jts.geom.MultiPoint} object
	 * @return a {@link org.locationtech.jts.geom.MultiPoint} object
	 */
	public MultiPoint transform(MultiPoint geometryToTransform) {
		Point[] points = new Point[geometryToTransform.getNumGeometries()];
		for (int numGeometry = 0; numGeometry < geometryToTransform.getNumGeometries(); numGeometry++) {
			Geometry geometryN = geometryToTransform.getGeometryN(numGeometry);
			points[numGeometry] = transform((Point) geometryN);
		}
		return geometryFactory.createMultiPoint(points);
	}

	/**
	 * <p>
	 * transform.
	 * </p>
	 * @param geometryToTransform a {@link org.locationtech.jts.geom.MultiLineString}
	 * object
	 * @return a {@link org.locationtech.jts.geom.MultiLineString} object
	 */
	public MultiLineString transform(MultiLineString geometryToTransform) {
		LineString[] lineStrings = new LineString[geometryToTransform.getNumGeometries()];
		for (int numGeometry = 0; numGeometry < geometryToTransform.getNumGeometries(); numGeometry++) {
			Geometry geometryN = geometryToTransform.getGeometryN(numGeometry);
			lineStrings[numGeometry] = transform((LineString) geometryN);
		}
		return geometryFactory.createMultiLineString(lineStrings);

	}

	/**
	 * <p>
	 * transform.
	 * </p>
	 * @param geometryToTransform a {@link org.locationtech.jts.geom.MultiPolygon} object
	 * @return a {@link org.locationtech.jts.geom.MultiPolygon} object
	 */
	public MultiPolygon transform(MultiPolygon geometryToTransform) {
		Polygon[] polygons = new Polygon[geometryToTransform.getNumGeometries()];
		for (int numGeometry = 0; numGeometry < geometryToTransform.getNumGeometries(); numGeometry++) {
			Geometry geometryN = geometryToTransform.getGeometryN(numGeometry);
			polygons[numGeometry] = transform((Polygon) geometryN);
		}
		return geometryFactory.createMultiPolygon(polygons);
	}

	private Coordinate[] transform(Coordinate[] coordinates) {
		List<Coordinate> transformedCoordinates = Arrays.stream(coordinates)
			.map(coord -> transform(coord))
			.collect(Collectors.toList());
		return transformedCoordinates.toArray(new Coordinate[transformedCoordinates.size()]);
	}

	private Coordinate transform(Coordinate coord) {
		ProjCoordinate srcCoordinate = new ProjCoordinate(coord.x, coord.y, coord.z);
		ProjCoordinate targetCoordinate = new ProjCoordinate();
		transformer.transform(srcCoordinate, targetCoordinate);
		return new Coordinate(targetCoordinate.x, targetCoordinate.y, targetCoordinate.z);
	}

}
