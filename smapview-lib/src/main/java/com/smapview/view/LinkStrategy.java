package com.smapview.view;

import java.util.LinkedList;
import java.util.List;

public class LinkStrategy {

	final NodeField linkField;
	
	final short linkId;
	
	final List<JoinStrategy> joinStrategies = new LinkedList<>();
	
	JoinStrategy singleJoin;
	
	LinkStrategy(NodeField linkField, NodeField inverseField, ViewBuilder builder) {
		this.linkField = linkField;
		Common.trace("Creating new link strategy for %s, inverse %s", linkField, inverseField);
		linkField.markAsLink(this, inverseField, builder);
		this.linkId = builder.register(this);
	}
				
}
