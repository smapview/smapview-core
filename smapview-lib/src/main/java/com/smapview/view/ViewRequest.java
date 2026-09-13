package com.smapview.view;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Stack;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.stream.JsonParser;

class ViewRequest {

	enum Context {
		
		DQL_SET("\n    "),
		
		GRAPHQL_QUERY(null),
		
		GRAPHQL_MUTATION("\n  "),
				
		GRAPHQL_INPUT_OBJECT(", ");
		
		final String itemSeparator;
		
		Context(String itemSeparator) {
			this.itemSeparator = itemSeparator;
		}
				
	}
	
	private class Scope {
		
		final Context context;
		
		final int maxItemCount;

		int itemCount = 0;
				
		public Scope(Context type, int maxItemCount) {
			if (maxItemCount > MAX_ITEM_COUNT) throw new IllegalArgumentException();
			this.context = type;
			this.maxItemCount = maxItemCount;
		}
		
		Scope ensureIn(Context... types) {
			Context current = stack.peek().context;
			for (Context type : types) {
				if (current == type) return this;
			}
			throw new IllegalStateException();
		}
		
		String newItem() {
			return newItem(null);
		}

		String newItem(String alias) {
			if (itemCount == maxItemCount) throw new IllegalStateException();
			if (itemCount++ > 0) printer.write(context.itemSeparator);
			if (alias != null) {
				alias = String.format(alias, itemCount);
				printer.write(alias);
				printer.write(": ");
			}
			return alias;
		}

	}
	
	static final int MAX_ITEM_COUNT = 999;
	
	final View view;
	
	final Context initialContext;
	
	final private StringWriter buffer = new StringWriter();
	
	final private PrintWriter printer = new PrintWriter(buffer);
	
	final private Stack<Scope> stack = new Stack<>();

	String content;

	ViewRequest(View view, Context context) {
		this.view = view;
		this.initialContext = context;
		switch (context) {
		case GRAPHQL_QUERY:
			printer.write("{ ");
			start(context, 1);
			break;
		case GRAPHQL_MUTATION:
			printer.write("mutation {\n  ");
			start(context);
			break;
		case GRAPHQL_INPUT_OBJECT:
			printer.write("{ ");
			start(context);
			break;
		case DQL_SET:
			printer.write("{\n  set {\n    ");
			start(context);
			break;
		default:
			break;
		}
	}
	
	ViewRequest(View view, String graphqlQuery, Object... parameters) {
		this(view, Context.GRAPHQL_QUERY);
		printer.format(graphqlQuery, parameters);
	}

	URI selectEndpoint() {
		switch (initialContext) {
		case GRAPHQL_INPUT_OBJECT:
		case GRAPHQL_MUTATION:
		case GRAPHQL_QUERY:
			return view.graphqlEndpoint;
		case DQL_SET:
			return view.mutateEndpoint;
		default:
			throw new Error();
		}
	}

	private void start(Context type) {
		start(type, MAX_ITEM_COUNT);
	}

	private void start(Context type, int maxItems) {
		stack.push(new Scope(type, maxItems));
	}
	
	private void end() {
		stack.pop();
	}
	
	private Scope scope() {
		return stack.peek();
	}

	void writeGet(NodeType type, String nodeId) {
		scope().ensureIn(Context.GRAPHQL_QUERY);
		printer.format("%s(%s: \"%s\") ", type.toGetName(),
				type.getIdField().fieldName, nodeId);
		type.fieldSet.writeTo(printer);
	}
	
	void writeQuery(GraphFieldSet fieldSet) {
		scope().ensureIn(Context.GRAPHQL_QUERY);
		printer.print(fieldSet.type.toQueryName());
		printer.print(" ");
		fieldSet.writeTo(printer);
	}

	String writeAdd(NodeData data) {
		String alias = scope().ensureIn(Context.GRAPHQL_MUTATION).newItem("_ar%03d");
		printer.format("%s(input: [{ ", data.nodeType.toAddName());
		start(Context.GRAPHQL_INPUT_OBJECT);
		data.writeTo(this);
		end();
		printer.format(" }]) { %s ", data.nodeType.toFieldName());
		data.nodeType.fieldSet.writeTo(printer);
		printer.write(" }");
		return alias;
	}

	String writeUpdate(NodeData data) {
		String alias = scope().ensureIn(Context.GRAPHQL_MUTATION).newItem("_ur%03d");
		// TODO complete this
		return alias;
	}

	void startInputObject(NodeField field) {
		scope().ensureIn(Context.GRAPHQL_INPUT_OBJECT).newItem(field.fieldName);
		start(Context.GRAPHQL_INPUT_OBJECT);
		printer.write("{ ");
	}

	void endInputObject() {
		// cannot close initial scope
		if (stack.size() == 1) throw new IllegalArgumentException();
		scope().ensureIn(Context.GRAPHQL_INPUT_OBJECT);
		end();
		printer.write(" }");
	}

	void append(NodeField field, String[] values) {
		scope().ensureIn(Context.GRAPHQL_INPUT_OBJECT).newItem(field.fieldName);
		printer.write("[");
		for (int i=0; i<values.length; i++) {
			if (i>0) printer.write(",");
			appendValue(values[i]);
		}
		printer.write("]");
	}

	void append(NodeField field, String value) {
		scope().ensureIn(Context.GRAPHQL_INPUT_OBJECT).newItem(field.fieldName);
		appendValue(value);
	}
	
	private void appendValue(String value) {
		printer.write("\"");
		for (int i=0; i<value.length(); i++) {
			char c = value.charAt(i);
			switch (c) {
			case '\b': 
				printer.write("\\b");
				break;
			case '\f': 
				printer.write("\\f");
				break;
			case '\n': 
				printer.write("\\n");
				break;
			case '\r': 
				printer.write("\\r");
				break;
			case '\t': 
				printer.write("\\t");
				break;
			case '\\': 
				printer.write("\\\\");
				break;
			default:
				// TODO also escape unicode characters
				printer.write(c);
			}
		}
		printer.write("\"");
	}

	void append(String nodeId, NodeField field, String value) {
		scope().ensureIn(Context.DQL_SET).newItem();
	}
	
	InputStream getResponse() throws ViewRequestException {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(selectEndpoint())
				.timeout(Duration.ofSeconds(10))
				.header("Content-Type", "application/graphql")
				.POST(BodyPublishers.ofString(getContent()))
				.build();
		try {
			return view.client.send(request, BodyHandlers.ofInputStream()).body();
		} catch (IOException | InterruptedException e) {
			throw new ViewRequestException(e);
		}
	}

	JsonValue exec() throws ViewRequestException {
		Common.trace("Sending GraphQL request: %s", getContent());
		try (InputStream stream = getResponse()) {
			JsonObject result = Json.createReader(stream).readObject();
			JsonArray errors = result.getJsonArray("errors");
			JsonValue data = result.get("data");
			if (errors == null || errors.isEmpty()) return data;
			else throw new ViewRequestException(errors);
		} catch (IOException e) {
			throw new ViewRequestException(e);
		}
	}

	JsonArray execToArray() throws ViewRequestException {
		return (JsonArray)exec();
	}

	JsonObject execToObject() throws ViewRequestException {
		return (JsonObject)exec();
	}

	JsonParser execToParser() throws ViewRequestException {
		Common.trace("GraphQL request: %s", getContent());
		return Json.createParser(getResponse());
	}
	
	private String getContent() throws ViewRequestException {
		if (content == null) {
			switch (stack.size()) {
			case 1:
				switch (stack.pop().context) {
				case DQL_SET:
					printer.write("\n  }\n}");
					break;
				case GRAPHQL_MUTATION:
					printer.write("\n}");
					break;
				case GRAPHQL_QUERY:
				case GRAPHQL_INPUT_OBJECT:
					printer.write(" }");
					break;
				}
				printer.flush();
				break;
			case 0:
				break;
			default: 
				throw new ViewRequestException("Incomplete request");
			}
			content = buffer.toString();
		}
		return content;
	}

}
