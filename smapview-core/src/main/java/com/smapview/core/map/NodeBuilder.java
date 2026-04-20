package com.smapview.core.map;

import java.io.StringWriter;

import jakarta.json.Json;
import jakarta.json.stream.JsonGenerator;

class NodeBuilder {

	final NodePool pool;
	
	final NodeBuilder parent;
	
	NodeType nodeType;
	
	int nodeId;
	
	NodeBuilder(NodePool pool) {
		this(pool, null);
	}

	NodeBuilder(NodePool pool, NodeBuilder parent) {
		this.pool = pool;
		this.parent = parent;
	}

	public MapSchema getSchema() {
		return pool.schema;
	}
		
	/**
	 * Starts building a new node.
	 * 
	 * @param typeName  Node type name.
	 * @param nodeName  Node name.
	 */
	public NodeBuilder newNode(String typeName, String nodeName) {
		// TODO complete this
		return this;
	}
	
	/**
	 * Adds the current build node to this pool.
	 */
	public NodeBuilder add() {
		StringWriter data = new StringWriter();
		JsonGenerator gen = Json.createGenerator(data);
		gen.writeStartObject();
		// TODO complete this
		gen.writeEnd();
		// TODO set NODEID and FLAG when inserting into PNODES
		return parent;
	}
		
	public NodeBuilder link(String linkName) 
	{
		// TODO complete this
		return this;
	}

	public NodeBuilder folder(String linkName) 
	{
		// TODO complete this
		return this;
	}

	public NodeBuilder toNode(String nodePath) 
	{
		// TODO complete this
		return this;
	}

	public NodeBuilder toJoin(String joinPath) 
	{
		// TODO complete this
		return this;
	}
	
	NodeBuilder set(String fieldName, String value) {
		// TODO complete this
		return this;		
	}

	
}
