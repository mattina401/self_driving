/*
 * Based on:
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package gui.playfield;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;

import core.Connection;
import core.Coord;
import core.DTNHost;
import core.NetworkInterface;

import java.awt.Image;

import javax.imageio.*;

import movement.map.MapNode;

import java.io.File;
import java.io.IOException;
import java.awt.Toolkit;

/**
 * Visualization of a Map Node
 *
 */
public class MapNodeGraphic extends PlayFieldGraphic {
	private static Color nodeColor = Color.MAGENTA;

	private MapNode node;

	public MapNodeGraphic(MapNode node) {
		this.node = node;
	}
	
	/**
	 * Return the node being displayed
	 * @return the node being displayed
	 */
	public MapNode getMapNode() {
		return this.node;
	}

	@Override
	public void draw(Graphics2D g2) {
		drawNode(g2);
	}

	/**
	 * Visualize node's location
	 * @param g2 The graphic context to draw to
	 */
	private void drawNode(Graphics2D g2) {
		/* draw node rectangle */
		g2.setColor(nodeColor);
		g2.drawRect(scale(node.getLocation().getX()-1),scale(node.getLocation().getY()-1),
				scale(2),scale(2));

	}
}
