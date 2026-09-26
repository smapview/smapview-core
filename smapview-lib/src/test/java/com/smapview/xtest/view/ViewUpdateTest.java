package com.smapview.xtest.view;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.logging.Level;

import org.junit.jupiter.api.Test;

import com.smapview.view.Common;
import com.smapview.view.GraphBuilder;
import com.smapview.view.GraphBuilderException;
import com.smapview.view.GraphSchemaException;
import com.smapview.view.JoinScope;
import com.smapview.view.TestView;
import com.smapview.view.TestViewBuilder;
import com.smapview.view.View;
import com.smapview.view.ViewRequestException;
import com.smapview.view.ViewUpdate;

public class ViewUpdateTest {

	static int ipRoot = 0;
	
	static {
		Common.LOGGER.setLevel(Level.FINEST);
	}

	@Test
	void missingPointerField() throws Exception {
		View view = TestViewBuilder.newBuilder().setSchema("servers").build();
		view.setRootType("ConfigSource");
		view.addPathField("ConfigSource.items", "ConfigItem.source");
		view.addPointerField("ConfigSource.name");
		view.addPointerField("Server.name");
		view.addPointerField("Template.name");
		assertThrows(GraphSchemaException.class, () -> view.startUpdate());
	}

	@Test
	void simpleNodes() throws Exception {
		View view = TestViewBuilder.newBuilder().setSchema("servers").build();
		view.setRootType("ConfigSource");
		view.addPathField("ConfigSource.items", "ConfigItem.source");
		view.addPointerField("ConfigSource.name");
		// here we use the name field defined on the interface
		view.addPointerField("ConfigItem.name");
		ViewUpdate update = view.startUpdate();
		try (GraphBuilder builder = update.startGraphUpdate()) {
			addRoot(builder);
			addTemplates(builder);
			addServers(builder);
			complete(builder);
			update.complete();
		}
	}

	@Test
	void nodesAndSubNodes() throws Exception {
		View view = TestViewBuilder.newBuilder().setSchema("servers").build();
		view.setRootType("ConfigSource");
		view.addPathField("ConfigSource.items", "ConfigItem.source");
		view.addPathField("Server.networkCards", "NetworkCard.server");
		view.addPointerField("ConfigSource.name");
		view.addPointerField("ConfigItem.name");
		view.addPointerField("NetworkCard.name");
		ViewUpdate update = view.startUpdate();
		try (GraphBuilder builder = update.startGraphUpdate()) {
			addRoot(builder);
			addServersWithCards(builder);
			complete(builder);
			update.complete();
		}
	}

	@Test
	void nodesWithLinks() throws Exception {
		TestView view = TestViewBuilder.newBuilder().setSchema("servers").build();
		view.setRootType("ConfigSource");
		view.addPathField("ConfigSource.items", "ConfigItem.source");
		view.addPointerField("ConfigSource.name");
		view.addPointerField("ConfigItem.name");
		view.addLinkField("Server.template", "Template.servers")
		.withSingleJoin("name", JoinScope.GRAPH);
		ViewUpdate update = view.startUpdate();
		try (GraphBuilder builder = update.startGraphUpdate()) {
			addRoot(builder);
			addTemplates(builder);
			addServersWithTemplates(builder);
			complete(builder);
			update.complete();
		}
		view.execGraphQL("{ queryServer { name, template { name } } }")
		.checkValues("template.name", "Ubuntu 16", "Ubuntu 18");
	}
	
	void addRoot(GraphBuilder builder) throws GraphBuilderException {
		builder.node("ConfigSource").set("name", "CMDB");
	}
	
	void complete(GraphBuilder builder) throws GraphBuilderException, ViewRequestException {
		// end root node
		builder.endNode();
	}
	
	void addServers(GraphBuilder builder) throws GraphBuilderException, ViewRequestException {
		builder.node("Server")
		.set("name", "HERMES")
		.endNode();
		builder.node("Server")
		.set("name", "APOLLO")
		.endNode();
	}

	void addTemplates(GraphBuilder builder) throws GraphBuilderException, ViewRequestException {
		builder.node("Template")
		.set("name", "Ubuntu 16")
		.endNode();
		builder.node("Template")
		.set("name", "Ubuntu 18")
		.endNode();
		builder.node("Template")
		.set("name", "Ubuntu 20")
		.endNode();
	}
	
	void addServersWithCards(GraphBuilder builder) throws GraphBuilderException, ViewRequestException {
		builder.node("Server")
		.set("name", "HERMES")
		.node("NetworkCard")
		.set("name", "ETH0")
		.set("ipAddress", "192.168.0.51")
		.endNode()
		.endNode();
		builder.node("Server")
		.set("name", "APOLLO")
		.node("NetworkCard")
		.set("name", "ETH0")
		.set("ipAddress", "192.168.0.52")
		.endNode()
		.endNode();
	}

	void addServersWithTemplates(GraphBuilder builder) throws GraphBuilderException, ViewRequestException {
		builder.node("Server")
		.set("name", "HERMES")
		.set("template", "Ubuntu 16")
		.endNode();
		builder.node("Server")
		.set("name", "APOLLO")
		.set("template", "Ubuntu 18")
		.endNode();
	}

}
