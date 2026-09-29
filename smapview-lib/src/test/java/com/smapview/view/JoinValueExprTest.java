package com.smapview.view;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import org.junit.jupiter.api.Test;

public class JoinValueExprTest {

	class EvalContext {
		
		final NodeData node;

		final NodeData parent;

		public EvalContext(NodeData node, NodeData parent) {
			this.node = node;
			this.parent = parent;
		}

		void assertExprValues(String expr, String... values) {
			JoinValueExpr jvex = new JoinValueExpr(expr, node.nodeType);
			List<String> result = jvex.eval(node, parent);
			Common.trace("Expression {%s} result: %s", expr, result);
			assertTrue(Arrays.asList(values).equals(result));
		}

	}
	
	static {
		Common.LOGGER.setLevel(Level.FINEST);
	}
	
	@Test
	void singleValue() throws Exception {
		EvalContext context = bookContext();
		context.assertExprValues("title", "Lessons learned from my uncle");
		context.assertExprValues("'title: ' + title", "title: Lessons learned from my uncle");
		context.assertExprValues("library.name", "All Books");
		context.assertExprValues("title.findAll('from my ([^ ]+)')", "uncle");
		context.assertExprValues("title.replaceAll('uncle','mother')", 
				"Lessons learned from my mother");
	}

	@Test
	void multiValues() throws Exception {
		EvalContext context = bookContext();
		context.assertExprValues("authors", "Alex Sogar", "Miet Hanke");
	}

	@Test
	void compound() throws Exception {
		EvalContext context = bookContext();
		context.assertExprValues("title + ' by ' + authors", 
				"Lessons learned from my uncle by Alex Sogar",
				"Lessons learned from my uncle by Miet Hanke");
		context.assertExprValues("quotes.topics", 
				"Family", "Nature", "Horses");
	}

	@Test
	void ifThen() throws Exception {
		EvalContext context = bookContext();
		context.assertExprValues("category.equals('Nonfiction')? title", 
				"Lessons learned from my uncle");
		context.assertExprValues("category.equals('Fiction')? title");
	}

	EvalContext bookContext() 
			throws GraphBuilderException, GraphSchemaException, 
			ViewRequestException, IOException 
	{
		View view = TestViewBuilder.newWithSchema("books")
				.setRootType("Library")
				.addPathField("Library.books", "Book.library")
				.addPathField("Book.quotes", "Quote.book")
				.addPointerField("Library.name")
				.addPointerField("Book.title")
				.addPointerField("Quote.text")
				.build();
		NodeDataBuilder builder = NodeDataBuilder.newBuilder(view)
				.addRoot("Library").set("name", "All Books");
		NodeData library = builder.get();
		builder.add("books", "Book")
		.set("title", "Lessons learned from my uncle")
		.set("authors", "Alex Sogar", "Miet Hanke")
		.set("category", "Nonfiction");
		NodeData book = builder.get();
		builder.add("quotes", "Quote")
		.set("text", "My uncle was giving me so much")
		.set("topics", "Family").endNode();
		builder.add("quotes", "Quote")
		.set("text", "He used to ride many times a week")
		.set("topics", "Nature", "Horses").endNode();
		return new EvalContext(book, library);
	}
	
}
