package com.smapview.view;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
		View view = TestViewBuilder.newBuilder().setSchema("books").build();
		view.setRootType("Library");
		view.addPathField("Library.books", "Book.library");
		view.addPathField("Book.quotes", "Quote.book");
		EvalContext context = bookContext(view);
		context.assertExprValues("title", "Lessons learned from my uncle");
		context.assertExprValues("'title: ' + title", "title: Lessons learned from my uncle");
		context.assertExprValues("library.name", "All Books");
		context.assertExprValues("title.findAll('from my ([^ ]+)')", "uncle");
		context.assertExprValues("title.replaceAll('uncle','mother')", 
				"Lessons learned from my mother");
	}

	@Test
	void multiValues() throws Exception {
		View view = TestViewBuilder.newBuilder().setSchema("books").build();
		view.setRootType("Library");
		view.addPathField("Library.books", "Book.library");
		view.addPathField("Book.quotes", "Quote.book");
		EvalContext context = bookContext(view);
		context.assertExprValues("authors", "Alex Sogar", "Miet Hanke");
	}

	@Test
	void compound() throws Exception {
		View view = TestViewBuilder.newBuilder().setSchema("books").build();
		view.setRootType("Library");
		view.addPathField("Library.books", "Book.library");
		view.addPathField("Book.quotes", "Quote.book");
		EvalContext context = bookContext(view);
		context.assertExprValues("title + ' by ' + authors", 
				"Lessons learned from my uncle by Alex Sogar",
				"Lessons learned from my uncle by Miet Hanke");
		context.assertExprValues("quotes.topics", 
				"Family", "Nature", "Horses");
	}

	@Test
	void ifThen() throws Exception {
		View view = TestViewBuilder.newBuilder().setSchema("books").build();
		view.setRootType("Library");
		view.addPathField("Library.books", "Book.library");
		view.addPathField("Book.quotes", "Quote.book");
		EvalContext context = bookContext(view);
		context.assertExprValues("category.equals('Nonfiction')? title", 
				"Lessons learned from my uncle");
		context.assertExprValues("category.equals('Fiction')? title");
	}

	EvalContext bookContext(View view) throws GraphBuilderException {
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
