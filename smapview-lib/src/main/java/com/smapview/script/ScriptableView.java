package com.smapview.script;

import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.annotations.JSFunction;

import com.smapview.view.View;

public class ScriptableView extends ScriptableObject {
	
	private static final long serialVersionUID = -8048858845968794515L;

	View view;
	
	public ScriptableView() {
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public String getClassName() {
		return "View";
	}

	@JSFunction
	public void setUrl(String url) {
		// TODO complete this
	}

	@JSFunction
	public void addSourceFile(String fileName, String rootPointer, Function mapFunction) {
		// TODO complete this
	}

	@JSFunction
	public void update() {
		// TODO complete this
	}
	

}
