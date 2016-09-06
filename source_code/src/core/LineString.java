package core;

/**
 * Two coordinates which make up a line
 * (and also a rectangle).
 */
public class LineString implements Comparable<LineString>{

	/** 
	 * When checking if a point is on the line, this is the
	 * difference allowed between a point's actual y and 
	 * the y calculated using this line's formula.  If the 
	 * difference is within this tolerance than the point
	 * is still considered "on the line".
	 */
	private final static double EPSILON = .05;

	private Coord begin;
	private Coord end;
	
	/** slope from line formula: a*x + b = y */
	private double slope;
	/** y-intercept line formula: a*x + b = y */
	private double yIntercept;
	
	public LineString(Coord beginArg, Coord endArg) {
		this.begin = beginArg;
		this.end = endArg;
		calculateSlopeAndYIntercept();
	}
	
	private void calculateSlopeAndYIntercept() {
		slope = (end.getY() - begin.getY()) / (end.getX() - begin.getX());
		yIntercept = begin.getY() - (slope * begin.getX());
	}

	public Coord getBegin() {
		return begin;
	}

	public void setBegin(Coord begin) {
		this.begin = begin;
		calculateSlopeAndYIntercept();
	}

	public Coord getEnd() {
		return end;
	}

	public void setEnd(Coord end) {
		this.end = end;
		calculateSlopeAndYIntercept();
	}
	
	public double getSlope() {
		return slope;
	}

	public double getYIntercept() {
		return yIntercept;
	}
	
	public Coord getCoordOnLineForX(double x) {
		double y = (x * this.slope) + this.yIntercept;
		Coord newCoord = new Coord(x, y);
		if (begin.getX() < end.getX()) {
			if ((x < begin.getX()) || (x > end.getX())) {
				newCoord.setLocation(end);
			}
		} else {
			if ((x < end.getX()) || (x > begin.getX())) {
				newCoord.setLocation(begin);
			}
		}
		return newCoord;
	}
	
	/**
	 * Two points can also define a rectangle.  Checks if the point argument
	 * is on or in the rectangle formed by the begin and end points of this line.
	 * This may return true in cases where isOnLine returns false, i.e.
	 * if the point is in the bounding box, but not actually on the line.
	 * @param point to check
	 * @return True if the point is within the bounding box
	 */
	public boolean isInBoundingBox(Coord point) {
		return ( ( ( (begin.getX() <= point.getX())
					  && (point.getX() <= end.getX()) ) ||
				   ( (end.getX() <= point.getX())
					  && (point.getX() <= begin.getX()) )
				 )  &&
				 ( ( (begin.getY() <= point.getY())
				      && (point.getY() <= end.getY()) ) ||
				   ( (end.getY() <= point.getY())
				      && (point.getY() <= begin.getY()) )
				 )
			   );
	}
	
	/**
	 * Checks if the point argument is on the line defined by this object.
	 * @param point to check
	 * @return True if the point is on the line
	 */
	public boolean contains(Coord point) {
		// if (!isInBoundingBox(point)) {
		// 	return false;
		// }
		double shouldBeY = (this.slope * point.getX()) + this.yIntercept;
		return (Math.abs(shouldBeY - point.getY()) < EPSILON);
		/*
		if (begin.getX() <= end.getX()) {
			return (begin.getX() <= point.getX()) && (point.getX() <= end.getX());
		} else {
			return (end.getX() <= point.getX()) && (point.getX() <= begin.getX());
		}
		*/
	}
	
	/**
	 * Two routes overlap if the first route contains the second or there's an intersection
	 * So: if the curr of the second line is between the curr and dest of the first AND
	 * EITHER the dest of the first line is between the curr and dest of the second OR
	 * the dest of the second line is between the curr of the second line and the dest of the first
	 * i.e. second trip is a subset of the first, but traveling in the correct direction
	 */
	/* This was the first naive solution; Actually it was one of many failed attempts.
	public boolean overlapsInSameDir(LineString other) {
		System.out.println("this.isOnLine(other.begin) = " + this.isOnLine(other.begin)); 
		System.out.println("(other.isOnLine(this.end) = " + other.isOnLine(this.end)); 
		System.out.println("new LineString(other.begin, this.end).isOnLine(other.end)) +"
				+ new LineString(other.begin, this.end).isOnLine(other.end));
		return this.isOnLine(other.begin) && 
			   (other.isOnLine(this.end) ||
			   new LineString(other.begin, this.end).isOnLine(other.end));
	}
	*/
	
	/**
	 * Checks if two lines intersect
	 * 
	 */
	public boolean intersects(LineString other) {
		return java.awt.geom.Line2D.linesIntersect(
				begin.getX(), begin.getY(), end.getX(), end.getY(), 
				other.begin.getX(), other.begin.getY(), other.end.getX(), other.end.getY());
	}

	@Override
	public String toString() {
		return "LineString [begin = " + begin + 
				"\n\tend = " + end + 
				"\n\tline formula = y = ((a=" + slope + ") * x) + (b = " + yIntercept + ") ]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((begin == null) ? 0 : begin.hashCode());
		result = prime * result + ((end == null) ? 0 : end.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof LineString)) {
			return false;
		}
		LineString other = (LineString) obj;
		if (begin == null) {
			if (other.begin != null) {
				return false;
			}
		} else if (!begin.equals(other.begin)) {
			return false;
		}
		if (end == null) {
			if (other.end != null) {
				return false;
			}
		} else if (!end.equals(other.end)) {
			return false;
		}
		return true;
	}

	public int compareTo(LineString other) {
		int beginCompare = this.begin.compareTo(other.begin);
		if (beginCompare == 0) {
			return this.end.compareTo(other.end);
		} else {
			return beginCompare;
		}
	}
	
	/**
	 * Convenience method to check if a point is on a line
	 * @param point Coord that is or is not on the line
	 * @param begin Coord of the line; begin and end can be in any order
	 * @param end Coord of the line; begin and end can be in any order
	 * @return True if the point is on the line defined by begin and end
	 */
	public final static boolean isPointOnLine(Coord point, Coord begin, Coord end) {
		return new LineString(begin, end).contains(point);
	}
	
	
	public final static void test(double x, double y, double x2, double y2, double ptx, double pty) {
		Coord begin = new Coord(x, y);
		Coord end = new Coord(x2, y2);
		LineString ls = new LineString(begin, end);
		System.out.println("begin = " + begin);
		System.out.println("end = " + end);
		System.out.println("slope = " + ls.getSlope());
		System.out.println("YIntercept = " + ls.getYIntercept());
		System.out.println("linestring = " + ls);
		Coord point = new Coord(ptx, pty);
		System.out.println("point = " + point);
		System.out.println("isInBoundingBox = " + ls.isInBoundingBox(point));
		System.out.println("isOnLine = " + ls.contains(point));
		System.out.println("");
	}

	public static void main(String[] args) {
		MovableObject thismo = null;
		MovableObject othermo = null;
		LineString lsMe = null; 
		LineString lsOther = null; 
		LineString lsRoute = null; 
		
		LineString x = new LineString(new Coord(229.68,347.05), new Coord(189.29,337.82));
		System.out.println(x);
		System.out.println(x.getCoordOnLineForX(223.04));

		
		LineString y = new LineString(new Coord(223.04,345.54), new Coord(189.29,337.82));
		System.out.println(y);
		System.out.println(x.getCoordOnLineForX(222.71));
				
		/*
		// Moving forward example
		thismo = new MovableObject();
		thismo.setStartingLoc(new Coord(628.72,446.90));
		thismo.setEndingLoc(new Coord(628.82,447.09));
		System.out.println("thismo = " + thismo);

		othermo = new MovableObject();
		othermo.setStartingLoc(new Coord(628.79, 446.99));
		othermo.setEndingLoc(new Coord(629.07, 447.54));
		System.out.println("othermo = " + othermo);

		lsMe = new LineString(thismo.getStartingLoc(), thismo.getEndingLoc());
		System.out.println("lsMe = " + lsMe);
		lsOther = new LineString(othermo.getStartingLoc(), othermo.getEndingLoc());
		System.out.println("lsOther = " + lsOther);
		
		System.out.println("Overlaps in same direction? " + lsMe.overlapsInSameDir(lsOther));
		*/
		/*

		// Moving backward example
		thismo = new MovableObject();
		thismo.setStartingLoc(new Coord(626.53,442.76)); //629.07, 447.54));
		thismo.setEndingLoc(new Coord(626.26,442.21)); //628.79, 446.99));
		System.out.println("thismo = " + thismo);

		othermo = new MovableObject();
		othermo.setStartingLoc(new Coord(629.46,448.29)); //628.82,447.09));
		othermo.setEndingLoc(new Coord(626.21,442.17)); //628.72,446.90));
		System.out.println("othermo = " + othermo);

		lsMe = new LineString(thismo.getStartingLoc(), thismo.getEndingLoc());
		System.out.println("lsMe = " + lsMe);
		lsOther = new LineString(othermo.getStartingLoc(), othermo.getEndingLoc());
		System.out.println("lsOther = " + lsOther);
		
		lsRoute = new LineString(new Coord(629.46,448.29), new Coord(625.45,440.53));
		System.out.println("lsRoute = " + lsRoute);
		System.out.println("\tthis.StartingLoc isOnLine = " + lsRoute.contains(thismo.getStartingLoc()));
		System.out.println("\tthis.EndingLoc isOnLine = " + lsRoute.contains(thismo.getEndingLoc()));
		System.out.println("\tother.StartingLoc isOnLine = " + lsRoute.contains(othermo.getStartingLoc()));
		System.out.println("\tother.EndingLoc isOnLine = " + lsRoute.contains(othermo.getEndingLoc()));
		
		/*
		boolean is = LineString.isPointOnLineButNotEnd(new Coord(246.76,351.30), new Coord(245.71,351.05), new Coord(246.66,351.27));
		System.out.println("is = " + is);
		Coord lsStart = new Coord(234.66,348.36);
		Coord lsEnd = new Coord(259.13,354.30);
		System.out.println("distance 1 = " + lsStart.distance(new Coord(245.71,351.05)));
		System.out.println("distance 2 = " + lsStart.distance(new Coord(246.76,351.30)));
		System.out.println("distance 3 = " + lsStart.distance(new Coord(246.66,351.27)));
		
		
		test(1, 2, 5, 4, 2, 2.5);
		test(1, 2, 5, 4, 2, 7);

		test(1, 4, 5, 2, 2, 2.5);
		test(1, 4, 5, 2, 2, 7);

		test(5, 2, 1, 4, 2, 2.5);
		test(5, 2, 1, 4, 2, 7);

		test(5, 4, 1, 2, 2, 2.5);
		test(5, 4, 1, 2, 2, 7);

		test(5, 4, 1, 2, 5, 4);
		test(5, 4, 1, 2, 1, 2);
		*/
	}
}
