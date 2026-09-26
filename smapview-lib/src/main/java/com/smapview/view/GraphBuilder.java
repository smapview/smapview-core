package com.smapview.view;

import java.util.Date;
import java.util.List;
import java.util.Stack;

import com.smapview.view.NodeInfo.NodeFlag;

public class GraphBuilder implements AutoCloseable {

	static final int MUTATION_BATCH_SIZE = 1;
	
	final GraphUpdate update;
		
	final Stack<NodeData> nodeStack = new Stack<>();
		
	private MutationBatch batch = null;
	
	private NodeField buildPath = null;
		
	GraphBuilder(GraphUpdate update) {
		this.update = update;
	}
	
	public GraphBuilder setLinkSpaces(String... spaceNames) {
		if (!nodeStack.isEmpty()) throw new IllegalStateException();
		else update.setLinkSpaces(spaceNames);
		return this;
	}
	
	private NodeField getPathField(String pathField) {
		NodeField field = getCurrentNode().nodeType.getField(pathField);
		if (field.isPath()) return field;
		else throw new IllegalArgumentException("Not a path field: "+field);
	}
		
	public GraphBuilder path(String pathField) {
		if (nodeStack.isEmpty()) throw new IllegalStateException();
		buildPath = getPathField(pathField);
		return this;
	}

	public GraphBuilder node(String nodeType) 
			throws GraphBuilderException 
	{
		NodeType resolvedType = update.context.view.getNodeType(nodeType);
		if (nodeStack.isEmpty()) {
			if (!update.context.view.getRootType().canBeCreatedWith(resolvedType)) {
				throw new GraphBuilderException("Invalid node type for root node");
			}
			nodeStack.push(new NodeData(resolvedType));
		}
		else {
			if (buildPath != null) {
				if (!buildPath.getValueNodeType().canBeCreatedWith(resolvedType)) {
					throw new GraphBuilderException("Invalid node type for selected path");
				}
			}
			else {
				buildPath = getCurrentNode().nodeType.fieldSet.getDefaultPathTo(resolvedType);
			}
			NodeData currentNode = getCurrentNode();
			if (!currentNode.hasPointer()) {
				throw new GraphBuilderException("Parent pointer field not set: " + 
						currentNode.nodeType.fieldSet.pointerField);
			}
			NodeData childNode = new NodeData(resolvedType);
			childNode.parentPath = buildPath;
			childNode.parentType = currentNode.nodeType;
			currentNode.add(buildPath, childNode);
			nodeStack.push(childNode);
			buildPath = null;
		}
		return this;
	}

	public GraphBuilder set(String field, String value) 
			throws GraphBuilderException 
	{
		NodeData data = getCurrentNode();
		NodeField sfield = data.nodeType.getField(field);
		if (sfield.isList()) safeSet(data, sfield, new String[] { value });
		else safeSet(data, sfield, value);
		return this;
	}

	public GraphBuilder set(String field, String[] values) 
			throws GraphBuilderException 
	{
		return safeSet(field, values);
	}

	public GraphBuilder set(String field, Date value) 
			throws GraphBuilderException 
	{
		return safeSet(field, value);
	}

	private GraphBuilder safeSet(String field, Object value) 
			throws GraphBuilderException 
	{
		NodeData data = getCurrentNode();
		safeSet(data, data.nodeType.getField(field), value);
		return this;
	}

	private static void safeSet(NodeData data, NodeField field, Object value) 
			throws GraphBuilderException 
	{
		if (field.isLink()) {
			if (field.isList()) data.set(field, toJoinValues(field, (String[])value));
			else data.set(field, toJoinValues(field, (String)value));
		}
		else try {
			data.set(field, value);
		}
		catch (IllegalArgumentException e) {
			throw new GraphBuilderException(e.getMessage());
		}
	}
	
	static JoinValue[] toJoinValues(NodeField linkField, String... values) {
		JoinValue[] result = new JoinValue[values.length];
		LinkStrategy ls = linkField.getLinkStrategy();
		if (ls.singleJoin != null) for (int i=0; i<values.length; i++) {
			result[i] = new JoinValue(values[i], ls.singleJoin.joinId);
		}
		else loopValues: for (int i=0; i<values.length; i++) {
			String strVal = values[i];
			for (JoinStrategy js : ls.joinStrategies) {
				if (strVal.length() > (js.keyName.length() + 1)
						&& strVal.startsWith(js.keyName)
						&& strVal.charAt(js.keyName.length()) == ':')
				{
					String joinKey = strVal.substring(js.keyName.length() + 1);
					result[i] = new JoinValue(joinKey, js.joinId);
					continue loopValues;
				}
				else throw new IllegalArgumentException(
						"Value not matching any join key: " + strVal);
			}
		}
		return result;
	}

	public GraphBuilder endNode() 
			throws GraphBuilderException, ViewRequestException 
	{
		switch (nodeStack.size()) {
		case 0 : 
			throw new IllegalStateException();
		case 1:
			update.buildComplete();
			break;
		case 2:
			try {
				NodeData data = getCurrentNode();
				data.checkMandatoryFields();
				mapToGraph(getRootInfo(), "/" + data.parentPath.fieldName, data);
				update.collectJoinValues(data, nodeStack.getFirst());
				addToBatch(data);
			}
			catch (IllegalStateException e) {
				throw new GraphBuilderException(e.getMessage());
			}
			break;
		}
		nodeStack.pop();
		return this;
	}
			
	@Override
	public void close() 
			throws Exception 
	{
		update.builderClosed();
	}
	
	NodeData getCurrentNode() {
		return nodeStack.peek();
	}
	
	private void addToBatch(NodeData data) 
			throws GraphBuilderException, ViewRequestException 
	{
		if (batch == null) batch = new MutationBatch(this, MUTATION_BATCH_SIZE);
		batch.add(data);
		if (batch.size() == MUTATION_BATCH_SIZE) {
			batch.execute();
			batch = null;
		}
	}
	
	private void mapToGraph(NodeInfo baseNode, String basePath, NodeData nodeData) 
			throws GraphBuilderException 
	{
		if (nodeData.nodeInfo != baseNode) {
			String nodePath = basePath + "/" + nodeData.getPointer();
			nodeData.nodeInfo = update.getNodeInfo(nodePath);
			if (nodeData.nodeInfo == null) {
				nodeData.nodeInfo = new NodeInfo(baseNode);
				nodeData.nodeInfo.add(NodeFlag.NEW, NodeFlag.BUILDING);
				update.mapNode(nodePath, nodeData.nodeInfo);
			} else if (nodeData.nodeInfo.parentNode == baseNode) {
				nodeData.nodeInfo.add(NodeFlag.BUILDING);
			} else {
				throw new GraphBuilderException("Duplicate node reference");
			}
			for (NodeField nodeField : nodeData.getFields()) {
				if (nodeField.isPath()) {
					List<NodeData> list = nodeData.unsafeGet(nodeField);
					if (list != null) for (NodeData childNodeData : list) {
						mapToGraph(nodeData.nodeInfo, 
								nodePath + "/" + nodeField.fieldName, 
								childNodeData);
					}
				}
			}
		}
	}
	
	private NodeInfo getRootInfo() throws ViewRequestException {
		if (nodeStack.isEmpty()) throw new IllegalStateException();
		else return update.getOrCreateRoot(nodeStack.getFirst());			
	}
				
}
