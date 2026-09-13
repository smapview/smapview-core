package com.smapview.view;

import com.smapview.view.GraphFieldSet.FieldRole;
import com.smapview.view.GraphUpdate.RootInfo;

import jakarta.json.stream.JsonParser;

class GraphExplorer {

	final GraphUpdate update;
	
	GraphExplorer(GraphUpdate update) {
		this.update = update;
	}
		
	void collectNodeInfo() throws ViewRequestException, GraphExplorerException {
		RootInfo rootInfo = update.getRootInfo(); 
		ViewRequest request = update.context.view.newGraphqlQuery();
		request.writeGet(rootInfo.type, rootInfo.nodeId);
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
					if (dataReached) parseNodeInfo(parser, rootInfo,
							rootInfo.type.fieldSet, "/");
					break;
				default:
					break;
				}
			}
			if (hasErrors) throw new GraphExplorerException();
		}
	}
	
	void parseNodeInfo(JsonParser parser, NodeInfo current, 
			GraphFieldSet fieldSet, String parentPath) throws GraphExplorerException 
	{
		String fieldName = null;
		FieldRole fieldRole = null;
		String nodePath = null;
		while (parser.hasNext()) {
			JsonParser.Event event = parser.next();
			switch(event) {
			case KEY_NAME:
				fieldName = parser.getString();
				fieldRole = fieldSet.getFieldRole(fieldName);
				break;
			case VALUE_STRING:
				if (fieldRole == null) throw new GraphExplorerException();
				else if (fieldRole == FieldRole.ID) {
					current.nodeId = parser.getString();
				} 
				else if (fieldRole == FieldRole.POINTER) {
					nodePath = parentPath + "/" + parser.getString();
					update.nodeMap.put(nodePath, current);
				} 
				else if (fieldRole == FieldRole.TIMESTAMP) {
					current.updateTime = Common.parseTime(parser.getString());
				} 
				break;
			case START_ARRAY:
			case END_ARRAY:
				// ignored but we handle contained objects
				break;
			case START_OBJECT:
				if (fieldRole != FieldRole.PATH || nodePath == null) {
					throw new GraphExplorerException();
				}
				parseNodeInfo(parser, new NodeInfo(current), 
						fieldSet.getPathFieldSet(fieldName), 
						nodePath);
				break;
			case END_OBJECT:
				return;
			default:
				break;
			}
		}
	}

}
