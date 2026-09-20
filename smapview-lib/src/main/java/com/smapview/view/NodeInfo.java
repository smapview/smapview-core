package com.smapview.view;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.smapview.view.ViewUpdate.LinkSpace;

class NodeInfo {

	enum NodeFlag {
		
		NEW(0x01),
		
		BUILDING(0x02),
		
		UPDATED(0x04);
		
		final int value;
		
		NodeFlag(int value) {
			this.value = value;
		}
		
	}
	
	static final List<NodeInfo> NO_CHILD = Arrays.asList();
		
	final NodeInfo parentNode;

	long timestamp;

	private byte flags = 0;
			
	private List<NodeInfo> childNodes;
	
	private LinkEndpoint endpoint = new LinkEndpoint(getLinkScope());;
	
	NodeInfo(NodeInfo parentNode) {
		this.parentNode = parentNode;
		if (parentNode != null) parentNode.addChild(this);
	}
	
	private void addChild(NodeInfo node) {
		if (childNodes == null) childNodes = new LinkedList<NodeInfo>();
		childNodes.add(node);
	}

	void add(NodeFlag... flags) {
		for (NodeFlag flag : flags)	this.flags |= flag.value; 
	}

	boolean has(NodeFlag flag) {
		return (this.flags & flag.value) != 0; 
	}

	List<LinkSpace> getLinkScope() {
		if (parentNode != null) return parentNode.getLinkScope();
		else throw new UnsupportedOperationException();
	}

	List<NodeInfo> listChildNodes() {
		return childNodes != null? childNodes : NO_CHILD;
	}
	
	String getNodeId() {
		return endpoint.nodeId;
	}
	
	void setNodeId(String nodeId) {
		endpoint.nodeId = nodeId;
	}
	
}
