package com.smapview.view;

import java.util.HashMap;
import java.util.Map;

import jakarta.json.stream.JsonParser;

class MutationBatch {

	final GraphBuilder builder;

	final Map<String,NodeInfo> nodeMap;

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
		String alias = node.nodeId == null? request.writeAdd(data)
				: request.writeUpdate(data);
		nodeMap.put(alias, node);
	}
	
	void execute() throws ViewRequestException {
		try (JsonParser parser = request.execToParser()) {
			boolean dataReached = false;
			boolean hasErrors = false;
			while (parser.hasNext()) {
				JsonParser.Event event = parser.next();
				switch(event) {
				case KEY_NAME:
					dataReached = "data".equals(parser.getString());
					hasErrors = "errors".equals(parser.getString());
					break;
				case START_OBJECT:
					if (dataReached) {
						// TODO complete this
					}
					break;
				default:
					break;
				}
			}
			if (hasErrors) throw new ViewRequestException("Request returned error(s)");
		}
	}
	
	int size() {
		return nodeMap.size();
	}

}
