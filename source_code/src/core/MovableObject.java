package core;

import java.util.*;

public class MovableObject {

	private final static String EOLN = System.getProperty("line.separator");
	
	private DTNHost host = null;
	private Coord startingLoc = null;
	private Coord endingLoc = null;
	private LineString moveLine = null;
	private double speed = 0;
	private double intervalTime = 0;
	/** The full path the vehicle is following */
	private List<Coord> fullPath = new ArrayList<Coord>();
	/** The path for this time increment */
	private List<LineString> movePath = new ArrayList<LineString>();

	public MovableObject() { }
	
	public MovableObject(DTNHost hostArg, Coord startingLocArg, Coord endingLocArg, 
		double speedArg, double intervalTimeArg, List<Coord> fullPathArg) {
		this.host = hostArg;
		this.startingLoc = startingLocArg;
		this.endingLoc = endingLocArg;
		calculateMoveLine();
		this.speed = speedArg;
		this.intervalTime = intervalTimeArg;
		if (fullPathArg != null) {
			this.fullPath = fullPathArg;
		}
		calculateMovePath();
	}
	
	public void calculateMovePath() {
		this.movePath.clear();
		if (this.fullPath.isEmpty()) {
			return; // we're in a bad state
		}
		int startIdx = 0;
		while ((startIdx < (this.fullPath.size() - 1)) && 
			!(new LineString(this.fullPath.get(startIdx), this.fullPath.get(startIdx + 1)).contains(startingLoc))) {
			startIdx++;
		}
		if (startIdx == (this.fullPath.size() - 1)) {
			return;
		}
		int endIdx = startIdx;
		do {
			this.movePath.add(new LineString(this.fullPath.get(startIdx), this.fullPath.get(startIdx + 1)));
			endIdx++;
		} while ((endIdx < (this.fullPath.size() - 1)) && 
			!(new LineString(this.fullPath.get(startIdx), this.fullPath.get(startIdx + 1)).contains(startingLoc)));
	}
	
	public boolean overlapImminent(MovableObject moOther) {
		if ((this.movePath.size() < 1) || (moOther.movePath.size() < 1)) {
			// One or both of these MovableObjects is in an unstable condition
			// so don't even try to calculate the overlap
			return false;
		}
		// Ensure node1.speed > node2.speed (otherwise node1 will never catch up to node2) 
		if (this.speed <= moOther.speed) {
			return false;
		}
		// Ensure node2.startingLocation is on one of the roads in node1.movePath 
		// (node1 can only ever possibly run over another node if that node is 
		// between its starting and ending location), i.e. one of the following is true:
		int node2StartingLocPathIdx = -1;
		// node2.startingLocation is between node1.startingLocation and the end of the first road in node1.movePath 
		if (new LineString(this.startingLoc, this.movePath.get(0).getEnd()).contains(moOther.startingLoc)) {
			node2StartingLocPathIdx = 0;
			/*
			if (new LineString(moOther.startingLoc, this.movePath.get(0).getEnd()).contains(moOther.endingLoc)) {
				return true;
			} else if (new LineString(this.movePath.get(this.movePath.size() - 1).getBegin(), this.endingLoc).contains(moOther.endingLoc)) {
				return true;
			} else {
				int idx = 1;
				while (idx < (this.movePath.size() - 1)) {
					if (this.movePath.get(idx).contains(moOther.endingLoc)) {
						return true;
					} else {
						idx++;
					}
				}
				// cannot be determined this way
			}
			*/
		// or: node2.startingLocation is between the start of the last road in node1.movePath and node1.endingLocation 
		} else if (new LineString(this.movePath.get(this.movePath.size() - 1).getBegin(), this.endingLoc).contains(moOther.startingLoc)) {
			node2StartingLocPathIdx = this.movePath.size() - 1;
			/*
			if (new LineString(this.movePath.get(this.movePath.size() - 1).getBegin(), this.endingLoc).contains(moOther.endingLoc)) {
				return true;
			} else {
				// cannot be determined this way
			}
			*/
		// or: node2.startingLocation is on any road in node1.movePath that is not the first or last road
		} else {
			int idx = 1;
			while ((node2StartingLocPathIdx == -1) && (idx < (this.movePath.size() - 1))) {
				if (this.movePath.get(idx).contains(moOther.startingLoc)) {
					node2StartingLocPathIdx = idx;
					break;
				} else {
					idx++;
				}
			}
			if ((idx == (this.movePath.size() - 1)) || (node2StartingLocPathIdx == -1)) {
				return false;
			}
			/*
			while (idx < (this.movePath.size() - 2)) {
				if (this.movePath.get(idx).contains(moOther.endingLoc)) {
					return true;
				} else {
					idx++;
				}
			}
			if ((idx == (this.movePath.size() - 1) && (new LineString(this.movePath.get(this.movePath.size() - 1).getBegin(), this.endingLoc).contains(moOther.endingLoc)))) {
				return true;
			}
			// cannot be determined this way
			 */
		}
		// The road in node1 which contains node2.startingLocation
		// is the same as (i.e. has the same endpoints in the same order)
		// the road in node2 which contains node2.startingLocation
		// i.e. the first road in node2.movePath since a node's starting location is always on the first road in its movePath).
		// Since paths have a direction, having the same road in both paths indicates 
		// both nodes are moving in the same direction.
		// APRIL_TODO: this may not work because sometimes getPath skips a value
		return (this.movePath.get(node2StartingLocPathIdx).getBegin().equals(moOther.movePath.get(0).getBegin())) ||
		       (this.movePath.get(node2StartingLocPathIdx).getEnd().equals(moOther.movePath.get(0).getEnd()));
	}
	
	public boolean isValid() {
		return (this.startingLoc != null) && 
			   (this.endingLoc != null) &&
			   !(this.movePath.isEmpty());
	}
	
	
	public DTNHost getHost() {
		return host;
	}

	public void setHost(DTNHost host) {
		this.host = host;
	}
	
	private void calculateMoveLine() {
		if (this.startingLoc != null && this.endingLoc != null) {
			this.moveLine = new LineString(this.startingLoc, this.endingLoc);
		} else {
			this.moveLine = null;
		}
	}

	public Coord getStartingLoc() {
		return startingLoc;
	}

	public void setStartingLoc(Coord curr) {
		this.startingLoc = curr;
		calculateMoveLine();
	}

	public Coord getEndingLoc() {
		return endingLoc;
	}

	public void setEndingLoc(Coord dest) {
		this.endingLoc = dest;
		calculateMoveLine();
	}

	public double getSpeed() {
		return speed;
	}

	public void setSpeed(double speed) {
		this.speed = speed;
	}
	
	public LineString getMoveLine() {
		return this.moveLine;
	}

	public double getIntervalTime() {
		return intervalTime;
	}

	public void setIntervalTime(double intervalTime) {
		this.intervalTime = intervalTime;
	}

	public List<Coord> getFullPath() {
		return fullPath;
	}

	public void setFullPath(List<Coord> hostPath) {
		this.fullPath = hostPath;
	}

	public void addToFullPath(Coord newCoord) {
		if (newCoord != null) {
			this.fullPath.add(newCoord);
		}
	}

	public List<LineString> getMovePath() {
		return movePath;
	}

	public void setMovePath(List<LineString> movePathArg) {
		this.movePath = movePathArg;
	}

	public void addToMovePath(LineString newRoad) {
		if (newRoad != null) {
			this.movePath.add(newRoad);
		}
	}
	
	@Override
	public String toString() {
		StringBuffer sbuf = new StringBuffer();
		sbuf.append("Host = ").append(host).append(EOLN)
			.append("Starting Loc = ").append(startingLoc).append(EOLN)
			.append("Ending Loc = ").append(endingLoc).append(EOLN)
			.append("Speed = ").append(speed).append(EOLN)
			.append("Interval Time = ").append(intervalTime).append(EOLN);
		sbuf.append("Full Path = ").append(EOLN);
		for(Coord coord : fullPath) {
			sbuf.append("\t").append(coord.toString()).append(EOLN);
		}
		sbuf.append("Movement Path = ").append(EOLN);
		for(LineString ls : movePath) {
			sbuf.append("\t").append(ls.toString()).append(EOLN);
		}
		return sbuf.toString();
	}
	
	public final static void main(String[] args) {
		/*
		MovableObject autocar0 = new MovableObject(null, 
				new Coord(186.98,337.60), 
				new Coord(186.00,337.51), 
				0.9845486236385901,
				1.0,
				Arrays.asList(new Coord[]{ 
					new Coord(189.29,337.82),
					new Coord(177.46,336.71)
				}));
		MovableObject car1 = new MovableObject(null, 
				new Coord(186.55,338.51),
				new Coord(186.44,338.49),
				0.1094394472484288,
				1.0,
				Arrays.asList(new Coord[]{ 
					new Coord(229.68,347.05),
					new Coord(177.46,336.71),
					new Coord(164.59,336.07)
				}));
		System.out.println(autocar0.overlapImminent(car1));
		*/
		MovableObject autocar0 = new MovableObject(null, 
				new Coord(172.94,336.48), 
				new Coord(172.30,336.45), 
				0.6364432204645312,
				1.0,
				Arrays.asList(new Coord[]{ 
					new Coord(177.46,336.71),
					new Coord(164.59,336.07)
				}));
		MovableObject car1 = new MovableObject(null, 
				new Coord(172.72,336.47),
				new Coord(172.61,336.47),
				0.1094394472484288,
				1.0,
				Arrays.asList(new Coord[]{ 
					new Coord(229.68,347.05),
					new Coord(177.46,336.71),
					new Coord(164.59,336.07)
				}));
		System.out.println(autocar0.overlapImminent(car1));
	}

}
