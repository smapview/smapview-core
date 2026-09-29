package com.smapview.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

public class ViewBuilder {

	static final String QUERY_TYPES = "__schema { types { name, kind, "
			+ "enumValues { name }, possibleTypes { name }, "
			+ "fields { name, type { name, kind, ofType { name } } } "
			+ "} }";
	
	private Map<String,JsonObject> schemaTypes = new HashMap<>();  

	private Map<String,FieldType> typeMap = new HashMap<>();

	private List<LinkStrategy> linkStrategies = new ArrayList<>();

	private List<JoinStrategy> joinStrategies = new ArrayList<>();

	private LinkStrategy linkStrategy;

	private NodeType rootType;

	private final View view;
		
	ViewBuilder(View view) {
		this.view = view;
	}

	void init() throws ViewRequestException {
		if (schemaTypes.isEmpty()) {
			JsonArray types = new ViewRequest(view, QUERY_TYPES)
					.execToObject(false)
					.getJsonObject("__schema")
					.getJsonArray("types");
			for (JsonValue value : types) {
				JsonObject type = (JsonObject)value;
				schemaTypes.put(type.getString("name"), type);
			}
		}
	}
	
	public static ViewBuilder newBuilder(String dgraphpUrl) throws ViewRequestException {
		ViewBuilder result = new ViewBuilder(new View(dgraphpUrl));
		result.init();
		return result;
	}
	
	public View build() throws GraphSchemaException {
		while (trimTypes() > 0) ;
		view.initFrom(this);
		return view;
	}
	
	private int trimTypes() {
		// keep only node types
		typeMap.entrySet().stream()
		.filter(e -> ! (e.getValue() instanceof NodeType))
		.map(e -> e.getKey())
		.toList().stream()
		.forEach(name -> typeMap.remove(name));
		// exclude node types with no field
		for (FieldType type : typeMap.values()) {
			NodeType nodeType = (NodeType)type;
			nodeType.trimFields();
			if (! nodeType.hasFields()) {
				nodeType.exclude();
			}
		}
		// remove excluded (and thus no longer visible) types
		int count = typeMap.size();
		typeMap.entrySet().stream()
		.filter(e -> ! (e.getValue().isVisible()))
		.map(e -> e.getKey())
		.toList().stream()
		.forEach(name -> typeMap.remove(name));
		int newCount = typeMap.size();
		// return number of removed node types
		return (count - newCount);
	}
		
	/**
	 * Sets the base node type for all root nodes in view graphs.
	 *   
	 * @param nodeTypeName The node type name for root nodes.
	 */
	public ViewBuilder setRootType(String nodeTypeName) {
		if (rootType == null) {
			rootType = getNodeType(nodeTypeName);
		}
		else throw new IllegalStateException();
		return this;
	}
	
	/**
	 * Adds a link field.
	 * <p>
	 * The link field (and inverse link field) must be of GraphQL object list type, so object 
	 * references can be added and removed from the lists during graph updates, based on 
	 * strategy options. 
	 * <p>
	 * @param linkField         The link field to be updated when resolving node links.
	 * @param inverseField      The inverse link associated with the specified link field.
	 */
	public ViewBuilder addLinkField(String linkField, String inverseField) {
		linkStrategy = new LinkStrategy(getNodeField(linkField), getNodeField(inverseField), this);
		return this;
	}
	
	/**
	 * Link the nodes based on join values.
	 * <p>
	 * Target nodes provide join values that can be used by source nodes as references to target 
	 * nodes. Target join values get computed from a value expression and source join values are
	 * provided as input data set on the link field.
	 * <p>
	 * @param valueExpr  The expression used to compute join values on target nodes.  
	 * @param joinScope  Tells what scope to use when resolving links (graph or spaces).
	 */
	public ViewBuilder withSingleJoin(String valueExpr, JoinScope joinScope) {
		if (linkStrategy == null) throw new IllegalStateException();
		else if (! linkStrategy.joinStrategies.isEmpty()) {
			throw new IllegalStateException("Join already defined");
		}
		else {
			linkStrategy.singleJoin = new JoinStrategy(linkStrategy, 
					valueExpr, joinScope, this);
			linkStrategy.joinStrategies.add(linkStrategy.singleJoin);
		}
		return this;
	}

	/**
	 * Link the nodes based on join key values.
	 * <p>
	 * When the resolved link is defined on an interface, this method can be used to compute
	 * join values on a specific type implementing this interfaces.
	 * For example if type <code>Post</code> implements interface <code>Authored</code> then one 
	 * can use this method to provide values for link <code>Authored.by</code> as references to 
	 * <code>Post.authorEmail</code>.
	 * <p>
	 * @param keyName     The key name, used as a prefix in join values associated with that key.  
	 * @param targetType  The target node type associated with the join key.
	 * @param valueExpr   The expression used to compute join values on target nodes.
	 * @param joinScope   Tells what scope to use when resolving links (graph or spaces).
	 */
	public ViewBuilder withJoinKey(String keyName, String targetType, String valueExpr, JoinScope joinScope) {
		if (linkStrategy == null) throw new IllegalStateException();
		else if (linkStrategy.singleJoin != null) {
			throw new IllegalStateException("Single join already defined");
		}
		else if (joinStrategies.stream().anyMatch(j -> keyName.equals(j.keyName))) {
			throw new IllegalStateException("Join key already used: " + keyName);
		}
		else linkStrategy.joinStrategies.add(new JoinStrategy(linkStrategy, 
				keyName, targetType, valueExpr, joinScope, this));
		return this;
	}

	public ViewBuilder addPathField(String pathField, String inverseField) {
		linkStrategy = null;
		getNodeField(pathField).markAsPath(getNodeField(inverseField), this);
		return this;
	}

	public ViewBuilder addPointerField(String pointerField) {
		linkStrategy = null;
		getNodeField(pointerField).markAsPointer();
		return this;
	}

	public ViewBuilder addTimestampField(String timestampField) {
		linkStrategy = null;
		getNodeField(timestampField).markAsTimestamp();
		return this;
	}

	NodeField getNodeField(String qualifiedFieldName) {
		Matcher m = NodeField.QUALIFIED_NAME.matcher(qualifiedFieldName);
		if (m.matches()) {
			String typeName = m.group(1);
			String fieldName = m.group(2);
			NodeField result = getNodeType(typeName).getField(fieldName);
			if (result == null) throw new IllegalArgumentException(
					"Cannot find field: " + qualifiedFieldName);
			else return result;
		}
		else throw new IllegalArgumentException(
				"Not a qualified field name: " + qualifiedFieldName);
	}
		
	NodeType getNodeType(String name) {
		FieldType type = getType(name);
		if (type != null && type instanceof NodeType) {
			NodeType result = (NodeType)type;
			if (result.isVisible()) result.completeWith(this);
			return result;
		}
		else throw new IllegalArgumentException("Cannot find node type: " + name);
	}
		
	FieldType getType(String name) {
		FieldType result = typeMap.get(name);
		if (result == null) {
			JsonObject schemaType = schemaTypes.get(name);
			if (schemaType != null) {
				result = new FieldType(schemaType, this);
				if (EnumType.canCreateFrom(result)) {
					result = new EnumType(result, this);
				}
				else if (NodeType.canCreateFrom(result)) {
					result = new NodeType(result, this);
				}
				typeMap.put(name, result);
				Common.trace("Loaded %s %s from %s", result.getClass().getName(), name, schemaType);
			}
			else throw new IllegalArgumentException("Cannot find type " + name);
		}
		return result;
	}
	
	List<NodeType> listNodeTypes() {
		return typeMap.values().stream()
				.filter(t -> t instanceof NodeType)
				.map(t -> (NodeType)t)
				.filter(t -> t.isVisible())
				.toList();
	}
	
	List<LinkStrategy> listLinkStrategies() {
		return linkStrategies;
	}

	List<JoinStrategy> listJoinStrategies() {
		return joinStrategies;
	}

	short register(LinkStrategy ls) {
		linkStrategies.add(ls);
		return (short)linkStrategies.size();
	}

	short register(JoinStrategy js) {
		joinStrategies.add(js);
		return (short)joinStrategies.size();
	}

	NodeType getRootType() {
		if (rootType == null) throw new IllegalStateException("Missing root type");
		return rootType;
	}

}
