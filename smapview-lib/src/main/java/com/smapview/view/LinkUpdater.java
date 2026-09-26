package com.smapview.view;

import java.util.Map;
import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.smapview.view.JoinValueMap.ValueBinder;

class LinkUpdater {
	
	interface LinkContext {
		
		View getView();
		
		boolean isGlobalContext();
		
		void queryExistingLinks(Consumer<Link> action);
		
	}

	static class Link {
		
		final short linkId;

		final String fromNodeId;
		
		final String toNodeId;

		Link(short linkId, String fromNodeId, String toNodeId) {
			this.linkId = linkId;
			this.fromNodeId = fromNodeId;
			this.toNodeId = toNodeId;
		}

		Link(JoinStrategy js, LinkEndpoint source, LinkEndpoint target) {
			this(js.getLinkId(), source.nodeId, target.nodeId);
		}

		@Override
		public int hashCode() {
			int result = 17; 
		    result = 31 * result + linkId;
		    result = 31 * result + fromNodeId.hashCode();
		    result = 31 * result + fromNodeId.hashCode();
		    return result;
		}
		
		@Override
		public boolean equals(Object object) {
			Link link = (Link)object;
			return linkId == link.linkId
					&& fromNodeId.equals(link.fromNodeId)
					&& toNodeId.equals(link.toNodeId);
		}
				
	}

	final static int BATCH_SIZE = 200;
	
	final static byte ADD = 1; 

	final static byte KEEP = 2; 

	final static byte REMOVE = 3;

	final JoinValueMap source;
	
	final LinkContext context;

	final private HashMap<Link,Byte> linkStatus = new HashMap<>(100000);

	private ViewRequest setRequest, deleteRequest;

	LinkUpdater(JoinValueMap source, LinkContext context) {
		this.source = source;
		this.context = context;
	}

	void updateLinks() throws ViewRequestException {
		Common.trace("Start updating links");
		// build all links based on source join values
		source.forEach(new BiConsumer<JoinValue,ValueBinder> () {
			@Override
			public void accept(JoinValue jval, ValueBinder binder) {
				JoinStrategy js = context.getView().getJoinStrategy(jval.joinId);
				for (LinkEndpoint source : binder.source) {
					for (LinkEndpoint target : binder.target) {
						linkStatus.put(new Link(js, source, target), ADD);
						linkStatus.put(new Link(js, source, target), ADD);
					}
				}
			}
		});
		Common.trace("Collected %d links from %d join values",
				linkStatus.size(), source.size());
		// search in view for existing links and set their status accordingly
		context.queryExistingLinks(new Consumer<Link>() {
			@Override
			public void accept(Link link) {
				linkStatus.put(link, linkStatus.containsKey(link)? KEEP : REMOVE);
			}
		});
		// write set and delete requests to update view links
		int added = 0, kept = 0, removed = 0;
		for (Map.Entry<Link,Byte> entry : linkStatus.entrySet()) {
			Link link = entry.getKey();
			byte status = entry.getValue();
			switch (status) {
			case ADD:
				writeSet(link);
				added++;
				break;
			case KEEP:
				kept++;
				break;
			case REMOVE:
				writeDelete(link);
				removed++;
				break;
			}
		}
		if (setRequest != null) setRequest.exec();
		if (deleteRequest != null) deleteRequest.exec();
		Common.trace("Links updated: added=%d kept=%d removed=%d",
				added, kept, removed);
		source.clear();
	}
	
	void writeSet(Link link) throws ViewRequestException {
		if (setRequest != null && setRequest.getItemCount() > BATCH_SIZE) {
			setRequest.exec();
			setRequest = null;
		}
		if (setRequest == null) setRequest = context.getView().newDqlSet();
		setRequest.writeSet(link, context.isGlobalContext());
	}
	
	void writeDelete(Link link) throws ViewRequestException {
		if (deleteRequest != null && deleteRequest.getItemCount() > BATCH_SIZE) {
			deleteRequest.exec();
			deleteRequest = null;
		}
		if (deleteRequest == null) deleteRequest = context.getView().newDqlDelete();
		// TODO add to delete request
	}
	
}
