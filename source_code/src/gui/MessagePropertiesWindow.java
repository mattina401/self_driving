/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.Timer;
import javax.swing.tree.DefaultMutableTreeNode;

import routing.util.RoutingInfo;
import core.DTNHost;
import core.Message;
import core.SimClock;

/**
 * A window for displaying message properties
 */
public class MessagePropertiesWindow extends JFrame implements ActionListener {
	private Message msg;
	private JScrollPane treePane;
	private JTree tree;

	public MessagePropertiesWindow(Message msg) {
		Container cp = this.getContentPane();
		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		this.msg = msg;
		this.setLayout(new BorderLayout());
		this.treePane = new JScrollPane();
		updateTree();

		cp.add(treePane, BorderLayout.CENTER);

		this.pack();
		this.setVisible(true);
	}

	private void updateTree() {
		super.setTitle("Message Properties");
		String topNodeInfo = this.msg.toString() + 
				" [" + this.msg.getFrom() + "->" +
				this.msg.getTo() + "]";
		DefaultMutableTreeNode top = new DefaultMutableTreeNode(topNodeInfo);
		Vector<Integer> expanded = new Vector<Integer>();
		
		for (String propKey : this.msg.getPropertyNames()) {
			String childInfo = propKey + " = " + this.msg.getProperty(propKey);
			DefaultMutableTreeNode node = new DefaultMutableTreeNode(childInfo);
			top.add(node);
		}

		if (this.tree != null) { /* store expanded state */
			for (int i=0; i < this.tree.getRowCount(); i++) {
				if (this.tree.isExpanded(i)) {
					expanded.add(i);
				}
			}
		}

		this.tree = new JTree(top);

		for (int i=0; i < this.tree.getRowCount(); i++) { /* restore expanded */
			if (expanded.size() > 0 && expanded.firstElement() == i) {
				this.tree.expandRow(i);
				expanded.remove(0);
			}
		}

		this.treePane.setViewportView(this.tree);
		this.treePane.revalidate();
	}

	public void actionPerformed(ActionEvent e) {
		Object s = e.getSource();
	}

}
