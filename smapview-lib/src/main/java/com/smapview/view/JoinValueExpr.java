package com.smapview.view;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JoinValueExpr {

	class FieldExpr {

		NodeField path;

		NodeField field;

		List<Function> functions = new LinkedList<>();

		FieldExpr(String expr) {
			LinkedList<String> tokens = new LinkedList<>();
			for (String token : expr.split("\\.")) {
				tokens.add(token.trim());
			}
			String fieldName = NodeField.NAME.matcher(tokens.peekFirst())
					.matches()? tokens.removeFirst() : null;
			if (fieldName == null) {
				throw new IllegalArgumentException("Invalid field name: " + fieldName);
			}
			else if (NodeField.NAME.matcher(tokens.peekFirst()).matches()) {
				this.path = context.getField(fieldName);
				this.field = context.getField(tokens.removeFirst());
				if (this.path == null) {
					throw new IllegalArgumentException("Unknown path: " + path);
				}
				if (this.path.has(FieldTag.PATH)) ;
				else if (this.path.has(FieldTag.REVERSE) 
						&& this.path.getInverseField().has(FieldTag.PATH)) ;
				else throw new IllegalArgumentException("Invalid path: " + path);
			}
			else {
				this.path = null;
				this.field = context.getField(tokens.removeFirst());
			}
			// path is optional but field is mandatory and must be a string
			if (this.field == null || ! this.field.has(FieldTag.STRING)) {
				throw new IllegalArgumentException("Invalid field: " + field);
			}
			this.functions = tokens.stream().map(s -> createFunction(s)).toList();
		}

		List<String> eval(NodeData node, NodeData parent) {
			if (path != null) {
				if (path.has(FieldTag.REVERSE)) return eval(parent);
				else {
					List<NodeData> childList = node.unsafeGet(path);
					List<String> result = new ArrayList<>();
					childList.stream().forEach(child -> result.addAll(eval(child)));
					return result;
				}
			}
			else return eval(node);

		}

		List<String> eval(NodeData context) {
			List<String> result = new ArrayList<>();
			if (field.isList()) {
				for (String value : (String[])context.unsafeGet(field)) {
					result.add(value);
				}
			}
			else result.add(context.unsafeGet(field));
			for (Function func : functions) {
				if (func instanceof UnaryFunction) {
					for (int i = 0; i < result.size() ; i++) {
						result.set(i, ((UnaryFunction)func).apply(result.get(i)));
					}
				}
				else if (func instanceof ExpandFunction) {
					List<String> newResult = new ArrayList<>();
					for (String base : result) {
						newResult.addAll(((ExpandFunction)func).apply(base));
					}
					result = newResult;
				}
			}
			return result;
		}

	}

	interface Function {}

	interface UnaryFunction extends Function {

		String apply(String input);

	}

	interface ExpandFunction extends Function {

		List<String> apply(String input);

	}

	static class FindAll implements ExpandFunction {

		final Pattern pattern;

		FindAll(String regex) {
			this.pattern = Pattern.compile(regex);
		}

		@Override
		public List<String> apply(String input) {
			Matcher m = pattern.matcher(input);
			List<String> result = new ArrayList<>();
			while (m.find()) {
				if (m.groupCount() == 0) {
					result.add(input.substring(m.start(), m.end() - 1));
				}
				else for (int i = 1; i <= m.groupCount(); i++) {
					result.add(input.substring(m.start(i), m.end(i) - 1));
				}
			}
			return result;
		}

	}

	static class ReplaceAll implements UnaryFunction {

		final Pattern pattern;

		final String replacement;

		ReplaceAll(String regex, String replacement) {
			this.pattern = Pattern.compile(regex);
			this.replacement = replacement;
		}

		@Override
		public String apply(String input) {
			Matcher m = pattern.matcher(input);
			StringBuffer result = new StringBuffer();
			while (m.find()) {
				m.appendReplacement(result, replacement);
			}
			m.appendTail(result);
			return result.toString();
		}

	}

	public static final Pattern FUNCTION = Pattern.compile("([a-z][a-zA-Z]*)\\((.*)\\)");

	public static final Pattern INT_ARG = Pattern.compile("[1-9][0-9]*");

	public static final Pattern STR_ARG = Pattern.compile("'(.*)'");

	final NodeType context;

	private final List<FieldExpr> components;

	JoinValueExpr(String expr, NodeType context) {
		this.context = context;
		LinkedList<String> tokens = new LinkedList<>();
		for (String token : expr.split("+")) {
			tokens.add(token.trim());
		}
		this.components = tokens.stream().map(s -> new FieldExpr(s)).toList();
	}

	static Function createFunction(String functionExpr) {
		Matcher m = FUNCTION.matcher(functionExpr);
		if (! m.matches()) throw new IllegalArgumentException("Invalid function expression");
		else {
			String functionName = m.group(1);
			List<Object> functionArgs = new ArrayList<>(); 
			for (String token : m.group(2).split(",")) {
				if (INT_ARG.matcher(token).matches()) try {
					functionArgs.add(Integer.parseInt(token));
				}
				catch (NumberFormatException e) {
					throw new IllegalArgumentException("Cannot parse integer argument");
				}
				else {
					Matcher strMatcher = STR_ARG.matcher(token);
					if (strMatcher.matches()) {
						functionArgs.add(m.group(1));
					}
					else throw new IllegalArgumentException("Cannot parse function argument");
				}
			}
			if ("findAll".equals(functionName)) 
				return new FindAll((String)functionArgs.get(0));
			else if ("replaceAll".equals(functionName)) 
				return new ReplaceAll((String)functionArgs.get(0), (String)functionArgs.get(1));
			else throw new IllegalArgumentException("Unknown function: " + functionName);
		}
	}

	List<String> eval(NodeData node, NodeData parent) {
		List<String> result = new ArrayList<>();
		result.add("");
		for (FieldExpr expr : components) {
			List<String> newResult = new ArrayList<>();
			for (String base : result) {
				for (String ext : expr.eval(node, parent)) {
					newResult.add(base + ext);
				}
			}
			result = newResult;
		}
		return result;
	}

}
