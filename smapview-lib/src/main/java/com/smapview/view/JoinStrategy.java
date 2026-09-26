package com.smapview.view;


class JoinStrategy {
	
	final LinkStrategy linkStrategy;

	final String keyName;

	final short joinId;

	final JoinValueExpr valueExpr;
	
	final JoinScope joinScope;
			
	JoinStrategy(LinkStrategy linkStrategy, String keyName, String targetType, 
			String valueExpr, JoinScope linkScope) 
	{
		NodeType toType = linkStrategy.view.getNodeType(targetType);
		this.linkStrategy = linkStrategy;
		this.keyName = keyName;
		this.joinId = linkStrategy.view.register(this);
		this.valueExpr = new JoinValueExpr(valueExpr, toType, joinId);
		this.joinScope = linkScope;
	}
	
	JoinStrategy getJoinStrategy() {
		return linkStrategy.view.getJoinStrategy(joinId);
	}
	
	short getLinkId() {
		return linkStrategy.linkId;
	}
			
}
