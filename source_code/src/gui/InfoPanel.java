/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import movement.Path;
import movement.ShortestPathMapBasedMovement;
import movement.map.MapNode;
import core.Coord;
import core.DTNHost;
import core.Message;

/**
 * Information panel that shows data of selected messages and nodes.
 */
public class InfoPanel extends JPanel implements ActionListener{
	private JComboBox msgChooser;
	private Map<Coord, MapNode> mappedNodes;
	private JComboBox<Coord> coordChooser;
	private JComboBox<Coord> neighborChooser;
	private JLabel info;
	private JButton infoButton;
	private JButton routingInfoButton;
	private JButton msgPropsButton;
	private Message selectedMessage;
	private DTNHost selectedHost;
	private DTNSimGUI gui;

	public InfoPanel(DTNSimGUI gui) {
		this.gui = gui;
		reset();
	}

	private void reset() {
		this.removeAll();
		this.repaint();
		this.info = null;
		this.infoButton = null;
		this.routingInfoButton = null;
		this.msgPropsButton = null;
		this.selectedMessage = null;
		this.selectedHost = null;
	}

	/**
	 * Show information about a host
	 * @param host Host to show the information of
	 */
	public void showInfo(DTNHost host) {
		Vector<Message> messages =
			new Vector<Message>(host.getMessageCollection());
		Collections.sort(messages);
		reset();
		this.selectedHost = host;
		String text = (host.isMovementActive() ? "" : "INACTIVE ") + host +
			" (" + this.selectedHost.getGroupType() + ")" +
			" at " + host.getLocation();

		msgChooser = new JComboBox(messages);
		msgChooser.insertItemAt(messages.size() + " messages", 0);
		msgChooser.setSelectedIndex(0);
		msgChooser.addActionListener(this);

		routingInfoButton = new JButton("routing info");
		routingInfoButton.addActionListener(this);

		this.add(new JLabel(text));
		this.add(msgChooser);
		this.add(routingInfoButton);

		/* Testing tool to display routes between nodes
		mappedNodes = ((ShortestPathMapBasedMovement)host.getMovementModel()).getMap().getMappedNodes();
		Coord[] coords = mappedNodes.keySet().toArray(new Coord[0]);
		Arrays.sort(coords);
		coordChooser = new JComboBox<Coord>(coords);
		coordChooser.addActionListener(this);
		this.add(coordChooser);

		neighborChooser = new JComboBox<Coord>();
		neighborChooser.addActionListener(this);
		this.add(neighborChooser);
		selectCoordBegin((Coord)coordChooser.getSelectedItem());
		*/

		this.revalidate();
		this.getParent().revalidate();
	}

	/**
	 * Show information about a message
	 * @param message Message to show the information of
	 */
	public void showInfo(Message message) {
		reset();
		this.add(new JLabel(message.toString()));
		setMessageInfo(message);
		this.revalidate();
		this.getParent().revalidate();
	}

	private void setMessageInfo(Message m) {
		int ttl = m.getTtl();
		String txt = " [" + m.getFrom() + "->" + m.getTo() + "] " +
				"size:" + m.getSize() + ", UI:" + m.getUniqueId() +
				", received @ " + String.format("%.2f", m.getReceiveTime());
		if (ttl != Integer.MAX_VALUE) {
			txt += " TTL: " + ttl;
		}


		if (this.info == null) {
			this.info = new JLabel(txt);
			this.add(info);
		} else {
			this.info.setText(txt);
		}
		
		if ((m.getPropertyNames() != null) && (m.getPropertyNames().size() > 0)) {
			this.msgPropsButton = new JButton("properties");
			this.add(msgPropsButton);
			msgPropsButton.addActionListener(this);
		}

		String butTxt = "path: " + (m.getHops().size()-1) + " hops";
		if (this.infoButton == null) {
			this.infoButton = new JButton(butTxt);
			this.add(infoButton);
			infoButton.addActionListener(this);
		} else {
			this.infoButton.setText(butTxt);
		}
	
		this.selectedMessage = m;
		infoButton.setToolTipText("path:" + m.getHops());

		this.revalidate();
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == msgChooser) {
			if (msgChooser.getSelectedIndex() == 0) { // title text selected
				return;
			}
			Message m = (Message)msgChooser.getSelectedItem();
			setMessageInfo(m);
		}
		else if (e.getSource() == this.infoButton) {
			Path p = new Path();
			for (DTNHost h : this.selectedMessage.getHops()) {
				p.addWaypoint(h.getLocation());
			}

			this.gui.showPath(p);
		}
		else if (e.getSource() ==  this.routingInfoButton) {
			new RoutingInfoWindow(this.selectedHost);
		}
		else if (e.getSource() ==  this.msgPropsButton) {
			new MessagePropertiesWindow(this.selectedMessage);
		}
		else if (e.getSource() ==  this.coordChooser) {
			Coord coord = (Coord)coordChooser.getSelectedItem();
			selectCoordBegin(coord);
		}
		else if (e.getSource() ==  this.neighborChooser) {
			Coord coordBegin = (Coord)coordChooser.getSelectedItem();
			Coord coordEnd = (Coord)neighborChooser.getSelectedItem();
			displayLine(coordBegin, coordEnd);
		}
	}
	
	private void selectCoordBegin(Coord coordBegin) {
		MapNode node = this.mappedNodes.get(coordBegin);
		if (node == null) {
			for (Map.Entry<Coord, MapNode> entry : this.mappedNodes.entrySet()) {
				if ((entry.getKey().getX() == coordBegin.getX()) &&
						(entry.getKey().getY() == coordBegin.getY())) {
					node = entry.getValue();
					break;
				}
			}
		}
		this.neighborChooser.removeAllItems();
		if (node == null) {
			return;
		}
		for (MapNode neighbor : node.getNeighbors()) {
			this.neighborChooser.addItem(neighbor.getLocation());
		}
		Coord coordEnd = (Coord)neighborChooser.getSelectedItem();
		displayLine(coordBegin, coordEnd);
		
	}
	
	private void displayLine(Coord coordBegin, Coord coordEnd) {
		if ((coordBegin != null) && (coordEnd != null)) {
			Path p = new Path();
			p.addWaypoint(coordBegin);
			p.addWaypoint(coordEnd);
			this.gui.showPath(p);
		}
	}

}
