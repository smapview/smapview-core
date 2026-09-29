package com.smapview.script;

import java.lang.reflect.InvocationTargetException;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class Script {

	final Context context = Context.enter();
	
	final Scriptable rootScope = context.initStandardObjects();

	final Scriptable evalScope = context.newObject(rootScope);

	public Script() throws ScriptException {
		try {
			ScriptableObject.defineClass(rootScope, ScriptableView.class);
			evalScope.put("view", evalScope, context.newObject(evalScope, "View"));
		} catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
			throw new ScriptException(e);
		}
	}

}
