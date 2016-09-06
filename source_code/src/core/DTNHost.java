/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import movement.MovementModel;
import movement.Path;
import routing.MessageRouter;
import routing.util.RoutingInfo;
import static core.Constants.DEBUG;

/**
 * A DTN capable host.
 */
public class DTNHost implements Comparable<DTNHost> {
	private static int nextAddress = 0;
	private int address;

	private Coord location; 	// where is the host
	private Coord destination;	// where is it going

	private MessageRouter router;
	private MovementModel movement;
	private Path path;
	private double speed;
	private double nextTimeToMove;
	private String name;
	private List<MessageListener> msgListeners;
	private List<MovementListener> movListeners;
	private List<NetworkInterface> net;
	private ModuleCommunicationBus comBus;
	
	private boolean active;
	private GroupTypeEnum groupType;
	private List<MovableObject> locUpdateMsgsRecd = new java.util.ArrayList<MovableObject>();
	private MovableObject mo;
	private Coord endOfLastPath = null;
	private List<Coord> myPath = new ArrayList<Coord>();

	static {
		DTNSim.registerForReset(DTNHost.class.getCanonicalName());
		reset();
	}
	/**
	 * Creates a new DTNHost.
	 * @param msgLs Message listeners
	 * @param movLs Movement listeners
	 * @param groupId GroupID of this host
	 * @param interf List of NetworkInterfaces for the class
	 * @param comBus Module communication bus object
	 * @param mmProto Prototype of the movement model of this host
	 * @param mRouterProto Prototype of the message router of this host
	 */
	public DTNHost(List<MessageListener> msgLs,
			List<MovementListener> movLs,
			String groupId, List<NetworkInterface> interf,
			ModuleCommunicationBus comBus,
			MovementModel mmProto, MessageRouter mRouterProto,
			GroupTypeEnum groupType) {
		this.comBus = comBus;
		this.location = new Coord(0,0);
		this.address = getNextAddress();
		this.name = groupId+address;
		this.net = new ArrayList<NetworkInterface>();
		this.groupType = groupType;
		this.active = true;

		for (NetworkInterface i : interf) {
			NetworkInterface ni = i.replicate();
			ni.setHost(this);
			net.add(ni);
		}

		// TODO - think about the names of the interfaces and the nodes
		//this.name = groupId + ((NetworkInterface)net.get(1)).getAddress();

		this.msgListeners = msgLs;
		this.movListeners = movLs;

		// create instances by replicating the prototypes
		this.movement = mmProto.replicate();
		this.movement.setComBus(comBus);
		this.movement.setHost(this);
		setRouter(mRouterProto.replicate());

		this.location = movement.getInitialLocation();

		this.nextTimeToMove = movement.nextPathAvailable();
		this.path = null;

		if (movLs != null) { // inform movement listeners about the location
			for (MovementListener l : movLs) {
				l.initialLocation(this, this.location);
			}
		}
	}
	
	public String getName() {
		return this.name;
	}
	

	/**
	 * Returns a new network interface address and increments the address for
	 * subsequent calls.
	 * @return The next address.
	 */
	private synchronized static int getNextAddress() {
		return nextAddress++;
	}

	/**
	 * Reset the host and its interfaces
	 */
	public static void reset() {
		nextAddress = 0;
	}

	/**
	 * Returns true if this node is actively moving (false if not)
	 * @return true if this node is actively moving (false if not)
	 */
	public boolean isMovementActive() {
		return this.movement.isActive();
	}
	
	public void setActive(boolean val) {
		this.active = val;
	}

	public boolean getActive() {
		return this.active;
	}

	/**
	 * Returns true if this node's radio is active (false if not)
	 * @return true if this node's radio is active (false if not)
	 */
	public boolean isRadioActive() {
		// Radio is active if any of the network interfaces are active.
		for (final NetworkInterface i : this.net) {
			if (i.isActive()) return true;
		}
		return false;
	}

	/**
	 * Set a router for this host
	 * @param router The router to set
	 */
	private void setRouter(MessageRouter router) {
		router.init(this, msgListeners);
		this.router = router;
	}

	/**
	 * Returns the router of this host
	 * @return the router of this host
	 */
	public MessageRouter getRouter() {
		return this.router;
	}

	/**
	 * Returns the movement model of this host
	 * @return the movement model of this host
	 */
	public MovementModel getMovementModel() {
		return this.movement;
	}

	/**
	 * Returns the network-layer address of this host.
	 */
	public int getAddress() {
		return this.address;
	}

	/**
	 * Returns this hosts's ModuleCommunicationBus
	 * @return this hosts's ModuleCommunicationBus
	 */
	public ModuleCommunicationBus getComBus() {
		return this.comBus;
	}

    /**
	 * Informs the router of this host about state change in a connection
	 * object.
	 * @param con  The connection object whose state changed
	 */
	public void connectionUp(Connection con) {
		this.router.changedConnection(con);
	}

	public void connectionDown(Connection con) {
		this.router.changedConnection(con);
	}

	/**
	 * Returns a copy of the list of connections this host has with other hosts
	 * @return a copy of the list of connections this host has with other hosts
	 */
	public List<Connection> getConnections() {
		List<Connection> lc = new ArrayList<Connection>();

		for (NetworkInterface i : net) {
			lc.addAll(i.getConnections());
		}

		return lc;
	}

	/**
	 * Returns the current location of this host.
	 * @return The location
	 */
	public Coord getLocation() {
		return this.location;
	}

	/**
	 * Returns the Path this node is currently traveling or null if no
	 * path is in use at the moment.
	 * @return The path this node is traveling
	 */
	public Path getPath() {
		return this.path;
	}


	/**
	 * Sets the Node's location overriding any location set by movement model
	 * @param location The location to set
	 */
	public void setLocation(Coord location) {
		this.location = location.clone();
	}

	/**
	 * Sets the Node's name overriding the default name (groupId + netAddress)
	 * @param name The name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns the messages in a collection.
	 * @return Messages in a collection
	 */
	public Collection<Message> getMessageCollection() {
		return this.router.getMessageCollection();
	}

	/**
	 * Returns the number of messages this node is carrying.
	 * @return How many messages the node is carrying currently.
	 */
	public int getNrofMessages() {
		return this.router.getNrofMessages();
	}

	/**
	 * Returns the buffer occupancy percentage. Occupancy is 0 for empty
	 * buffer but can be over 100 if a created message is bigger than buffer
	 * space that could be freed.
	 * @return Buffer occupancy percentage
	 */
	public double getBufferOccupancy() {
		double bSize = router.getBufferSize();
		double freeBuffer = router.getFreeBufferSize();
		return 100*((bSize-freeBuffer)/bSize);
	}

	/**
	 * Returns routing info of this host's router.
	 * @return The routing info.
	 */
	public RoutingInfo getRoutingInfo() {
		return this.router.getRoutingInfo();
	}

	/**
	 * Returns the interface objects of the node
	 */
	public List<NetworkInterface> getInterfaces() {
		return net;
	}

	/**
	 * Find the network interface based on the index
	 */
	public NetworkInterface getInterface(int interfaceNo) {
		NetworkInterface ni = null;
		try {
			ni = net.get(interfaceNo-1);
		} catch (IndexOutOfBoundsException ex) {
			throw new SimError("No such interface: "+interfaceNo +
					" at " + this);
		}
		return ni;
	}

	/**
	 * Find the network interface based on the interfacetype
	 */
	protected NetworkInterface getInterface(String interfacetype) {
		for (NetworkInterface ni : net) {
			if (ni.getInterfaceType().equals(interfacetype)) {
				return ni;
			}
		}
		return null;
	}

	/**
	 * Force a connection event
	 */
	public void forceConnection(DTNHost anotherHost, String interfaceId,
			boolean up) {
		NetworkInterface ni;
		NetworkInterface no;

		if (interfaceId != null) {
			ni = getInterface(interfaceId);
			no = anotherHost.getInterface(interfaceId);

			assert (ni != null) : "Tried to use a nonexisting interfacetype "+interfaceId;
			assert (no != null) : "Tried to use a nonexisting interfacetype "+interfaceId;
		} else {
			ni = getInterface(1);
			no = anotherHost.getInterface(1);

			assert (ni.getInterfaceType().equals(no.getInterfaceType())) :
				"Interface types do not match.  Please specify interface type explicitly";
		}

		if (up) {
			ni.createConnection(no);
		} else {
			ni.destroyConnection(no);
		}
	}

	/**
	 * for tests only --- do not use!!!
	 */
	public void connect(DTNHost h) {
		if (DEBUG) Debug.p("WARNING: using deprecated DTNHost.connect" +
			"(DTNHost) Use DTNHost.forceConnection(DTNHost,null,true) instead");
		forceConnection(h,null,true);
	}

	/**
	 * Updates node's network layer and router.
	 * @param simulateConnections Should network layer be updated too
	 */
	public void update(boolean simulateConnections) {
		if (!isRadioActive()) {
			// Make sure inactive nodes don't have connections
			tearDownAllConnections();
			return;
		}

		if (simulateConnections) {
			for (NetworkInterface i : net) {
				i.update();
			}
		}
		this.router.update();
		
		// System.out.println("DTNHost.update :: " + this.name);
		// Check if this vehicle needs to watch for another vehicle 
		int DISTANCE_TO_CHECK = 50;
		World w = SimScenario.getInstance().getWorld();
		int numHosts = w.getNumHosts();
		for (int hostIdx = 0; hostIdx < numHosts; hostIdx++) {
			DTNHost other = w.getNodeByAddress(hostIdx);
			// Don't check if you're going to overrun or T yourself
			if (this.getAddress() == other.getAddress()) {
				continue;
			}
			
			System.out.println("this.location.distance(other.location) = " + this.location.distance(other.location));
			System.out.println("this.mo = " + (this.mo != null ? this.mo : "null"));
			System.out.println("other.mo = " + (other.mo != null ? other.mo : "null"));
			System.out.println("");
			// Check first if we're anywhere near the other car 
			if (this.location.distance(other.location) < DISTANCE_TO_CHECK) { 
				if ((this.mo != null) && (other.mo != null) && this.mo.isValid() && other.mo.isValid()) {
					// Then it's worth checking whether this car is about to overrun the other
					
					// Are we in the overlap condition?
					// if (this.mo.getEndingLoc() == null) this.mo.setEndingLoc(this.mo.getStartingLoc());
					// if (other.mo.getEndingLoc() == null) other.mo.setEndingLoc(other.mo.getStartingLoc());

					if (this.mo.overlapImminent(other.mo)) {
						
						// We're about to run over, slow down.
						// Slow down by moving the lesser of halfway to the other's starting point
						// or the end of the first road in the movePath
						this.location = this.mo.getMovePath().get(0).getCoordOnLineForX(this.mo.getStartingLoc().getX() +
								(0.5 * (other.mo.getStartingLoc().getX() - this.mo.getStartingLoc().getX())));
						this.mo.setEndingLoc(this.location.clone());
						System.out.println(SimClock.getTime() + ": " + this.name + " is about to run over " + other.name);
						System.out.println("this.mo = " + this.mo);
						System.out.println("");
					} else if (this.mo.getMoveLine().intersects(other.mo.getMoveLine()) &&
							!(other.mo.getStartingLoc().equals(other.mo.getEndingLoc()))) {
						// We're about to run into, slow down.
						// Slow down by moving the lesser of halfway to the other's starting point
						// or the end of the first road in the movePath
						this.location = this.mo.getMovePath().get(0).getCoordOnLineForX(this.mo.getStartingLoc().getX() +
								(0.5 * (other.mo.getStartingLoc().getX() - this.mo.getStartingLoc().getX())));
						this.mo.setEndingLoc(this.location.clone());
						System.out.println(SimClock.getTime() + ": " + this.name + " is about to run into " + other.name);
						System.out.println("this.mo = " + this.mo);
						System.out.println("");
					} // else: otherwise we don't care
				}
			}
		}
	}
	

	/**
	 * Tears down all connections for this host.
	 */
	private void tearDownAllConnections() {
		for (NetworkInterface i : net) {
			// Get all connections for the interface
			List<Connection> conns = i.getConnections();
			if (conns.size() == 0) continue;

			// Destroy all connections
			List<NetworkInterface> removeList =
				new ArrayList<NetworkInterface>(conns.size());
			for (Connection con : conns) {
				removeList.add(con.getOtherInterface(i));
			}
			for (NetworkInterface inf : removeList) {
				i.destroyConnection(inf);
			}
		}
	}

	/**
	 * Moves the node towards the next waypoint or waits if it is
	 * not time to move yet
	 * @param timeIncrement How long time the node moves
	 */
	public void move(double timeIncrement) {
		double possibleMovement;
		double distance;
		double dx, dy;

 		// System.out.println("DTNHost.move(): " + this.name);
		
		// Create a MovableObject instance that represents this movement;
		// Save path as soon as possible
		MovableObject oldMO = mo;
		mo = new MovableObject();
		if (myPath != null) {
			mo.getFullPath().addAll(myPath);
		}
		mo.setHost(this);
		mo.setIntervalTime(timeIncrement);
		mo.setStartingLoc(this.location.clone());
		
		if (!isMovementActive() || SimClock.getTime() < this.nextTimeToMove || !active) {
			this.mo = oldMO;
			return;
		}
		if (this.destination == null) {
			if (!setNextWaypoint()) {
				return;
			}
		}

		possibleMovement = timeIncrement * speed;
		distance = this.location.distance(this.destination);

		while (possibleMovement >= distance) {
			// node can move past its next destination
			this.location.setLocation(this.destination); // snap to destination
														 // so can calculate dx and dy later
			possibleMovement -= distance;
			if (!setNextWaypoint()) { // get a new waypoint
				if (mo.getStartingLoc().equals(this.destination)) {
					return; // no more waypoints left on this path; wait until can get another path
				} else {
					break; // this is as far as we can go on this path
				}
			}
			distance = this.location.distance(this.destination);
		}

		// move towards the point for possibleMovement amount
		if (!(this.destination.equals(this.location))) {
			dx = (possibleMovement/distance) * (this.destination.getX() -
					this.location.getX());
			dy = (possibleMovement/distance) * (this.destination.getY() -
					this.location.getY());
			if (this.location.getX() == this.destination.getX()) {
				// The way the slope is calculated in LineString,
				// When the X's are equal, getCoordOnLineForX returns a
				// Y value which is NaN because there is a division by 0
				this.location.translate(dx, dy);
		} else {
				this.location.setLocation(new LineString(this.location, this.destination)
					.getCoordOnLineForX(this.location.getX() + dx));
			}
		}
		mo.setEndingLoc(this.location.clone());
		mo.setSpeed(speed);
		mo.calculateMovePath();
 		// System.out.println("DTNHost.move(): " + mo);
	}
	
	public MovableObject getAndRemoveMovableObject() {
		MovableObject moClone = this.mo;
		this.mo = null;
		return moClone;
	}
	
	/**
	 * Add an update message to an internal list to use later
	 * @param locUpdate The update to add to the internal list
	 */
	public void addLocUpdateMsg(MovableObject locUpdate) {
		this.locUpdateMsgsRecd.add(locUpdate);
	}

	/**
	 * Sets the next destination and speed to correspond the next waypoint
	 * on the path.
	 * @return True if there was a next waypoint to set, false if node still
	 * should wait
	 */
	private boolean setNextWaypoint() {
		if (path == null) {
			path = movement.getPath();
			if ((path != null) && !path.getCoords().isEmpty()) {
				myPath.clear();
				if ((endOfLastPath != null) && !(path.getCoords().contains(endOfLastPath))) { 
					myPath.add(endOfLastPath);
				}
				myPath.addAll(path.getCoords());
				mo.getFullPath().clear();
				mo.getFullPath().addAll(myPath);
				endOfLastPath = myPath.get(myPath.size() - 1);

			}
		}

		if (path == null || !path.hasNext()) {
			this.nextTimeToMove = movement.nextPathAvailable();
			this.path = null;
			return false;
		}

		this.destination = path.getNextWaypoint();
		this.speed = path.getSpeed();

		if (this.movListeners != null) {
			for (MovementListener l : this.movListeners) {
				l.newDestination(this, this.destination, this.speed);
			}
		}

		return true;
	}

	/**
	 * Sends a message from this host to another host
	 * @param id Identifier of the message
	 * @param to Host the message should be sent to
	 */
	public void sendMessage(String id, DTNHost to) {
		this.router.sendMessage(id, to);
	}

	/**
	 * Start receiving a message from another host
	 * @param m The message
	 * @param from Who the message is from
	 * @return The value returned by
	 * {@link MessageRouter#receiveMessage(Message, DTNHost)}
	 */
	public int receiveMessage(Message m, DTNHost from) {
		int retVal = this.router.receiveMessage(m, from);

		if (retVal == MessageRouter.RCV_OK) {
			m.addNodeOnPath(this);	// add this node on the messages path
		}

		return retVal;
	}

	/**
	 * Requests for deliverable message from this host to be sent trough a
	 * connection.
	 * @param con The connection to send the messages trough
	 * @return True if this host started a transfer, false if not
	 */
	public boolean requestDeliverableMessages(Connection con) {
		return this.router.requestDeliverableMessages(con);
	}

	/**
	 * Informs the host that a message was successfully transferred.
	 * @param id Identifier of the message
	 * @param from From who the message was from
	 */
	public void messageTransferred(String id, DTNHost from) {
		this.router.messageTransferred(id, from);
	}

	/**
	 * Informs the host that a message transfer was aborted.
	 * @param id Identifier of the message
	 * @param from From who the message was from
	 * @param bytesRemaining Nrof bytes that were left before the transfer
	 * would have been ready; or -1 if the number of bytes is not known
	 */
	public void messageAborted(String id, DTNHost from, int bytesRemaining) {
		this.router.messageAborted(id, from, bytesRemaining);
	}

	/**
	 * Creates a new message to this host's router
	 * @param m The message to create
	 */
	public void createNewMessage(Message m) {
		this.router.createNewMessage(m);
	}

	/**
	 * Deletes a message from this host
	 * @param id Identifier of the message
	 * @param drop True if the message is deleted because of "dropping"
	 * (e.g. buffer is full) or false if it was deleted for some other reason
	 * (e.g. the message got delivered to final destination). This effects the
	 * way the removing is reported to the message listeners.
	 */
	public void deleteMessage(String id, boolean drop) {
		this.router.deleteMessage(id, drop);
	}

	/**
	 * Returns a string presentation of the host.
	 * @return Host's name
	 */
	public String toString() {
		return name;
	}

	/**
	 * Checks if a host is the same as this host by comparing the object
	 * reference
	 * @param otherHost The other host
	 * @return True if the hosts objects are the same object
	 */
	public boolean equals(DTNHost otherHost) {
		return this == otherHost;
	}

	/**
	 * Compares two DTNHosts by their addresses.
	 * @see Comparable#compareTo(Object)
	 */
	public int compareTo(DTNHost h) {
		return this.getAddress() - h.getAddress();
	}
	
	/**
	 * Returns this host's current speed
	 * @return this host's current speed
	 */
	public double getSpeed() {
		return this.speed;
	}
	
	/**
	 * Returns the group type of this host
	 */
	public GroupTypeEnum getGroupType() {
		return this.groupType;
	}
	
	/**
	 * Returns true if this host is a self driving car
	 */
	public boolean isSelfDrivingCar() {
		return this.groupType == GroupTypeEnum.selfDrivingCar;
	}
	
	/**
	 * Returns true if this host is a manual car
	 */
	public boolean isManualCar() {
		return this.groupType == GroupTypeEnum.manualCar;
	}
	
	/**
	 * Returns true if this host is a pedestrian
	 */
	public boolean isPedestrian() {
		return this.groupType == GroupTypeEnum.pedestrian;
	}
	
	/**
	 * Returns true if this host is a bike
	 */
	public boolean isBike() {
		return this.groupType == GroupTypeEnum.bike;
	}
	
	/**
	 * Returns true if this host is a bus
	 */
	public boolean isBus() {
		return this.groupType == GroupTypeEnum.bus;
	}
	
	/*
	// Tests for method: getSubsetOfList
	private static void test(Coord prevLoc, Coord currLoc, List<Coord> origCoordList) {
		System.out.println("prevLoc = " + prevLoc);
		System.out.println("currLoc = " + currLoc);
		System.out.println("origCoordList = " + origCoordList);
		List<Coord> result = DTNHost.getSubsetOfList(prevLoc, currLoc, origCoordList);
		System.out.println("results = " + result);
		System.out.println("");
	}

	public static void main(String[] args) {
		List<Coord> origCoordList = new ArrayList<Coord>();
		origCoordList.add(new Coord(1, 1));
		origCoordList.add(new Coord(2, 2));
		origCoordList.add(new Coord(3, 3));
		origCoordList.add(new Coord(4, 4));
		
		test(new Coord(0.25, 0.25), new Coord(0.5, 0.5), origCoordList);

		origCoordList.add(0, new Coord(0, 0));
		test(new Coord(0.25, 0.25), new Coord(0.5, 0.5), origCoordList);
		test(new Coord(0.5, 0.5), new Coord(4, 4), origCoordList);
		test(new Coord(0, 0), new Coord(2.5, 2.5), origCoordList);
		test(new Coord(1, 1), new Coord(2.5, 2.5), origCoordList);
		test(new Coord(1.5, 1.5), new Coord(2, 2), origCoordList);
	}
	*/
	
}
