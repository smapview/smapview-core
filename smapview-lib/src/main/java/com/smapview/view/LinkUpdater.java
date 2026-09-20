package com.smapview.view;

import java.util.HashMap;
import java.util.Map;

class LinkUpdater {

	final Map<JoinStrategy,JoinValueMap> consumedMaps = new HashMap<>(8);

	final HashMap<Link,LinkStatus> linkStatus = new HashMap<>(100000);

	final LinkStrategy linkStrategy;	
	
	class Link {
		
		String fromNodeId;
		
		String toNodeId;
		
		@Override
		public int hashCode() {
			int result = 17; 
		    result = 31 * result + fromNodeId.hashCode();
		    result = 31 * result + toNodeId.hashCode();
		    return result;
		}
		
		@Override
		public boolean equals(Object object) {
			Link link = (Link)object;
			return link.fromNodeId.equals(fromNodeId)
					&& toNodeId.equals(toNodeId);
		}
				
	}

	enum StatusFlag {
		
		NEW((byte)0x1),
		
		OLD((byte)0x2);
		
		byte value;
		
		StatusFlag(byte value) {
			this.value = value;
		}
		
	}

	class LinkStatus extends Link {
				
		byte flags = 0;
		
	}

	LinkUpdater(LinkStrategy linkStrategy) {
		this.linkStrategy = linkStrategy;
		for (JoinStrategy js : linkStrategy.joinStrategies) {
			consumedMaps.put(js, null);
		}
	}
	
	void consume(JoinValueMap map) {
		// TODO complete this
		map.clear();
		consumedMaps.put(map.strategy, map);
		for (JoinStrategy js : linkStrategy.joinStrategies) {
			if (! consumedMaps.containsKey(js)) return; 
		}
		updateLinks();
	}
	
	private void updateLinks() {
		// TODO complete this
	}

}
