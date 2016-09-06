/*
  The Intersection class defines how vehicles cross the intersection
*/
package routing;

import java.lang.*;
import core.DTNHost;
import core.Message;
import core.World;
import input.MessageCreateEvent;
import core.Settings;
import core.Connection;
import core.SimError;

import java.io.Serializable;

import java.util.*;

public class IntersectionRouter extends ActiveRouter {

    //NS_EW: true- NS = Green
    //NS_EW: false- EW = Green

    private boolean NS_EW = false;
    private int GREEN =0, YELLOW = 1, RED =2;
    private double time, max;
    private long delay_time;

    private List<DTNHost> vehicles;

    public IntersectionRouter(Settings s) {
        super(s);
    }

    protected IntersectionRouter(IntersectionRouter r) {
        super(r);
    }

    private void switchLights(){
		NS_EW = !NS_EW;
    }

    private long delay() {
    	try {
    		Thread.sleep(delay_time);
    	}
    	catch (InterruptedException e) {
    		System.out.println("Interrupted");
    	}
    	return delay_time;
    }

    @Override
    public boolean createNewMessage(Message msg) {
        makeRoomForNewMessage(msg.getSize());
        addToMessages(msg, true);
        return true;
    }

    public void sendMessage(boolean NS_EW, int color){

        
        //get neighbors
        vehicles = getNeighbors(NS_EW);
        DTNHost host = getHost();
        for (DTNHost neighbor: vehicles) {

            Message msg = new Message(host,neighbor,"Green",1);
            msg.setTtl(this.msgTtl);
            msg.addProperty("Color", color);
            if(!createNewMessage(msg))
            {
                throw new SimError("Message could not be created");
            }
            host.createNewMessage(msg);

            // Received a pong reply
            // Send event to listeners
            //super.sendEventToListeners("GotMsg", null, host);
            //super.sendEventToListeners("SentMsg", null, host);
        }
//        tryMessagesForConnected();
    }

    // @Override
    // public Message handle(Message msg, DTNHost host) {
    //     String type = (String)msg.getProperty("type");
    //     if (type==null) return msg; // Not a ping/pong message

    //     // Respond with pong if we're the recipient
    //     if (msg.getTo()==host && type.equalsIgnoreCase("ping")) {
    //         String id = "pong" + SimClock.getIntTime() + "-" +
    //             host.getAddress();
    //         Message m = new Message(host, msg.getFrom(), id, getPongSize());
    //         m.addProperty("type", "pong");
    //         m.setAppID(APP_ID);
    //         host.createNewMessage(m);

    //         // Send event to listeners
    //         super.sendEventToListeners("GotPing", null, host);
    //         super.sendEventToListeners("SentPong", null, host);
    //     }

    //     // Received a pong reply
    //     if (msg.getTo()==host && type.equalsIgnoreCase("pong")) {
    //         // Send event to listeners
    //         super.sendEventToListeners("GotPong", null, host);
    //     }

    //     return msg;
    // }


    // @override
    // protected List<Tuple<Message, Connection>> getMessagesForConnected() {
    //     if (getNrofMessages() == 0 || getConnections().size() == 0) {
    //         /* no messages -> empty list */
    //         return new ArrayList<Tuple<Message, Connection>>(0);
    //     }

    //     List<Tuple<Message, Connection>> forTuples =
    //         new ArrayList<Tuple<Message, Connection>>();
    //     for (Message m : getMessageCollection()) {
    //         for (Connection con : getConnections()) {
    //             DTNHost to = con.getOtherNode(getHost());
    //             if (m.getTo() == to) {
    //                 forTuples.add(new Tuple<Message, Connection>(m,con));
    //             }
    //         }
    //     }

    //     return forTuples;
    // }

    private List<DTNHost> getNeighbors(boolean NS_EW) {

        DTNHost me = getHost();
        ArrayList<DTNHost> neighbors = new ArrayList<DTNHost>();
        for (Connection c : getConnections()) {
            DTNHost neighbor = c.getOtherNode(me);
            //TODO get particular neighbors
            if (neighbor!=null) {
                if(NS_EW) {

                    neighbors.add(neighbor);
                }
                else {
                    neighbors.add(neighbor);
                }

            }
        }

        return neighbors;
    }

    @Override
    public void update() {
        Vector<String> messages = new Vector<String>();
        super.update();

        if (isTransferring() || !canStartTransfer()) {
            return; /* transferring, don't try other connections yet */
        }

        /* Try first the messages that can be delivered to final recipient */
        if (exchangeDeliverableMessages() != null) {
            return;
        }
        //need to send message to all connected
        for(;time<max;) {
          //East-West Light is green
          NS_EW = true;
          sendMessage(NS_EW,GREEN);
          time += delay();
          //after time out
          NS_EW = false;
          sendMessage(NS_EW,GREEN);
        }

        /* see if need to drop some messages... 
        for (Message m : getMessageCollection()) {
            peerMsgCount = getPeerMessageCount(m);
            if (peerMsgCount < this.countRange[0] ||
                    peerMsgCount > this.countRange[1]) {
                messagesToDelete.add(m.getId());
            }
        }
        for (String id : messagesToDelete) { /* ...and drop them 
            this.deleteMessage(id, true);
        }
        */
    }

    @Override
    public IntersectionRouter replicate() {
        return new IntersectionRouter(this);
    }

    
}




