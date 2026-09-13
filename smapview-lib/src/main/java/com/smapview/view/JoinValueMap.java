package com.smapview.view;

import java.util.HashMap;
import java.util.Map;

import com.smapview.view.LinkStrategy.JoinRole;

public class JoinValueMap {

	final LinkStrategy strategy;
	
	private final Map<String,JoinValueSubscription> map = new HashMap<>(10000);

	public JoinValueMap(LinkStrategy strategy) {
		this.strategy = strategy;
	}
	
	void collectJoinValues(NodeInfo node, NodeData data) {
		for (JoinRole role : data.nodeType.joinRoles) {
			if (role.getStrategy() == strategy) {
				collectJoinValues(node, data, role);
			}			
		}
	}

	private void collectJoinValues(NodeInfo node, NodeData data, JoinRole role) {
		Object rawVal = data.unsafeGet(role.joinField);
		if (rawVal != null) {
			if (rawVal instanceof String) {
				addJoinValue(node, role, (String)rawVal);
			}
			else if (rawVal instanceof String[]) {
				for (String value : (String[])rawVal) {
					addJoinValue(node, role, value);
				}
			}
			// join field type checked upon link strategy creation 
			else throw new Error();
		}
	}

	private void addJoinValue(NodeInfo node, JoinRole role, String value) {
		// not synchronized, so a synchronized JoinValueMap subclass may
		// be required to accommodate for graph parallel updates
		JoinValueSubscription jval = map.get(value);
		if (jval == null) {
			jval = new JoinValueSubscription(strategy);
			map.put(value, jval);
		}
		jval.add(node,  role.roleType);
	}


}
