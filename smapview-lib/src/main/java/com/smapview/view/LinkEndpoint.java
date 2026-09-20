package com.smapview.view;

import java.util.List;

import com.smapview.view.ViewUpdate.LinkSpace;

class LinkEndpoint {
	
	String nodeId;
	
	final List<LinkSpace> linkScope;

	public LinkEndpoint(List<LinkSpace> linkScope) {
		this.linkScope = linkScope;
	}
	
	boolean canJoin(LinkEndpoint endpoint) {
		LinkEndpoint source = this, target = endpoint;
		if (source.linkScope.size() > 0 
				&& target.linkScope.size() > 0) 
		{
			LinkSpace primarySpace = source.linkScope.getFirst();
			for (LinkSpace targetSpace : target.linkScope) {
				if (targetSpace == primarySpace) return true;
			}
		}
		return false;
	}

}
