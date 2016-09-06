/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */

package applications;

import java.util.List;
import java.util.Random;

import report.PingAppReporter;
import core.Application;
import core.Coord;
import core.DTNHost;
import core.LineString;
import core.Message;
import core.MovableObject;
import core.Settings;
import core.SimClock;
import core.SimScenario;
import core.World;

/**
 * Sends out a location update
 */
public class LocationUpdateApplication extends Application {
	/** Don't have any settings right now */
	public static final String NO_SETTINGS_KEY_S = "no_settings";

	/** Application ID */
	public static final String APP_ID = "aic.SelfDrivingCarRulesApplication";

	// Private vars
	private boolean futureSetting = false;
	private Random	rng;

	// General Message features
	public static final String EVENT_NAME = "SendLocInfo";
	public static final int MSG_SIZE = 1;
	
	// Message Property Keys (and values)
	public static final String MSG_TYPE_KEY = "type";
	public static final String MSG_TYPE_VALUE = "loc_update";
	public static final String CURR_COORD_KEY = "curr_coord";
	public static final String DEST_COORD_KEY = "dest_coord";
	public static final String SPEED_KEY = "speed";
	public static final String INTERVAL_TIME_KEY = "time_interval";
	public static final String PATH_SIZE_KEY = "path_size";
	public static final String PATH_ITEM_KEY = "path_";
	
	/**
	 * Creates a new ping application with the given settings.
	 *
	 * @param s	Settings to use for initializing the application.
	 */
	public LocationUpdateApplication(Settings s) {
		if (s.contains(NO_SETTINGS_KEY_S)){
			this.futureSetting = s.getBoolean(NO_SETTINGS_KEY_S);
		}

		this.rng = new Random(new java.util.Date().getTime());
		super.setAppID(APP_ID);
	}

	/**
	 * Copy-constructor
	 *
	 * @param a
	 */
	public LocationUpdateApplication(LocationUpdateApplication a) {
		super(a);
		this.rng = new Random(new java.util.Date().getTime());
	}

	/**
	 * Handles an incoming message. 
	 *
	 * @param msg	message received by the router
	 * @param host	host to which the application instance is attached
	 */
	@Override
	public Message handle(Message msg, DTNHost host) {
		String type = (String)msg.getProperty(MSG_TYPE_KEY);
		if ((type == null) || (type != MSG_TYPE_VALUE)) {
			// we aren't interested in the message
			return msg;
		}
		
		Object time_interval = msg.getProperty(INTERVAL_TIME_KEY);
		core.MovableObject mo = new core.MovableObject(
			msg.getFrom(),
			(Coord)msg.getProperty(CURR_COORD_KEY),
			(Coord)msg.getProperty(DEST_COORD_KEY),
			(Double)msg.getProperty(SPEED_KEY),
			(time_interval == null ? 0 : (Double) time_interval),
			null
		);
		Integer pathSize = (Integer)msg.getProperty(PATH_SIZE_KEY);
		if ((pathSize != null) && (pathSize.intValue() > 0)) {
			for (int pathIdx = 0, pathSizeMax = pathSize.intValue(); pathIdx < pathSizeMax; pathIdx++) {
				mo.addToMovePath((LineString)msg.getProperty(PATH_ITEM_KEY + pathIdx));
			}
		}
		System.out.println("Received location update :: " + msg.toString());
		System.out.println(mo);
		host.addLocUpdateMsg(mo);
		
		// Right now we don't need to send a response

		return msg;
	}

	@Override
	public Application replicate() {
		return new LocationUpdateApplication(this);
	}

	/**
	 * Sends a packet with info about where the host is moving to next
	 *
	 * @param host to which the application instance is attached
	 */
	@Override
	public void update(DTNHost host) {
		/*
		World w = SimScenario.getInstance().getWorld();
		int numHosts = w.getNumHosts();
		for (int hostIdx = 0; hostIdx < numHosts; hostIdx++) {
			DTNHost recipient = w.getNodeByAddress(hostIdx);

			MovableObject mo = host.getAndRemoveMovableObject();
			if (mo == null) {
				break;
			}
			Message msg = new Message(host, recipient, MSG_TYPE_VALUE + "_" +
					SimClock.getIntTime() + "-" + host.getAddress(),
					MSG_SIZE);
			msg.addProperty(MSG_TYPE_KEY, MSG_TYPE_VALUE);
			msg.addProperty(CURR_COORD_KEY, mo.getStartingLoc());
			msg.addProperty(DEST_COORD_KEY, mo.getEndingLoc()); 
			msg.addProperty(SPEED_KEY, mo.getSpeed());
			msg.addProperty(INTERVAL_TIME_KEY, mo.getIntervalTime());
			if ((mo.getMovePath() != null) && (mo.getMovePath().size() > 0)) {
				msg.addProperty(PATH_SIZE_KEY, mo.getMovePath().size());
				for (int pathIdx = 0; pathIdx < mo.getMovePath().size(); pathIdx++) {
					msg.addProperty(PATH_ITEM_KEY + pathIdx, mo.getMovePath().get(pathIdx)); 
				}
			}
			msg.setAppID(APP_ID);
			host.createNewMessage(msg);

			// Call listeners
			super.sendEventToListeners(EVENT_NAME, null, host);
		}
		*/
	}

}
