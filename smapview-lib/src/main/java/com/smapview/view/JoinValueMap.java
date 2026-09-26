package com.smapview.view;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;


class JoinValueMap {
			
	static class ValueBinder {
		
		final List<LinkEndpoint> source = new LinkedList<>();

		final List<LinkEndpoint> target = new LinkedList<>();
		
	}

	private final Map<JoinValue,ValueBinder> map = new HashMap<>(100000);
	
	private void bind(LinkEndpoint endpoint, JoinValue value, boolean asTarget) {
		// not synchronized, so a synchronized JoinValueMap subclass may
		// be required to accommodate for graph parallel updates
		ValueBinder binder = map.get(value);
		if (binder == null) {
			binder = new ValueBinder();
			map.put(value, binder);
		}
		if (asTarget) binder.target.add(endpoint);
		else binder.source.add(endpoint);
		Common.trace("Bound %s endpoint to join value %s", 
				asTarget? "target" : "source", value);
	}
	
	void bindSource(LinkEndpoint endpoint, JoinValue value) {
		bind(endpoint, value, false);
	}

	void bindTarget(LinkEndpoint endpoint, JoinValue value) {
		bind(endpoint, value, true);
	}

	void clear() {
		map.clear();
	}
	
	void forEach(BiConsumer<JoinValue,ValueBinder> action) {
		map.forEach(action);
	}
	
	int size() {
		return map.size();
	}

}
