package com.smapview.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Entry point for view updates.
 */
public class ViewUpdate {
	
	class LinkSpace {
		
		final String name;

		public LinkSpace(String name) {
			this.name = name;
		}
		
	}
	
	final View view;
	
	final private Map<JoinStrategy,JoinValueMap> joinValues = new HashMap<>(32);
	
	final List<LinkSpace> linkSpaces = new ArrayList<>();
	
	final List<GraphUpdate> graphUpdates = new ArrayList<>();
	
	final Map<LinkStrategy,LinkUpdater> linkUpdaters; 
		
	boolean completed = false;
		
	ViewUpdate(View view) {
		this.view = view;
		this.linkUpdaters = new HashMap<>(view.linkStrategies.size());
		// prepare for link updates on each link strategy
		// and build join value maps to resolve inter-graph links
		for (LinkStrategy strategy : view.linkStrategies) {
			linkUpdaters.put(strategy, new LinkUpdater(strategy)); 
			for (JoinStrategy js : strategy.joinStrategies) {
				if (js.linkScope == LinkScope.SPACES) {
					joinValues.put(js, new JoinValueMap(js));
				}
			}			
		}
	}

	/**
	 * Completes this update.
	 * 
	 * @throws ViewUpdateException
	 */
	synchronized public void complete() throws ViewUpdateException {
		// TODO implement this
		completed = true;
	}
	
	public boolean isCompleted() {
		return completed;
	}
		
	/**
	 * Starts updating a graph.
	 * 
	 * Concurrent graph updates are not allowed.
	 * 
	 * @throws ViewRequestException 
	 */
	synchronized public GraphBuilder startGraphUpdate() throws ViewRequestException{
		// TODO support parallel graph updates, maybe as an option during ViewUpdate creation
		for (GraphUpdate update : graphUpdates) {
			if (update.hasBuilder()) throw new IllegalStateException("Graph already being updated");
		}
		GraphUpdate result = new GraphUpdate(this); 
		graphUpdates.add(result);
		return result.getBuilder();
	}

	List<LinkSpace> selectLinkSpaces(String[] spaceNames) {
		List<LinkSpace> selectedSpaces = new ArrayList<>(); 
		for (String spaceName : spaceNames) {
			LinkSpace space = this.linkSpaces.stream()
					.filter(s -> s.name.equals(spaceName))
					.findFirst().orElse(null);
			if (space == null) this.linkSpaces.add(space = new LinkSpace(spaceName));
			selectedSpaces.add(space);
		}
		return selectedSpaces;
	}

}
