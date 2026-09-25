package com.smapview.view;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.smapview.view.GraphFieldSet.FieldRole;
import com.smapview.view.NodeInfo.NodeFlag;
import com.smapview.view.ViewUpdate.LinkSpace;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.stream.JsonParser;

/**
 * Groups all operations to update a single node tree graph.
 */
class GraphUpdate {

	class RootInfo extends NodeInfo {

		final String pointer;

		final NodeType type;

		public RootInfo(String pointer, NodeType type) throws ViewRequestException {
			super(null);
			this.pointer = pointer;
			this.type = type;
			setNodeId(getRootNodeId());
			if (getNodeId() == null) createRootNode();
		}

		String getRootNodeId() throws ViewRequestException {
			NodeField idField = type.getIdField();
			NodeField pointerField = type.getPointerField();
			ViewRequest request = new ViewRequest(context.view, "%s { %s, %s}", 
					type.toQueryName(), idField.fieldName, pointerField.fieldName);
			JsonArray data = request.execToObject()
					.getJsonArray(type.toQueryName());
			for (int i=0; i<data.size(); i++) {
				JsonObject info = data.getJsonObject(i);
				if (pointer.equals(info.getString(pointerField.fieldName))) {
					return info.getString(idField.fieldName); 
				}
			}
			return null;
		}

		void createRootNode() throws ViewRequestException {
			NodeData data = new NodeData(type);
			data.unsafeSet(type.getPointerField(), pointer);
			ViewRequest request = context.view.newGraphqlMutation();
			String alias = request.writeAdd(data);
			JsonArray result = request.execToObject()
					.getJsonObject(alias)
					.getJsonArray(type.toFieldName());
			for (int i=0; i<result.size(); i++) {
				JsonObject obj = result.getJsonObject(i);
				setNodeId(obj.getString("id", null));
			}
			if (getNodeId() == null) throw new ViewRequestException("Failed to create root node");
			add(NodeFlag.NEW);
		}

		@Override
		List<NodeInfo> listChildNodes() {
			throw new UnsupportedOperationException();
		}

		@Override
		List<LinkSpace> getLinkScope() {
			return linkScope;
		}

	}

	final ViewUpdate context;

	private GraphBuilder builder;

	private RootInfo rootInfo; 

	private List<LinkSpace> linkScope = Arrays.asList();

	private final Map<String, NodeInfo> nodeMap = new HashMap<>(20000);
	
	private final JoinValueMap joinValues = new JoinValueMap();
	
	GraphUpdate(ViewUpdate viewUpdate) throws ViewRequestException {
		this.context = viewUpdate;
		this.builder = new GraphBuilder(this);
	}

	synchronized boolean hasBuilder() {
		return builder != null;
	}

	/**
	 * Gets the graph node builder
	 * 
	 * @return The node builder, or null if builder was closed.
	 */
	synchronized GraphBuilder getBuilder() {
		if (builder != null)
			return builder;
		else
			throw new IllegalStateException();
	}

	synchronized void builderClosed() {
		if (builder != null) {
			joinValues.clear();
			nodeMap.clear();
			builder = null;
		}
	}

	synchronized void buildComplete() {
		if (builder != null) {
			LinkUpdater linkUpdater = new LinkUpdater(joinValues,
					context.view, LinkScope.GRAPH);
			linkUpdater.updateLinks();
			nodeMap.clear();
			builder = null;
		}
	}

	void setLinkSpaces(String... spaceNames) {
		linkScope = Collections.unmodifiableList(context.selectLinkSpaces(spaceNames));
	}
	
	RootInfo getOrCreateRoot(NodeData data) throws ViewRequestException {
		if (rootInfo == null) {
			rootInfo = new RootInfo(data.getPointer(), data.nodeType);
			data.nodeInfo = rootInfo;
			nodeMap.put("/", rootInfo);
			if (! rootInfo.has(NodeFlag.NEW)) collectNodeInfo();
		}
		return rootInfo;
	}
	
	RootInfo getRootInfo() {
		return rootInfo;
	}

	NodeInfo readNodeInfo(NodeInfo baseNode, String basePath, 
			GraphFieldSet fieldSet, JsonObject fieldSetData) throws ViewRequestException 
	{
		Common.trace("Reading node info, base path = %s, data = %s", basePath, fieldSetData);
		String pointer = fieldSetData.getString(fieldSet.pointerField.fieldName);
		String path = basePath + "/" + pointer;
		NodeInfo info = nodeMap.get(path);
		if (info == null) {
			info = new NodeInfo(baseNode);
			mapNode(path, info);
		}
		for (String key : fieldSetData.keySet()) {
			FieldRole role = fieldSet.getFieldRole(key);
			Common.trace("Reading %s field-set data key %s with role %s", 
					fieldSet.type, key, role);
			if (role != null) switch (role) {
			case ID:
				info.setNodeId(fieldSetData.getString(key));
				Common.trace("Mapped node %s to path %s", info.getNodeId(), path);
				break;
			case TIMESTAMP:
				info.timestamp = Common.parseTime(fieldSetData.getString(key));
				break;
			case PATH:
				GraphFieldSet childFieldSet = fieldSet.getPathFieldSet(key);
				if (childFieldSet == null) {
					throw new ViewRequestException("Cannot find path: " + key);
				}
				else {
					String childPath = path + "/" + key;
					JsonArray array = fieldSetData.getJsonArray(key);
					for (int i = 0; i<array.size(); i++) {
						readNodeInfo(info, childPath, childFieldSet, 
								array.getJsonObject(i));
					}
				}
				break;
			default:
				break;
			}
		}
		return info;
	}
	
	void collectNodeInfo() throws ViewRequestException {
		Common.trace("Start collecting node info");
		ViewRequest request = context.view.newGraphqlQuery();
		request.writeGet(rootInfo.type, rootInfo.getNodeId());
		int depth = 0;
		try (JsonParser parser = request.execToParser()) {
			Entry<NodeField,GraphFieldSet> path = null;
			while (parser.hasNext()) {
				JsonParser.Event event = parser.next();
				switch(event) {
				case KEY_NAME:
					path = rootInfo.type.fieldSet.getPath(parser.getString());
					break;
				case START_OBJECT:
					if (++depth == 2) {
						if (path == null) throw new ViewRequestException("Unknown path");
						readNodeInfo(rootInfo, "/" + path.getKey().fieldName, 
								path.getValue() , parser.getObject());
						--depth;
					}
					break;
				case END_OBJECT:
					if (--depth == 0) return;
				default:
					break;
				}
			}
		}
		finally {
			if (depth == 0) Common.trace("Completed node info collection");
			else Common.trace("Cannot complete node info collection");
		}
	}
	
	void mapNode(String nodePath, NodeInfo nodeInfo) {
		nodeMap.put(nodePath, nodeInfo);
	}
	
	NodeInfo getNodeInfo(String nodePath) {
		return nodeMap.get(nodePath);
	}

}
