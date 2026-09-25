package com.smapview.view;

import java.util.LinkedList;
import java.util.List;

public class LinkStrategy {

	final View view;
	
	final NodeField linkField;
	
	final List<JoinStrategy> joinStrategies = new LinkedList<>();
	
	JoinStrategy singleJoin;
	
	LinkStrategy(View view, NodeField linkField, NodeField inverseField) {
		this.view = view;
		this.linkField = linkField;
		Common.trace("Creating new link strategy for %s, inverse %s", linkField, inverseField);
		linkField.markAsLink(this, inverseField, view);
	}
	
	/**
	 * Link the nodes based on join values.
	 * <p>
	 * Target nodes provide join values that can be used by source nodes as references to target 
	 * nodes. Target join values get computed from a value expression and source join values are
	 * provided as input data set on the link field.
	 * <p>
	 * @param valueExpr  The expression used to compute join values on target nodes.  
	 * @param linkScope  Tells what scope to use when resolving links (graph or spaces).
	 */
	public void withSingleJoin(String valueExpr, LinkScope linkScope) {
		if (! joinStrategies.isEmpty()) {
			throw new IllegalStateException("Join already defined");
		}
		else {
			this.singleJoin = new JoinStrategy(this, null, 
					linkField.getValueNodeType().typeName,
					valueExpr, linkScope);
			joinStrategies.add(singleJoin);
		}
	}

	/**
	 * Link the nodes based on join key values.
	 * <p>
	 * When the resolved link is defined on an interface, this method can be used to compute
	 * join values on a specific type implementing this interfaces.
	 * For example if type <code>Post</code> implements interface <code>Authored</code> then one 
	 * can use this method to provide values for link <code>Authored.by</code> as references to 
	 * <code>Post.authorEmail</code>.
	 * <p>
	 * @param keyName     The key name, used as a prefix in join values associated with that key.  
	 * @param targetType  The target node type associated with the join key.
	 * @param valueExpr   The expression used to compute join values on target nodes.
	 * @param linkScope   Tells what scope to use when resolving links (graph or spaces).
	 */
	public void withJoinKey(String keyName, String targetType, String valueExpr, LinkScope linkScope) {
		if (singleJoin != null) {
			throw new IllegalStateException("Single join already defined");
		}
		else if (joinStrategies.stream().anyMatch(j -> keyName.equals(j.keyName))) {
			throw new IllegalStateException("Join key already used: " + keyName);
		}
		else joinStrategies.add(new JoinStrategy(this, keyName, targetType, valueExpr, linkScope));
	}
	
	void checkSourceJoinValues(String... values) {
		if (singleJoin == null) loopValues: for (String jval : values) {
			for (JoinStrategy js : joinStrategies) {
				if (jval.length() > js.keyName.length()
						&& jval.startsWith(js.keyName)
						&& jval.charAt(js.keyName.length()) == ':')
				{
					continue loopValues;
				}
			}
			throw new IllegalArgumentException(
					"Join value not matching any join key: " + jval);
		}
	}
	
	LinkScope getScope() {
		for (JoinStrategy js : joinStrategies) {
			if (js.linkScope == LinkScope.SPACES) return LinkScope.SPACES;
		}
		return LinkScope.GRAPH;
	}
	
}
