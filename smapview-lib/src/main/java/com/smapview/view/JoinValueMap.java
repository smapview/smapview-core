package com.smapview.view;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.smapview.view.JoinStrategy.JoinRole;

public class JoinValueMap {

		
	static class JoinValue {
		
		final String value;
		
		final short joinId;
		
		public JoinValue(String value, short joinId) {
			this.value = value;
			this.joinId = joinId;
		}

		@Override
		public int hashCode() {
			int result = 17; 
		    result = 31 * result + value.hashCode();
		    result = 31 * result + joinId;
		    return result;
		}
		
		@Override
		public boolean equals(Object object) {
			JoinValue jval = (JoinValue)object;
			return value.equals(jval.value)
					&& joinId == jval.joinId;
		}
	
	}
			
	static class JoinValueBinder {
		
		final List<LinkEndpoint> source = new LinkedList<>();

		final List<LinkEndpoint> target = new LinkedList<>();
		
	}

	private final Map<JoinValue,JoinValueBinder> map = new HashMap<>(100000);
	
	void addJoinValue(LinkEndpoint endpoint, JoinRole role, String value) {
		// not synchronized, so a synchronized JoinValueMap subclass may
		// be required to accommodate for graph parallel updates
		JoinValue jval = new JoinValue(value, role.getStrategy().joinId);
		JoinValueBinder binder = map.get(jval);
		if (binder == null) {
			binder = new JoinValueBinder();
			map.put(jval, binder);
		}
		if (role.isTarget()) binder.target.add(endpoint);
		else binder.source.add(endpoint);
	}
		
	void clear() {
		map.clear();
	}


}
