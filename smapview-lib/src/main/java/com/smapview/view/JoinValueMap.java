package com.smapview.view;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.smapview.view.JoinStrategy.JoinRole;

public class JoinValueMap {

	
	class JoinValue {
		
		final List<LinkEndpoint> source = new LinkedList<>();

		final List<LinkEndpoint> target = new LinkedList<>();
		
	}
	
	final JoinStrategy strategy;
	
	private final Map<String,JoinValue> map = new HashMap<>(100000);

	public JoinValueMap(JoinStrategy strategy) {
		this.strategy = strategy;
	}
	
	void addJoinValue(LinkEndpoint endpoint, JoinRole role, String value) {
		// not synchronized, so a synchronized JoinValueMap subclass may
		// be required to accommodate for graph parallel updates
		JoinValue jval = null;
		if (jval == null) {
			jval = new JoinValue();
			map.put(value, jval);
		}
		if (role.isTarget()) jval.target.add(endpoint);
		else jval.source.add(endpoint);
	}
		
	void clear() {
		map.clear();
	}


}
