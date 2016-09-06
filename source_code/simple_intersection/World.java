/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package core;

import input.EventQueue;
import input.ExternalEvent;
import input.ScheduledUpdatesQueue;

import java.util.ArrayList;
import static java.util.Arrays.asList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import core.DTNHost;




/**
 * World contains all the nodes and is responsible for updating their
 * location and connections.
 */
public class World {
	/** name space of optimization settings ({@value})*/
	public static final String OPTIMIZATION_SETTINGS_NS = "Optimization";

	/**
	 * Should the order of node updates be different (random) within every
	 * update step -setting id ({@value}). Boolean (true/false) variable.
	 * Default is @link {@link #DEF_RANDOMIZE_UPDATES}.
	 */
	public static final String RANDOMIZE_UPDATES_S = "randomizeUpdateOrder";
	/** should the update order of nodes be randomized -setting's default value
	 * ({@value}) */
	public static final boolean DEF_RANDOMIZE_UPDATES = true;

	/**
	 * Should the connectivity simulation be stopped after one round
	 * -setting id ({@value}). Boolean (true/false) variable.
	 */
	public static final String SIMULATE_CON_ONCE_S = "simulateConnectionsOnce";

	private int sizeX;
	private int sizeY;
	private List<EventQueue> eventQueues;
	private double updateInterval;
	private SimClock simClock;
	private double nextQueueEventTime;
	private EventQueue nextEventQueue;
	/** list of nodes; nodes are indexed by their network address */
	private List<DTNHost> hosts;
	private boolean simulateConnections;
	/** nodes in the order they should be updated (if the order should be
	 * randomized; null value means that the order should not be randomized) */
	private ArrayList<DTNHost> updateOrder;
	/** is cancellation of simulation requested from UI */
	private boolean isCancelled;
	private List<UpdateListener> updateListeners;
	/** Queue of scheduled update requests */
	private ScheduledUpdatesQueue scheduledUpdates;
	private boolean simulateConOnce;

	private DTNHost intersection;
	private Coord location;

	private ArrayList<Coord> lineEW;
	private ArrayList<Coord> lineNS;
	private List<DTNHost> neighbors;
	private double delay;

	private boolean direction;
	private long delay_time = 100;


	private void switchDirection() {

		direction = !direction;
	}

	private void delay() {

		try {
    		Thread.sleep(delay_time);
    	}
    	catch (InterruptedException e) {
    		System.out.println("Interrupted");
    	}
	}


	private boolean liesInNS(Coord location) {

		if (location.getX() <= 169 &&  location.getX() >= 164
				&& location.getY() <= 119 && location.getY() >= 0)
			return true;
		else
			return false;
	}

	private boolean liesInEW(Coord location) {

		if (location.getX() <= 297 &&  location.getX() >= 95
				&& location.getY() <= 72 && location.getY() >= 65)
			return true;
		else
			return false;
	}
	/**
	 * Constructor.
	 */
	public World(List<DTNHost> hosts, int sizeX, int sizeY,
			double updateInterval, List<UpdateListener> updateListeners,
			boolean simulateConnections, List<EventQueue> eventQueues) {
		this.hosts = hosts;
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.updateInterval = updateInterval;
		this.updateListeners = updateListeners;
		this.simulateConnections = simulateConnections;
		this.eventQueues = eventQueues;

		this.simClock = SimClock.getInstance();
		this.scheduledUpdates = new ScheduledUpdatesQueue();
		this.isCancelled = false;
		this.direction = true;

		this.lineNS = new ArrayList<Coord>();

		lineNS.add(new Coord(814.732,300.471));
		lineNS.add(new Coord(814.961,320.797));
		lineNS.add(new Coord(815.19,341.402));
		lineNS.add(new Coord(815.291,350.197));
		lineNS.add(new Coord(815.291,350.197));
		lineNS.add(new Coord(815.374,356.947));
		lineNS.add(new Coord(815.943,404.616));
		lineNS.add(new Coord(816.108,418.249));

		this.lineEW = new ArrayList<Coord>();

		lineEW.add(new Coord(659.977,361.895));
		lineEW.add(new Coord(668.74,356.902));
		lineEW.add(new Coord(685.173,352.243));
		lineEW.add(new Coord(708.825,351.832));
		lineEW.add(new Coord(753.458,351.065));
		lineEW.add(new Coord(753.458,351.065));
		lineEW.add(new Coord(776.324,350.687));
		lineEW.add(new Coord(794.597,350.431));
		lineEW.add(new Coord(806.751,350.264));
		lineEW.add(new Coord(815.291,350.197));
		lineEW.add(new Coord(815.291,350.197));
		lineEW.add(new Coord(821.354,350.042));
		lineEW.add(new Coord(869.98,348.83));

		//get the intersection host
		 for (DTNHost host: hosts) {

		 	System.out.println("Host: "+host.toString());

		 	if (host.toString().equals("autocar2")) {

		 		location = host.getLocation();

		 		System.out.println("Autocar's location: "+ location.getX() + "  and Y: " + location.getY());
		 	}

       		if (host.toString().equals("Intersection6")) {

       			this.intersection = host;
       			location = new Coord(157.3598,67.9789);
       			//location = new Coord(815.291,509.659);
     			this.intersection.setLocation(location);
       		}



        }

    	//set location for intersection
		setNextEventQueue();
		initSettings();
	}

	/**
	 * Initializes settings fields that can be configured using Settings class
	 */
	private void initSettings() {
		Settings s = new Settings(OPTIMIZATION_SETTINGS_NS);
		boolean randomizeUpdates = DEF_RANDOMIZE_UPDATES;

		if (s.contains(RANDOMIZE_UPDATES_S)) {
			randomizeUpdates = s.getBoolean(RANDOMIZE_UPDATES_S);
		}
		simulateConOnce = s.getBoolean(SIMULATE_CON_ONCE_S, false);

		if(randomizeUpdates) {
			// creates the update order array that can be shuffled
			this.updateOrder = new ArrayList<DTNHost>(this.hosts);
		}
		else { // null pointer means "don't randomize"
			this.updateOrder = null;
		}
	}

	/**
	 * Moves hosts in the world for the time given time initialize host
	 * positions properly. SimClock must be set to <CODE>-time</CODE> before
	 * calling this method.
	 * @param time The total time (seconds) to move
	 */
	public void warmupMovementModel(double time) {

		neighbors = new ArrayList<DTNHost>();
		neighbors = getNeighbours();

		if (time <= 0) {
			return;
		}

		while(SimClock.getTime() < -updateInterval) {

			moveHosts(updateInterval,neighbors);
			simClock.advance(updateInterval);
		}

		double finalStep = -SimClock.getTime();

		moveHosts(finalStep,neighbors);
		simClock.setTime(0);
	}

	/**
	 * Goes through all event Queues and sets the
	 * event queue that has the next event.
	 */
	public void setNextEventQueue() {
		EventQueue nextQueue = scheduledUpdates;
		double earliest = nextQueue.nextEventsTime();

		/* find the queue that has the next event */
		for (EventQueue eq : eventQueues) {
			if (eq.nextEventsTime() < earliest){
				nextQueue = eq;
				earliest = eq.nextEventsTime();
			}
		}

		this.nextEventQueue = nextQueue;
		this.nextQueueEventTime = earliest;
	}

	/**
	 * Update (move, connect, disconnect etc.) all hosts in the world.
	 * Runs all external events that are due between the time when
	 * this method is called and after one update interval.
	 */

	public List<DTNHost> getNeighbours() {

		//direction= True: North-South
		//direction= False: East-West

		neighbors = new ArrayList<DTNHost>();

		Coord hostLocation;

		for(DTNHost host: this.hosts) {

			if(host.toString().equals(this.intersection.toString())) {
				continue;
			}
			hostLocation = host.getLocation();
			// System.out.println("HOST: " + host.toString() + " X: "+ hostLocation.getX() + " Y: "+hostLocation.getY());
			if(direction) {
				//North-South
				//TODO: get hosts whose location lies in the list of lineNS
				 System.out.println("...NORTH SOUTH NEIGHBORS...");
				//if(lineNS.contains(hostLocation)) {
				if(liesInNS(hostLocation)) {
					neighbors.add(host);
					System.out.println("HOST: " + host.toString());
				}
			}
			else{
				//East-West
				//TODO: get hosts whose location lies in the list of lineEW
				System.out.println("...EAST WEST NEIGHBORS...");
				//if(lineEW.contains(hostLocation)) {
				if(liesInEW(hostLocation)) {
					neighbors.add(host);
					System.out.println("HOST: " + host.toString());
				}
			}
		}
		
		return neighbors;
	}


	public void update () {
		double runUntil = SimClock.getTime() + this.updateInterval;

		neighbors = new ArrayList<DTNHost>();

		setNextEventQueue();

		/* process all events that are due until next interval update */
		while (this.nextQueueEventTime <= runUntil) {
			simClock.setTime(this.nextQueueEventTime);
			ExternalEvent ee = this.nextEventQueue.nextEvent();
			ee.processEvent(this);
			neighbors = this.getNeighbours();
			// if (!(neighbors.isEmpty())) {

			// 	updateHosts(neighbors);
			// 	delay();

			// }
			// switchDirection();
			// neighbors = this.getNeighbours();
			// if (!(neighbors.isEmpty())) {

			// 	updateHosts(neighbors);
			// 	delay();

			// }
			updateHosts(); // update all hosts after every event
			setNextEventQueue();
		}

		//introduce delay + send messages
		/*
		 for (DTNHost neighbor: neighbours) {

		 	sendGreen(this.intersection,neighbor);
		 	delay();
		 	sendOrange(this.intersection,neighbor);
		 	delay();
		 	sendRed
            Message msg = new Message(this.intersection,neighbor,"Green",1);
            msg.setTtl(this.msgTtl);
            msg.addProperty("Color", "Green");
            if(!createNewMessage(msg))
            {
                throw new SimError("Message could not be created");
            }
            host.createNewMessage(msg);
        } */


        neighbors = this.getNeighbours();
		moveHosts(this.updateInterval, neighbors);
		simClock.setTime(runUntil);

		updateHosts(neighbors);
		
		// if (!(neighbors.isEmpty())) {

		// 	updateHosts(neighbors);
		// 	delay();

		// }
		// switchDirection();
		// System.out.println("...SWITCHING DIRECTION NOW...");
		// neighbors = this.getNeighbours();
		// if (!(neighbors.isEmpty())) {

		// 	updateHosts(neighbors);
		// 	delay();

		// }
		// /* inform all update listeners */
		// for (UpdateListener ul : this.updateListeners) {
		// 	ul.updated(this.hosts);
		// }
	}

	/**
	 * Updates all hosts (calls update for every one of them). If update
	 * order randomizing is on (updateOrder array is defined), the calls
	 * are made in random order.
	 */
	private void updateHosts(List<DTNHost> neighbours) {

		neighbours = getNeighbours();

		if (this.updateOrder == null) { // randomizing is off

			System.out.println("Randomizing is off");
			for (int i=0, n = hosts.size();i < n; i++) {
				if (this.isCancelled) {
					break;
				}
				if(neighbours.contains(hosts.get(i))) {
					// System.out.println("ok, host : " + hosts.get(i).toString());
					hosts.get(i).update(simulateConnections);
				}
			}
		}
		else { // update order randomizing is on
			assert this.updateOrder.size() == this.hosts.size() :
				"Nrof hosts has changed unexpectedly";
			Random rng = new Random(SimClock.getIntTime());
			Collections.shuffle(this.updateOrder, rng);
			for (int i=0, n = hosts.size();i < n; i++) {
				if (this.isCancelled) {
					break;
				}
				this.updateOrder.get(i).update(simulateConnections);
			}
		}

		if (simulateConOnce && simulateConnections) {
			simulateConnections = false;
		}
	}

	/**
	 * Moves all hosts in the world for a given amount of time
	 * @param timeIncrement The time how long all nodes should move
	 */
	private void moveHosts(double timeIncrement,List<DTNHost> neighbours) {
		for (int i=0,n = hosts.size(); i<n; i++) {
			DTNHost host = hosts.get(i);

				host.move(timeIncrement);
			
			
		}
	}

	/**
	 * Asynchronously cancels the currently running simulation
	 */
	public void cancelSim() {
		this.isCancelled = true;
	}

	/**
	 * Returns the hosts in a list
	 * @return the hosts in a list
	 */
	public List<DTNHost> getHosts() {
		return this.hosts;
	}

	/**
	 * Returns the x-size (width) of the world
	 * @return the x-size (width) of the world
	 */
	public int getSizeX() {
		return this.sizeX;
	}

	/**
	 * Returns the y-size (height) of the world
	 * @return the y-size (height) of the world
	 */
	public int getSizeY() {
		return this.sizeY;
	}

	/**
	 * Returns a node from the world by its address
	 * @param address The address of the node
	 * @return The requested node or null if it wasn't found
	 */
	public DTNHost getNodeByAddress(int address) {
		if (address < 0 || address >= hosts.size()) {
			throw new SimError("No host for address " + address + ". Address " +
					"range of 0-" + (hosts.size()-1) + " is valid");
		}

		DTNHost node = this.hosts.get(address);
		assert node.getAddress() == address : "Node indexing failed. " +
			"Node " + node + " in index " + address;

		return node;
	}

	/**
	 * Schedules an update request to all nodes to happen at the specified
	 * simulation time.
	 * @param simTime The time of the update
	 */
	public void scheduleUpdate(double simTime) {
		scheduledUpdates.addUpdate(simTime);
	}
}
