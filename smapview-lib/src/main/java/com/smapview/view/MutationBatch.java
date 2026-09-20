package com.smapview.view;

import java.util.HashMap;
import java.util.Map;

import jakarta.json.JsonObject;
import jakarta.json.stream.JsonParser;

class MutationBatch {
	
	private class Mutation {
		
		final NodeInfo mutatedNode;
		
		final GraphFieldSet fieldSet;
		
		final String parentPath;

		public Mutation(NodeData data) {
			this.mutatedNode = data.nodeInfo;
			this.fieldSet = data.nodeType.fieldSet;
			this.parentPath = "/" + data.parentPath.fieldName;
		}
		
		void parseResult(JsonParser parser) throws ViewRequestException {
			while (parser.hasNext()) {
				JsonParser.Event event = parser.next();
				switch(event) {
				case START_OBJECT:
					handleResult(parser.getObject());
					break;
				case END_OBJECT:
					return;
				default:
					break;
				}
			}
		}
		
		void handleResult(JsonObject result) throws ViewRequestException {
			NodeInfo info =  builder.update.readNodeInfo(
					builder.update.getRootInfo(), 
					parentPath, fieldSet, result);
			if (info != mutatedNode) throw new IllegalStateException();
		}
		
	}
	
	final GraphBuilder builder;

	final Map<String,Mutation> nodeMap;

	final ViewRequest request;
	
	MutationBatch(GraphBuilder builder, int maxSize) {
		if (maxSize > ViewRequest.MAX_ITEM_COUNT) {
			throw new IllegalArgumentException();
		}
		this.builder = builder;
		this.nodeMap = new HashMap<>(maxSize * 2);
		this.request = builder.update.context.view.newGraphqlMutation();
	}
	
	void add(NodeData data) {
		NodeInfo node = data.nodeInfo;
		String alias = node.getNodeId() == null? request.writeAdd(data)
				: request.writeUpdate(data);
		nodeMap.put(alias, new Mutation(data));
	}
	
	void execute() throws ViewRequestException {
		try (JsonParser parser = request.execToParser()) {
			Mutation mutation = null;
			while (parser.hasNext()) {
				JsonParser.Event event = parser.next();
				switch(event) {
				case KEY_NAME:
					String keyName = parser.getString();
					mutation = nodeMap.get(keyName);
					if (mutation == null) {
						throw new ViewRequestException("Invalid alias: " + keyName);
					}
					else Common.trace("Got result for mutation alias %s", keyName);
					break;
				case START_OBJECT:
					if (mutation == null) throw new IllegalStateException();
					else mutation.parseResult(parser);
					break;
				case END_OBJECT:
					return;
				default:
					break;
				}
			}
		}
		catch (IllegalStateException e) {
			throw new ViewRequestException("Cannot read response");
		}
	}
	
	int size() {
		return nodeMap.size();
	}
		
}
