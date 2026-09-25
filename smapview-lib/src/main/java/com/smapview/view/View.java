package com.smapview.view;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;

import com.smapview.view.ViewRequest.Context;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

public class View {

	static final String QUERY_TYPES = "__schema { types { "
			+ "name, kind, possibleTypes { name }, "
			+ "fields { name, type { name, kind, ofType { name } } } "
			+ "} }";
	
	final List<LinkStrategy> linkStrategies = new ArrayList<>();

	final private Map<String,NodeType> typeMap = new HashMap<>();
	
	final URI graphqlEndpoint;
	
	final URI mutateEndpoint;
	
	final HttpClient client;

	private Map<String,JsonObject> schemaTypes;  
	
	private NodeType rootType;
	
	private ViewUpdate update;
		
	public View(String dgraphHttpUrl) throws ViewRequestException {
		this.graphqlEndpoint = URI.create(dgraphHttpUrl + "/graphql");
		this.mutateEndpoint = URI.create(dgraphHttpUrl + "/mutate");
		this.client = HttpClient.newHttpClient();
		loadSchemaTypes();
	}
	
	private void loadSchemaTypes() throws ViewRequestException {
		JsonArray types = new ViewRequest(this, QUERY_TYPES)
				.execToObject()
				.getJsonObject("__schema")
				.getJsonArray("types");
		HashMap<String,JsonObject> map = new HashMap<>();
		for (JsonValue value : types) {
			JsonObject type = (JsonObject)value;
			map.put(type.getString("name"), type);
		}
		this.schemaTypes = Collections.unmodifiableMap(map);
	}
		
	/**
	 * Sets the base node type for all root nodes in view graphs.
	 *   
	 * @param nodeTypeName The node type name for root nodes.
	 */
	public void setRootType(String nodeTypeName) {
		if (rootType == null) {
			rootType = mapNodeType(nodeTypeName);
		}
		else throw new IllegalStateException();
	}
	
	NodeType mapNodeType(String typeName) {
		if (typeMap.containsKey(typeName)) return typeMap.get(typeName);
		if (!schemaTypes.containsKey(typeName)) {
			throw new IllegalArgumentException("Cannot find type " + typeName);
		}
		NodeType type = new NodeType(this, schemaTypes.get(typeName));
		typeMap.put(typeName, type);
		return type;
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
	 * 
	 * @return A link strategy used to resolve links during graph updates. 
	 */
	public LinkStrategy addLinkField(String linkField, String inverseField) {
		LinkStrategy result = new LinkStrategy(this, getNodeField(linkField), getNodeField(inverseField));
		linkStrategies.add(result);
		return result;
	}
	
	public void addPathField(String pathField, String inverseField) {
		getNodeField(pathField).markAsPath(getNodeField(inverseField), this);
	}

	public void addPointerField(String pointerField) {
		getNodeField(pointerField).markAsPointer();
	}

	public void addTimestampField(String timestampField) {
		getNodeField(timestampField).markAsTimestamp();
	}

	NodeField getNodeField(String qualifiedFieldName) {
		Matcher m = NodeField.QUALIFIED_NAME.matcher(qualifiedFieldName);
		if (m.matches()) {
			String typeName = m.group(1);
			String fieldName = m.group(2);
			NodeField result = mapNodeType(typeName).getField(fieldName);
			if (result == null) throw new IllegalArgumentException(
					"Cannot find field: " + qualifiedFieldName);
			else return result;
		}
		else throw new IllegalArgumentException(
				"Not a qualified field name: " + qualifiedFieldName);
	}

	/**
	 * Starts a new view update.
	 * 
	 * @return A <code>ViewUpdate</code> object used to update view graphs.
	 * 
	 * @throws IllegalStateException If an update is already started and is not completed.
	 * 
	 * @see ViewUpdate#isCompleted()
	 */
	public ViewUpdate startUpdate() throws GraphSchemaException {
		if (update != null) throw new IllegalStateException();
		else {
			mapToTypes(buildFieldSet(rootType, new HashSet<>()));
			return (update = new ViewUpdate(this));
		}
	}
		
	public NodeType getNodeType(String typeName) {
		NodeType result = typeMap.get(typeName);
		if (result == null) throw new IllegalArgumentException(
				"Cannot find view node type: "+typeName);
		return result;
	}
		
	public NodeType getRootType() {
		return rootType;
	}

	NodeType getType(String name) throws GraphSchemaException {
		NodeType type = typeMap.get(name);
		if (type == null) throw new GraphSchemaException("Unknown type: "+name);
		else return type;
	}
	
	public Map<String,NodeType> getTypeMap() {
		return Collections.unmodifiableMap(typeMap);
	}
	
	private GraphFieldSet buildFieldSet(NodeType type, Set<NodeType> visitedTypes) 
			throws GraphSchemaException 
	{
		if (!visitedTypes.add(type)) throw new GraphSchemaException("Path cycle detected");
		GraphFieldSet fieldSet = new GraphFieldSet(type);
		for (NodeField field : type.fields.values()) {
			field.prepareForInput();
			if (field.has(FieldTag.PATH)) {
				fieldSet.addPath(field, 
						buildFieldSet(field.getValueNodeType(), visitedTypes));
			}
		}
		if (type.isInterface) {
			for (NodeType ptype : type.possibleTypes) {
				GraphFieldSet fragment = buildFieldSet(ptype, visitedTypes);
				fieldSet.addFragment(fragment);
			}
		}
		else {
			if (fieldSet.idField == null) {
				throw new GraphSchemaException("Missing ID field for "+type);
			}
			if (fieldSet.pointerField == null) {
				throw new GraphSchemaException("Missing pointer field for "+type);
			}
		}
		return fieldSet;
	}
	
	private void mapToTypes(GraphFieldSet fieldSet) {
		fieldSet.type.fieldSet = fieldSet;
		for (GraphFieldSet fragment : fieldSet.listFragments()) {
			mapToTypes(fragment);
		}
		for (Entry<NodeField,GraphFieldSet> path : fieldSet.listPaths()) {
			mapToTypes(path.getValue());
		}
		Common.trace("Mapped field set to %s: %s", fieldSet.type, fieldSet);
		Common.trace("Field roles in field set: %s", fieldSet.listFieldRoles());
	}
	
	ViewRequest newGraphqlQuery() {
		return new ViewRequest(this, Context.GRAPHQL_QUERY);
	}

	ViewRequest newGraphqlMutation() {
		return new ViewRequest(this, Context.GRAPHQL_MUTATION);
	}

	ViewRequest newDqlSet() {
		return new ViewRequest(this, Context.DQL_SET);
	}

}
