package com.smapview.view;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.smapview.view.NodeField.ValueType;
import com.smapview.view.ViewRequest.Context;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

public class View {

	static final Pattern QUALIFIED_FIELDNAME = Pattern.compile("([A-Z][a-zA-Z_]*)\\.([a-z_][a-zA-Z_]*)");

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
	 * Adds a strategy to link nodes based on join fields.
	 * <p>
	 * Join fields provide join values that can be compared: when a "source" node has a join value
	 * matching (equality) a join value provided by another "target" node, then the source node
	 * is linked to the target node. When join values get changed and no longer match, then the 
	 * link is deleted.
	 * <p>
	 * This method identifies a "link field" to be updated based on value matches between a source 
	 * join field and a target join field. Fields are specified with the format "TypeName.fieldName"
	 * and the link field must be available on the object type denoted by the source join field.
	 * For example if type <code>Post</code> implements interface <code>Authored</code> then one 
	 * can add a strategy as follows:
	 * <pre>
	 *   addLinkStrategy("Authored.by", "Post.authorEmail", "Author.email");
	 * </pre> 
	 * The link field (and inverse link field) must be of GraphQL object list type, so objects 
	 * can be added and removed from the lists during graph updates based on join value matches. 
	 * Join fields must be of type <code>ID</code>, <code>String</code> or <code>[String]</code>
	 * and are usually transient fields not reflected in the view (if they have a name starting 
	 * with underscore, for example "_productId").
	 * <p>
	 * It is not possible to mix different link scopes for the same link field.
	 * <p>
	 * @param linkField         The link field to be updated when resolving node links.
	 * @param inverseLinkField  The inverse link associated with the specified link field.
	 * @param sourceJoinField   The source join field, which provides join values for source nodes.  
	 * @param targetJoinField   The target join field, which provides join values for target nodes.
	 * @param linkScope         Tells what scope to use when resolving links (graph or spaces).
	 */
	public void addLinkStrategy(String linkField, String inverseLinkField,
			String sourceJoinField, String targetJoinField, LinkScope linkScope) 
	{
		// TODO check if compatible with existing strategies
		// TODO check field references through GraphQL introspection
		// TODO tag fields and set their inverseField
		linkStrategies.add(new LinkStrategy(getLinkField(linkField), getLinkField(inverseLinkField), 
				getJoinField(sourceJoinField), getJoinField(targetJoinField), linkScope, typeMap));
	}
	
	public void tagPathField(String pathField, String inversePathField) {
		NodeField field = getNodeField(pathField);
		NodeField inverseField = getNodeField(inversePathField);
		if (field.valueType == ValueType.NODE && field.isList()) {
			field.add(FieldTag.PATH);
			field.inverseField = inverseField;
			mapNodeType(field.valueSchemaType);
		}
		else throw new IllegalArgumentException("Invalid field type");
	}
	
	public void tagPointerField(String pointerField) {
		NodeField field = getNodeField(pointerField);
		if (field.valueType == ValueType.STRING) field.add(FieldTag.POINTER);
		else throw new IllegalArgumentException("Invalid field type");
		if (field.declaringType.isInterface) {
			for (NodeType type : field.declaringType.possibleTypes) {
				tagPointerField(type.typeName + "." + field.fieldName);
			}
		}
	}

	public void tagTimestampField(String timestampField) {
		NodeField field = getNodeField(timestampField);
		if (field.valueType == ValueType.DATE_TIME) field.add(FieldTag.TIMESTAMP);
		else throw new IllegalArgumentException("Invalid field type");
	}

	private NodeField getNodeField(String qualifiedFieldName) {
		Matcher m = QUALIFIED_FIELDNAME.matcher(qualifiedFieldName);
		if (m.matches()) {
			String typeName = m.group(1);
			String fieldName = m.group(2);
			return mapNodeType(typeName).getField(fieldName);
		}
		else throw new IllegalArgumentException("Not a qualified field name: "+qualifiedFieldName);
	}

	private NodeField getLinkField(String qualifiedFieldName) {
		NodeField field = getNodeField(qualifiedFieldName);
		if (field.valueType == ValueType.NODE) {
			if (field.tags.length == 0) { 
				field.add(FieldTag.LINK);
				// make sure the type exists
				getNodeType(field.valueSchemaType);
				return field;
			}
			else throw new IllegalArgumentException("Field already tagged: " + field);
		}
		else throw new IllegalArgumentException("Invalid join field type for " + field);
	}

	private NodeField getJoinField(String qualifiedFieldName) {
		NodeField field = getNodeField(qualifiedFieldName);
		if (field.has(FieldTag.JOIN)) return field;
		else if (field.valueType == ValueType.ID
				|| field.valueType == ValueType.STRING) 
		{
			field.add(FieldTag.JOIN);
			return field;
		}
		else throw new IllegalArgumentException("Invalid link field type for " + field);
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
		for (NodeField field : type.fields) {
			if (field.hasAny(FieldTag.PATH, FieldTag.LINK)) {
				field.valueNodeType = getNodeType(field.valueSchemaType); 
			}
			if (field.has(FieldTag.PATH)) {
				fieldSet.addPath(field, buildFieldSet(field.valueNodeType, visitedTypes));
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
		for (NodeField pathField : fieldSet.listPathFields()) {
			mapToTypes(fieldSet.getPathFieldSet(pathField.fieldName));
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
