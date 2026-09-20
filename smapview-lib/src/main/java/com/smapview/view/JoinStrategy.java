package com.smapview.view;


class JoinStrategy {

	class JoinRole {

		JoinStrategy getStrategy() {
			return JoinStrategy.this;
		}
		
		boolean isTarget() {
			return this == target;
		}
		
	}

	final JoinRole source = new JoinRole();
	
	final JoinRole target = new JoinRole();

	final LinkStrategy linkStrategy;

	final String keyName;

	final JoinValueExpr valueExpr;
	
	final LinkScope linkScope;
		
	JoinStrategy(LinkStrategy linkStrategy, String keyName, String targetType, 
			String valueExpr, LinkScope linkScope) 
	{
		this.linkStrategy = linkStrategy;
		this.keyName = keyName;
		this.valueExpr = new JoinValueExpr(valueExpr, 
				linkStrategy.view.getNodeType(targetType));
		this.linkScope = linkScope;
		linkStrategy.linkField.declaringType.add(source);
		this.valueExpr.context.add(target);
	}
			
}
