package com.smapview.view;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.labs.parse.OperatorTable;
import com.google.common.labs.parse.Parser;


public class JoinValueExpr {
	
	enum Operator {
		
		APPEND("+"),
		
		UNION("|"),
		
		IF_THEN("?");
		
		final String symbol;
		
		Operator(String symbol) {
			this.symbol = symbol;
		}
		
		@Override
		public String toString() {
			return symbol;
		}
		
	}
	
	interface ValidExpr {
		
		List<String> eval(NodeData node, NodeData parent);
		
		boolean test(NodeData node, NodeData parent);
		
		boolean isTestable();

	}
	
	class CompoundExpr implements ValidExpr {
		
		final ValidExpr e1, e2;
		
		final Operator op;
		
		CompoundExpr(ValidExpr e1, ValidExpr e2, Operator op) {
			this.e1 = e1;
			this.e2 = e2;
			this.op = op;
			switch (op) {
			case IF_THEN:
				if (! e1.isTestable())
					throw new IllegalArgumentException("Invalid test expression");
				break;
			case APPEND:
			case UNION:
				if (e1.isTestable() || e2.isTestable())
					throw new IllegalArgumentException("Invalid expression for " + op);
				break;
			}
		}

		@Override
		public List<String> eval(NodeData node, NodeData parent) {
			switch (op) {
			case IF_THEN:
				return e1.test(node, parent)? e2.eval(node, parent)
						: Arrays.asList();
			case APPEND: {
				List<String> r1 = e1.eval(node, parent);
				List<String> r2 = e2.eval(node, parent);
				List<String> result = new ArrayList<>(r1.size() * r2.size());
				for (String base : r1) for (String ext : r2) result.add(base + ext);
				return result;
			}
			case UNION: {
				List<String> result = e1.eval(node, parent);
				result.addAll(e2.eval(node, parent));
				return result;
			}
			default:
				throw new Error();
			}
		}

		@Override
		public boolean test(NodeData node, NodeData parent) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isTestable() {
			return false;
		}
		
	}
	
	class FieldExpr implements ValidExpr {

		NodeField path;

		NodeField value;

		List<Function> functions = new LinkedList<>();

		FieldExpr(String field1, String field2) {
			if (field2 == null) {
				this.path = null;
				this.value = context.getField(field1);				
			}
			else {
				this.path = context.getField(field1);
				if (this.path == null)
					throw new IllegalArgumentException("Unknown path field: " + path);
				else if (this.path.has(FieldTag.PATH)) ;
				else if (this.path.has(FieldTag.REVERSE) 
						&& this.path.getInverseField().has(FieldTag.PATH)) ;
				else throw new IllegalArgumentException("Invalid path field: " + path);
				this.value = path.getValueNodeType().getField(field2);
			}
			// value field is mandatory and must be of String type
			if (this.value == null || ! this.value.has(FieldTag.STRING)) {
				throw new IllegalArgumentException("Invalid value field: " + value);
			}
		}
		
		FieldExpr addFunction(Function func) {
			if (! functions.isEmpty()) {
				Function last = functions.getLast();
				if (last instanceof TestFunction) {
					throw new IllegalStateException("Cannot add function to test function");
				}
			}
			if (func instanceof TestFunction) {
				if (path != null && ! path.has(FieldTag.REVERSE))
					throw new IllegalStateException("Cannot add test function to child reference");
				else if (hasExpandFunction()) 
					throw new IllegalStateException("Cannot add test function to expand function");
			}
			functions.add(func);
			return this;
		}
		
		boolean hasExpandFunction() {
			for (Function func : functions) {
				if (func instanceof ExpandFunction) return true;
			}
			return false;
		}

		public List<String> eval(NodeData node, NodeData parent) {
			if (path != null) {
				if (path.has(FieldTag.REVERSE)) return eval(parent);
				else {
					List<NodeData> childList = node.unsafeGet(path);
					if (childList == null) return Arrays.asList();
					else {
						List<String> result = new ArrayList<>();
						childList.stream().forEach(child -> result.addAll(eval(child)));
						return result;
					}
				}
			}
			else return eval(node);
		}

		@SuppressWarnings("unchecked")
		private <T> T eval(NodeData context) {
			List<String> result = new ArrayList<>();
			if (value.isList()) {
				for (String value : (String[])context.unsafeGet(value)) {
					result.add(value);
				}
			}
			else result.add(context.unsafeGet(value));
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
				else if (func instanceof TestFunction) {
					for (String base : result) {
						return (T) (Boolean)((TestFunction)func).apply(base);
					}
					return (T) (Boolean)false;
				}
			}
			return (T)result;
		}
		
		@Override
		public boolean test(NodeData node, NodeData parent) {
			if (path != null) {
				if (path.has(FieldTag.REVERSE)) return eval(parent);
				else throw new IllegalStateException();
			}
			else return eval(node);
		}

		@Override
		public boolean isTestable() {
			return functions.size() > 0 
					&& functions.getLast() instanceof TestFunction;
		}

	}
	
	class Literal implements ValidExpr {

		final String value;
		
		Literal(String value) {
			this.value = value;
		}
		
		@Override
		public List<String> eval(NodeData node, NodeData parent) {
			// TODO Auto-generated method stub
			return Arrays.asList(value);
		}

		@Override
		public boolean test(NodeData node, NodeData parent) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isTestable() {
			return false;
		}
		
		
	}

	interface Function {}

	interface UnaryFunction extends Function {

		String apply(String input);

	}

	interface ExpandFunction extends Function {

		List<String> apply(String input);

	}

	interface TestFunction extends Function {

		boolean apply(String input);

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
					result.add(input.substring(m.start(), m.end()));
				}
				else for (int i = 1; i <= m.groupCount(); i++) {
					result.add(input.substring(m.start(i), m.end(i)));
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

	static class Equals implements TestFunction {

		final String value;

		Equals(String value) {
			this.value = value;
		}

		@Override
		public boolean apply(String input) {
			return value.equals(input);
		}

	}

	final NodeType context;

	final ValidExpr validExpr;
	
	JoinValueExpr(String expr, NodeType context) {
		this.context = context;
		this.validExpr = createParser().parseSkipping(Character::isWhitespace, expr);
	}

	static Function createFunction(String functionName, List<Object> functionArgs) {
		if ("findAll".equals(functionName)) 
			return new FindAll((String)functionArgs.get(0));
		else if ("replaceAll".equals(functionName)) 
			return new ReplaceAll((String)functionArgs.get(0), (String)functionArgs.get(1));
		else if ("equals".equals(functionName)) 
			return new Equals((String)functionArgs.get(0));
		else 
			throw new IllegalArgumentException("Unknown function: " + functionName);
	}

	List<String> eval(NodeData node, NodeData parent) {
		return validExpr.eval(node, parent);
	}
	
	Parser<ValidExpr> createParser() {
		Parser<String> dot = Parser.string(".");
		Parser<String> fieldName = Parser.word()
				.suchThat(w -> NodeField.NAME.matcher(w).matches(), "field name");
		Parser<String> functionName = Parser.word()
				.suchThat(w -> w.matches("[a-z][a-zA-Z]+"), "function name");
		Parser<Object> strArg = Parser.quotedByWithEscapes('\'', '\'', Parser.chars(1)).as("string")
				.map(s -> (Object)s);
		Parser<Object> intArg = Parser.digits().suchThat(s -> ! s.startsWith("0"), "integer")
				.map(s -> Integer.parseInt(s));
		Parser<Function> function = Parser.sequence(functionName, 
				strArg.or(intArg).zeroOrMoreDelimitedBy(",").between("(", ")").as("arguments"),
				JoinValueExpr::createFunction).as("function");
		Parser<FieldExpr> fieldExpr = Parser
				.sequence(fieldName, dot.then(fieldName).notFollowedBy("(").orElse(null), 
						(f1, f2) -> new FieldExpr(f1, f2))
				.followedByZeroOrMore(dot.then(function), FieldExpr::addFunction);
		Parser<Literal> literal = Parser.quotedByWithEscapes('\'', '\'', Parser.chars(1)).as("literal")
				.map(s -> new Literal(s));
		return Parser.define(
			      sub -> new OperatorTable<ValidExpr>()
		          .leftAssociative(Parser.string("+").thenReturn(
		        		  (e1, e2) -> new CompoundExpr(e1, e2, Operator.APPEND)), 30)
		          .leftAssociative(Parser.string("|").thenReturn(
		        		  (e1, e2) -> new CompoundExpr(e1, e2, Operator.UNION)), 20)
		          .leftAssociative(Parser.string("?").thenReturn(
		        		  (e1, e2) -> new CompoundExpr(e1, e2, Operator.IF_THEN)), 10)
		          .build(Parser.anyOf(literal, fieldExpr, sub.between("(", ")"))));
	}

}
