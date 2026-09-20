package com.smapview.view;

import java.util.LinkedList;
import java.util.List;

public class LinkStrategy {

	final View view;
	
	final NodeField linkField;
	
	final List<JoinStrategy> joinStrategies = new LinkedList<>();
	
	LinkStrategy(View view, NodeField linkField, NodeField inverseField) {
		this.view = view;
		this.linkField = linkField;
		Common.trace("Creating new link strategy for %s, inverse %s", linkField, inverseField);
		linkField.markAsLink(inverseField, view);
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
		// TODO complete this
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
		joinStrategies.add(new JoinStrategy(this, keyName, targetType, valueExpr, linkScope));
	}
	
}
