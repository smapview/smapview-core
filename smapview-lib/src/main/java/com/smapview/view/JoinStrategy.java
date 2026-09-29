package com.smapview.view;


class JoinStrategy {
	
	final LinkStrategy linkStrategy;

	final String keyName;

	final short joinId;

	final JoinValueExpr valueExpr;
	
	final JoinScope joinScope;
			
	JoinStrategy(LinkStrategy linkStrategy, String valueExpr, 
			JoinScope joinScope, ViewBuilder builder) 
	{
		this(linkStrategy, null, 
				linkStrategy.linkField.fieldType.getBaseType().typeName,
				valueExpr, joinScope, builder); 
	}
	
	JoinStrategy(LinkStrategy linkStrategy, String keyName, String targetType, 
			String valueExpr, JoinScope joinScope, ViewBuilder builder) 
	{
		NodeType toType = builder.getNodeType(targetType);
		this.linkStrategy = linkStrategy;
		this.keyName = keyName;
		this.joinId = builder.register(this);
		this.valueExpr = new JoinValueExpr(valueExpr, toType, joinId);
		this.joinScope = joinScope;
	}
		
	short getLinkId() {
		return linkStrategy.linkId;
	}
			
}
