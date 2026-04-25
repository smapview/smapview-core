package com.smapview.core.map;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Stack;

import com.smapview.core.map.NodeType.TypeField;
import com.smapview.core.map.NodeType.ValueField;
import com.smapview.core.map.NodeType.ValueType;

import jakarta.json.Json;
import jakarta.json.stream.JsonGenerator;

class NodeBuilder {
	
	class NodeContext {
		
		final String shortPath;
		
		final NodeType nodeType;
		
		final int nodeId;
		
		Object[] data; 	

		NodeContext(String shortPath) {
			this.shortPath = shortPath;
			this.nodeType = pool.schema.getShortPathType(shortPath);
			this.nodeId = pool.getNodeId(shortPath);
		}

		NodeContext(String name, MapFolder folder, NodeType type) {
			this.shortPath = Node.buildPath(context.peek().shortPath, folder.alias, name);
			this.nodeType = type;
			this.nodeId = pool.getNodeId(shortPath);
		}

		void close() {
			StringWriter json = new StringWriter();
			JsonGenerator gen = Json.createGenerator(json);
			gen.writeStartObject();
			// TODO complete this to prepare VALUES in JSON format
			// do not write key values (skip them) as they are stored in PJOINS
			gen.writeEnd();
			// TODO write node values to the database
		}

	}
	
	final NodePool pool;
	
	final String basePath;
	
	final Stack<NodeContext> context = new Stack<>();
		
	public NodeBuilder(NodePool pool, String basePath) {
		this.pool = pool;
		this.basePath = basePath;
		context.add(new NodeContext(basePath));
	}
	
	public MapSchema getSchema() {
		return pool.schema;
	}
	
	public String getBasePath() {
		return basePath;
	}
		
	public String getCurrentPath() {
		if (context.isEmpty()) throw new IllegalStateException();
		return pool.schema.toFullPath(context.peek().shortPath);
	}

	/**
	 * Adds a node to this builder, based on current path.
	 * 
	 * This method starts from the node at the current path to find a
	 * suitable folder for the added node, alternatively looking at 
	 * parent node folders, iterating over parent nodes up to the
	 * node at this builder base path.
	 * <p>
	 * If a suitable folder is found, then the node is added into that folder
	 * and the current path is changed to the added node path. An exception
	 * is thrown if no suitable folder can be found.
	 * 
	 * @param typeName  The node type name.
	 * @param nodeName  The node name.
	 * 
	 * @return This builder.
	 *         
	 * @throws NodeBuildException If not able to find a suitable folder for the node.
	 */
	public NodeBuilder addNode(String typeName, String nodeName) throws NodeBuildException {
		if (context.isEmpty()) throw new IllegalStateException();
		// TODO complete this
		// TODO do not forget to update context and call NodeContext.close() 
		return this;
	}

	/**
	 * Gets a node based on the current builder context.
	 * 
	 * The specified node can be:
	 * <ul>
	 * <li> The node selected from {@link NodePool#newNodeBuilder(String)}
	 * <li> Any view node contained in above node
	 * <li> Any node added from a previous call to {@link #addNode(String, String)}
	 * </ul>
	 * <p>
	 * If the node is found, then this builder current path is changed to the found 
	 * node path. An exception is thrown if the node cannot be found.
	 * 
	 * @param typeName  The node type name.
	 * @param nodeName  The node name.
	 * 
	 * @return This builder.
	 * 
	 * @throws NodeBuildException If the specified node cannot be found.
	 */
	public NodeBuilder getNode(String typeName, String nodeName) {
		if (context.isEmpty()) throw new IllegalStateException();
		// TODO complete this
		// TODO do not forget to update context and call NodeContext.close() 
		return this;
	}

	/**
	 * Keeps a node unmodified, so it does not get removed from the map.
	 * 
	 * This method also keeps all contained nodes unmodified. 
	 * 
	 * The specified node can be:
	 * <ul>
	 * <li> The node selected from {@link NodePool#newNodeBuilder(String)}
	 * <li> Any view node contained in above node
	 * <li> Any node added from a previous call to {@link #addNode(String, String)}
	 * </ul>
	 * <p>
	 * If the node is found, then this builder current path is changed to the found 
	 * node path. An exception is thrown if the node cannot be found.
	 * 
	 * @param typeName  The node type name.
	 * @param nodeName  The node name.
	 * 
	 * @return This builder.
	 */
	public NodeBuilder keepNode(String typeName, String nodeName) {
		if (context.isEmpty()) throw new IllegalStateException();
		// TODO complete this (set FLAG to K in PNODES table)
		// TODO do not forget to update context and call NodeContext.close() 
		return null;		
	}
	
	/**
	 * Sets a field value on the node at the current path.
	 *  
	 * @param fieldName  The field name, for which value is set.
	 * @param value      The new value for the specified field.
	 * 
	 * @return This builder.
	 * 
	 * @throws NodeBuildException If the specified field already exists and 
	 *                            does not have a compatible value type.   
	 */
	public NodeBuilder set(String fieldName, String value) throws SchemaBuildException, NodeBuildException {
		if (context.isEmpty()) throw new IllegalStateException();
		set(getOrCreateField(fieldName, ValueType.STRING, false), value);
		return this;
	}
	
	private ValueField getOrCreateField(String fieldName, ValueType type, boolean list) 
			throws NodeBuildException, SchemaBuildException 
	{
		NodeContext node = context.peek();
		TypeField field = pool.schema.getField(node.nodeType, fieldName);
		if (field == null) {
			ValueField newField = new ValueField(node.nodeType, fieldName, type, list);
			pool.schema.build();
			return newField;
		}
		else if (field instanceof ValueField existingField) {
			if (existingField.valueType != type || existingField.list != list) {
				throw new NodeBuildException("Invalid value type");
			}
			return existingField;
		}
		else throw new NodeBuildException("Invalid field type");		
	}
	
	private void set(ValueField field, Object value) {
		Object[] data = context.peek().data;
		if (data == null) data = new Object[field.type.dataLength];
		else if (field.dataIndex >= data.length) {
			data = Arrays.copyOf(data, field.type.dataLength);
		}
		data[field.dataIndex] = value;
	}

	/**
	 * Links this builder node to another node.
	 * 
	 * @param linkName  The link name.
	 * @param nodePath  The target node path.
	 * 
	 * @return This builder.
	 */
	public NodeBuilder linkNode(String linkName, String nodePath) 
	{
		if (context.isEmpty()) throw new IllegalStateException();
		// TODO complete this
		return this;
	}
	
	/**
	 * Links this builder node to other nodes, based on a key value.
	 * 
	 * This method links to all nodes having the specified key value. 
	 * 
	 * @param linkName  The link name.
	 * @param keyValue  The target node key value.
	 * 
	 * @return This builder.
	 */
	public NodeBuilder joinKey(String linkName, String keyValue) 
	{
		if (context.isEmpty()) throw new IllegalStateException();
		// TODO complete this
		return this;
	}
	
	/**
	 * Completes this builder, flushing all information to the database.
	 */
	public void build() {
		while (!context.isEmpty()) {
			context.pop().close();
		}
	}
	
}
