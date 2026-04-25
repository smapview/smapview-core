package com.smapview.core.map;

import com.smapview.core.map.NodeType.TypeField;
import com.smapview.core.map.NodeType.ValueField;

/**
 * A node with properties.
 */
public class Node {
		
	final NodeQuery query;
	
	final NodeType type;
	
	final int nodeId;
	
	final String path;
	
	final Object[] data;

	Node(NodeQuery query, NodeType type, int nodeId, String path, Object[] data) {
		this.query = query;
		this.type = type;
		this.nodeId = nodeId;
		this.path = path;
		this.data = data;
	}
	
	Object get(String fieldName) {
		TypeField field = type.getField(fieldName);
		if (field != null && field instanceof ValueField vf) {
			if (vf.dataIndex < data.length) 
			{
				return data[vf.dataIndex];
			}
			else return null;
		}
		else throw new IllegalArgumentException();
	}
	
	static String escapeName(String nodeName) {
		// TODO complete this
		return nodeName;
	}

	static String unescapeName(String escapedName) {
		// TODO complete this
		return escapedName;
	}
	
	static String buildPath(String basePath, String folderAlias, String nodeName) {
		return basePath + "/" + folderAlias + ":" + escapeName(nodeName);
	}

}
