package com.smapview.view;

import java.util.HashMap;
import java.util.List;

class LinkUpdater {
	
	final JoinValueMap source;
	
	final View view;
	
	final List<LinkStrategy> scope;

	final HashMap<Link,LinkStatus> linkStatus = new HashMap<>(100000);

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
			return fromNodeId.equals(link.fromNodeId)
					&& toNodeId.equals(link.toNodeId);
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

	LinkUpdater(JoinValueMap source, View view, LinkScope scope) {
		this.source = source;
		this.view = view;
		this.scope = view.linkStrategies.stream()
				.filter(s -> s.getScope() == scope).toList();
	}
	
	void updateLinks() {
		// TODO complete this
		source.clear();
	}
	
}
