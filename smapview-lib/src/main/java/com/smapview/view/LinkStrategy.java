package com.smapview.view;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

class LinkStrategy {

	enum RoleType {
		SOURCE,
		TARGET
	}

	class JoinRole {

		final RoleType roleType;
		
		final NodeField joinField;
		
		final NodeField linkField;
		
		JoinRole(RoleType roleType, NodeField joinField, 
				NodeField linkField, Map<String,NodeType> typeMap) 
		{
			this.roleType = roleType;
			this.joinField = joinField;
			this.linkField = linkField;
		}
		
		LinkStrategy getStrategy() {
			return LinkStrategy.this;
		}
		
		JoinRole getJoiningRole() {
			return roleType == RoleType.SOURCE? target : source;
		}

	}
		
	final LinkScope linkScope;
	
	final JoinRole source;

	final JoinRole target;

	final List<JoinRole> joinRoles;

	LinkStrategy(NodeField linkField, NodeField inverseLinkField, 
			NodeField sourceJoinField, NodeField targetJoinField, 
			LinkScope linkScope, Map<String,NodeType> typeMap) 
	{
		this.linkScope = linkScope;
		this.source = new JoinRole(RoleType.SOURCE, sourceJoinField, linkField, typeMap);
		this.target = new JoinRole(RoleType.TARGET, targetJoinField, inverseLinkField, typeMap); 
		this.joinRoles = Arrays.asList(source, target);
	}
	
	JoinRole getJoinRole(RoleType roleType) {
		return roleType == RoleType.SOURCE? source : target;
	}
		
}
