package com.smapview.view;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.smapview.view.JoinValueSubscription.JoiningNodeHandler;
import com.smapview.view.LinkStrategy.JoinRole;
import com.smapview.view.ViewUpdate.LinkSpace;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;

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
			nodeId = getRootNodeId();
			if (nodeId == null) createRootNode();
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
				nodeId = obj.getString("id", null);
			}
			if (nodeId == null) throw new ViewRequestException("Failed to create root node");
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

	final Map<String, NodeInfo> nodeMap = new HashMap<>(10000);
	
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

	synchronized void builderClosed(boolean complete) {
		if (builder != null) {
			if (complete) {
				// update graph-internal links 
				for (NodeInfo node : nodeMap.values()) {
					node.handleJoiningNodes(LinkScope.GRAPH, new JoiningNodeHandler() {
						@Override
						public void handle(NodeInfo joiningNode, JoinRole role) {
							// TODO Auto-generated method stub
						}
					});
				}
			}
			builder = null;
		}
	}
	
	void setLinkSpaces(String... spaceNames) {
		linkScope = Collections.unmodifiableList(context.selectLinkSpaces(spaceNames));
	}
	
	RootInfo getOrCreateRoot(NodeData data) throws ViewRequestException {
		if (rootInfo == null) {
			rootInfo = new RootInfo(data.nodePointer, data.nodeType);
			data.nodeInfo = rootInfo;
			nodeMap.put("/", rootInfo);
		}
		return rootInfo;
	}
	
	RootInfo getRootInfo() {
		return rootInfo;
	}

}
