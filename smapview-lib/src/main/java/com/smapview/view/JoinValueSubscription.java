package com.smapview.view;

import java.util.LinkedList;
import java.util.List;

import com.smapview.view.LinkStrategy.JoinRole;
import com.smapview.view.LinkStrategy.RoleType;
import com.smapview.view.ViewUpdate.LinkSpace;

class JoinValueSubscription {

	@SuppressWarnings("serial")
	class SubscriptionRole extends LinkedList<Subscriber> {
		
		final JoinRole joinRole;
		
		SubscriptionRole(JoinRole joinRole) {
			this.joinRole = joinRole;
		}
		
		@Override
		public boolean add(Subscriber subs) {
			if (super.add(subs)) {
				if (subs.roles == null) subs.roles = new LinkedList<>();
				subs.roles.add(this);
				return true;
			}
			else throw new Error();
		}
		
		SubscriptionRole getJoiningRole() {
			return joinRole.roleType == RoleType.SOURCE? target : source;
		}
		
	}

	static abstract class Subscriber {
				
		private List<SubscriptionRole> roles;
		
		abstract NodeInfo getNodeInfo();
		
		abstract List<LinkSpace> getLinkScope();

		void handleJoiningNodes(LinkScope scope, JoiningNodeHandler handler) {
			for (SubscriptionRole role : roles) {
				if (role.joinRole.getStrategy().linkScope == scope) {
					SubscriptionRole joiningRole = role.getJoiningRole();
					for (Subscriber joiningSubs : joiningRole) {
						if (canJoin(joiningSubs, joiningRole)) {
							handler.handle(joiningSubs.getNodeInfo(), joiningRole.joinRole);
						}
					}
				}
			}
		}
		
		boolean canJoin(Subscriber subs, SubscriptionRole subsRole) {
			if (subsRole.joinRole.getStrategy().linkScope == LinkScope.GRAPH) {
				return true;
			}
			else {
				Subscriber source, target;
				if (subsRole.joinRole.roleType == RoleType.SOURCE) {
					source = subs;
					target = this;
				}
				else {
					source = this;
					target = subs;
				}
				if (source.getLinkScope().size() > 0 
						&& target.getLinkScope().size() > 0) 
				{
					LinkSpace primarySpace = source.getLinkScope().getFirst();
					for (LinkSpace targetSpace : target.getLinkScope()) {
						if (targetSpace == primarySpace) return true;
					}
				}
				return false;
			}
		}
		
	}
	
	interface JoiningNodeHandler {
		
		void handle(NodeInfo joiningNode, JoinRole role);
		
	}

	final SubscriptionRole source;

	final SubscriptionRole target;
	
	JoinValueSubscription(LinkStrategy strategy) {
		this.source = new SubscriptionRole(strategy.getJoinRole(RoleType.SOURCE));
		this.target = new SubscriptionRole(strategy.getJoinRole(RoleType.TARGET));
	}
	
	void add(NodeInfo node, RoleType role) {
		switch (role) {
		case SOURCE:
			source.add(node);
			break;
		case TARGET:
			target.add(node);
			break;
		}
	}
	
}
