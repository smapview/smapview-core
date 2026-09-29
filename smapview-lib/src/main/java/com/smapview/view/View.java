package com.smapview.view;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.smapview.view.ViewRequest.Context;

public class View {

	static final String QUERY_TYPES = "__schema { types { name, kind, "
			+ "enumValues { name }, possibleTypes { name }, "
			+ "fields { name, type { name, kind, ofType { name } } } "
			+ "} }";
	
	final URI graphqlEndpoint;
	
	final URI mutateEndpoint;
	
	final HttpClient client;

	private NodeType rootType;

	private Map<String,NodeType> nodeTypes;

	private List<LinkStrategy> linkStrategies;

	private List<JoinStrategy> joinStrategies;

	private ViewUpdate update;
			
	View(String dgraphpUrl) {
		this.graphqlEndpoint = URI.create(dgraphpUrl + "/graphql");
		this.mutateEndpoint = URI.create(dgraphpUrl + "/mutate?commitNow=true");
		this.client = HttpClient.newHttpClient();
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
			return (update = new ViewUpdate(this));
		}
	}
	
	void initFrom(ViewBuilder builder) throws GraphSchemaException {
		this.rootType = builder.getRootType();
		this.nodeTypes = HashMap.newHashMap(builder.listNodeTypes().size());
		this.linkStrategies = new ArrayList<>(builder.listLinkStrategies());
		this.joinStrategies = new ArrayList<>(builder.listJoinStrategies());
		for (NodeType type : builder.listNodeTypes()) {
			nodeTypes.put(type.typeName, type);
		}
		mapToTypes(buildFieldSet(rootType, new HashSet<>()));
	}
			
	public NodeType getNodeType(String name) {
		NodeType result = nodeTypes.get(name);
		if (result != null) {
			if (result.isAbstract() && ! result.hasPossibleTypes()) {
				throw new IllegalArgumentException("Type " + result.typeName + 
						" not implemented");
			}
			else return result;
		}
		else throw new IllegalArgumentException("Cannot find node type: " + name);
	}
		
	public Collection<NodeType> listNodeTypes() {
		return Collections.unmodifiableCollection(nodeTypes.values());
	}

	public NodeType getRootType() {
		return rootType;
	}
		
	private GraphFieldSet buildFieldSet(NodeType type, Set<NodeType> visitedTypes) 
			throws GraphSchemaException 
	{
		if (!visitedTypes.add(type)) throw new GraphSchemaException("Path cycle detected");
		GraphFieldSet fieldSet = new GraphFieldSet(type);
		for (NodeField field : type.getFields()) {
			field.prepareForInput();
			List<Class<?>> writeableTypes = ViewRequest.listInputTypes(field.fieldType);
			if (field.allowsInput() && (! field.isLink()) 
					&& ! writeableTypes.contains(field.getInputType()))
			{
				throw new GraphSchemaException("Unsupported input type for " + field +
						": " + field.getInputType().getName());
			}
			if (field.isPath()) {
				fieldSet.addPath(field, 
						buildFieldSet(field.getValueNodeType(), visitedTypes));
			}
			if (field.isLink() && field.getLinkStrategy().joinStrategies.isEmpty()) {
				throw new GraphSchemaException("Missing join for link field: " + field);
			}
		}
		if (type.isAbstract()) {
			for (NodeType ptype : type.getPossibleTypes()) {
				GraphFieldSet fragment = buildFieldSet(ptype, visitedTypes);
				fieldSet.addFragment(fragment);
			}
		}
		else {
			if (fieldSet.idField == null) {
				throw new GraphSchemaException("Missing ID field for " + type);
			}
			if (fieldSet.pointerField == null) {
				throw new GraphSchemaException("Missing pointer field for " + type);
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

	ViewRequest newDqlDelete() {
		// TODO implement this
		throw new UnsupportedOperationException();
	}

	LinkStrategy getLinkStrategy(short linkId) {
		return linkStrategies.get(linkId - 1);
	}
	
	JoinStrategy getJoinStrategy(short joinId) {
		return joinStrategies.get(joinId - 1);
	}
	
}
