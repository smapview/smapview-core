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
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Stack;

import com.smapview.view.LinkUpdater.Link;

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
		
		INPUT_OBJECT(", ");
				
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
	
	static final int ADD_PARENT_REF = 0x01;

	static final int SKIP_ID = 0x02;

	static final int SKIP_POINTER = 0x04;

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
		
	private Scope scope() {
		return stack.peek();
	}

	private void end() {
		stack.pop();
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
		writeInput(data, ADD_PARENT_REF);
		printer.format(" }]) { %s ", data.nodeType.toFieldName());
		data.nodeType.fieldSet.writeTo(printer);
		printer.write(" }");
		return alias;
	}

	String writeUpdate(NodeData data) {
		String alias = scope().ensureIn(Context.GRAPHQL_MUTATION).newItem("_ur%03d");
		printer.format("%s(input: { filter: { id: [\"%s\"] }, set: { ", 
				data.nodeType.toUpdateName(), data.nodeInfo.getNodeId());
		writeInput(data, SKIP_ID | SKIP_POINTER);
		printer.format(" } }) { %s ", data.nodeType.toFieldName());
		data.nodeType.fieldSet.writeTo(printer);
		printer.write(" }");
		return alias;
	}
	
	private void writeInput(NodeData data, int options) {
		scope().ensureIn(Context.GRAPHQL_MUTATION, Context.INPUT_OBJECT);
		start(Context.INPUT_OBJECT);
		boolean addParentRef = (options & ADD_PARENT_REF) != 0;
		boolean skipId = (options & SKIP_ID) != 0;
		boolean skipPointer = (options & SKIP_POINTER) != 0;
		// write id or pointer field
		if (data.getNodeId() != null && ! skipId) {
			append(data.getIdField(), data.getNodeId());
		}
		else if (addParentRef && data.getParentNodeId() != null) {
			printer.write(data.getInversePath().fieldName);
			printer.write(": { ");
			append(data.getParentIdField(), data.getParentNodeId());
			printer.write("}");
			append(data.getPointerField(), data.getPointer());
		}
		else if (! skipPointer) {
			append(data.getPointerField(), data.getPointer());
		}
		// write other fields
		data.forEachValue(f -> ! f.hasAny(FieldTag.POINTER, FieldTag.LINK), 
				this::append);
		end();
	}

	private void append(NodeField field, Object value) {
		if (value == null) {
			throw new IllegalArgumentException("Unexpected null value for " + field);
		}
		else {
			scope().newItem();
			printer.write(field.fieldName);
			printer.write(": ");
			appendValue(field.fieldType, value);
		}
	}
	
	private void appendValue(FieldType fieldType, Object value) {
		switch (fieldType.kind) {
		case LIST:
			if (NodeType.canCreateFrom(fieldType.ofType)) {
				@SuppressWarnings("unchecked")
				List<NodeData> list = (List<NodeData>)value;
				printer.write("[ ");
				for (int i = 0; i < list.size(); i++) {
					if (i > 0) printer.append(", ");
					printer.write("{ ");
					writeInput(list.get(i), 0);
					printer.write(" }");
				}
				printer.write(" ]");
			}
			else if (value instanceof String[]) {
				String[] list = (String[])value;
				printer.append("[");
				for (int i = 0; i < list.length; i++) {
					if (i > 0) printer.append(",");
					switch (fieldType.ofType.kind) {
					case ENUM:
						appendEnum(list[i]);
						break;
					case SCALAR:
						appendString(list[i]);
						break;
					default:
						throw new IllegalArgumentException(
								"Invalid input field type: " + fieldType);
					}
				}
				printer.append("]");
			}
			else throw new IllegalArgumentException(
					"Invalid LIST input type: " + value.getClass());
			break;
		case NON_NULL:
			appendValue(fieldType.ofType, value);
			break;
		case SCALAR:
			if (value instanceof String) appendString((String)value);
			else if (value instanceof Date) appendDateTime((Date)value);
			else throw new IllegalArgumentException(
					"Invalid SCALAR input type: " + value.getClass());
			break;
		case ENUM:
			if (value instanceof String) appendEnum((String)value);
			else throw new IllegalArgumentException(
					"Invalid ENUM input type: " + value.getClass());
			break;
		default:
			throw new IllegalArgumentException(
					"Invalid input field type: " + fieldType);
		}	
	}

	/**
	 * Lists supported input types for a given field type.
	 */
	static List<Class<?>> listInputTypes(FieldType fieldType) {
		switch (fieldType.kind) {
		case LIST:
			if (NodeType.canCreateFrom(fieldType.ofType)) {
				return Arrays.asList(List.class);
			}
			else return  Arrays.asList(String[].class);
		case NON_NULL:
			return listInputTypes(fieldType.ofType);
		case SCALAR:
			return Arrays.asList(String.class, Date.class);
		case ENUM:
			return Arrays.asList(String.class);
		default:
			return Arrays.asList();
		}	
	}
	
	private void appendEnum(String value) {
		printer.append(value);
	}

	private void appendDateTime(Date value) {
		// TODO implement support for DateTime values
		throw new UnsupportedOperationException();
	}

	private void appendString(String value) {
		printer.write("\"");
		for (int i=0; i<value.length(); i++) {
			char c = value.charAt(i);
			switch (c) {
			case '"': 
				printer.write("\\\"");
				break;
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

	void writeSet(Link link, boolean isGlobal) {
		scope().ensureIn(Context.DQL_SET);
		NodeField linkField = view.getLinkStrategy(link.linkId).linkField;
		String format = isGlobal? "<%s> <%s> <%s> (gc=true) ."
				: "<%s> <%s> <%s> (gc=false) .";
		scope().newItem();
		printer.format(format, link.fromNodeId,
				linkField.toString(),
				link.toNodeId);
		scope().newItem();
		printer.format(format, link.toNodeId,
				linkField.getInverseField().toString(),
				link.fromNodeId);
	}
	
	InputStream getResponse() throws ViewRequestException {
		URI endpoint = selectEndpoint();
		String content = getContent();
		String contentType = getContentType();
		HttpRequest request = HttpRequest.newBuilder().uri(endpoint)
				.timeout(Duration.ofSeconds(10))
				.header("Content-Type", contentType)
				.POST(BodyPublishers.ofString(content))
				.build();
		Common.trace("Using endpoint %s to execute request: %s", 
				endpoint, content);
		try {
			return view.client.send(request, BodyHandlers.ofInputStream()).body();
		} catch (IOException | InterruptedException e) {
			throw new ViewRequestException(e);
		}
	}

	JsonValue exec() throws ViewRequestException {
		return exec(true);
	}

	JsonValue exec(boolean traceResult) throws ViewRequestException {
		try (InputStream stream = getResponse()) {
			JsonObject result = Json.createReader(stream).readObject();
			JsonArray errors = result.getJsonArray("errors");
			JsonValue data = result.get("data");
			if (traceResult) Common.trace("Got request result: %s", result);
			if (errors == null || errors.isEmpty()) return data;
			else throw new ViewRequestException(errors);
		} catch (IOException e) {
			throw new ViewRequestException(e);
		}
	}

	JsonArray execToArray() throws ViewRequestException {
		return execToArray(true);
	}

	JsonArray execToArray(boolean traceResult) throws ViewRequestException {
		return (JsonArray)exec(traceResult);
	}

	JsonObject execToObject() throws ViewRequestException {
		return execToObject(true);
	}

	JsonObject execToObject(boolean traceResult) throws ViewRequestException {
		return (JsonObject)exec(traceResult);
	}

	JsonParser execToParser() throws ViewRequestException {
		try (JsonParser parser = Json.createParser(getResponse())) {
			int resultDepth = 0;
			boolean hasErrors = false;
			boolean hasData = false;
			while (parser.hasNext()) {
				JsonParser.Event event = parser.next();
				switch(event) {
				case KEY_NAME:
					String keyName = parser.getString();
					if (resultDepth == 1) {
						if ("errors".equals(keyName)) hasErrors = true;
						else if ("data".equals(keyName)) hasData = true;
						else if ("extensions".equals(keyName)) parser.skipObject(); 
					}
					break;
				case START_ARRAY:
					if (hasErrors) throw new ViewRequestException(parser.getArray());
					else if (hasData) return parser;
					else throw new ViewRequestException("Cannot parse response: unexpected array");
				case START_OBJECT:
					resultDepth++;
					switch (resultDepth) {
					case 1:
						break;
					case 2:
						if (hasData) return parser;
					default:
						throw new ViewRequestException("Cannot parse response: unexpected object");
					}
					break;
				case END_OBJECT:
					resultDepth--;
					break;
				default:
					break;
				}
			}
			throw new ViewRequestException("Cannot parse response");
		}

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
					printer.write(" }");
					break;
				default:
					// invalid initial context
					throw new Error();
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
	
	int getItemCount() {
		return stack.peek().itemCount;
	}

	String getContentType() {
		switch (initialContext) {
		case GRAPHQL_MUTATION:
		case GRAPHQL_QUERY:
			return "application/graphql";
		case DQL_SET:
			return "application/rdf";
		default:
			throw new Error();
		}
	}

}
