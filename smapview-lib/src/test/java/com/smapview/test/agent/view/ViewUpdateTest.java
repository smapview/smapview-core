package com.smapview.test.agent.view;

import java.util.logging.Level;

import org.junit.jupiter.api.Test;

import com.smapview.view.Common;
import com.smapview.view.GraphBuilder;
import com.smapview.view.View;
import com.smapview.view.ViewUpdate;

public class ViewUpdateTest {

	static {
		Common.LOGGER.setLevel(Level.FINEST);
	}

	@Test
	void createGraph1() throws Exception {
		View view = new View("http://localhost:8080");
		view.setRootType("ConfigSource");
		view.tagPathField("ConfigSource.items", "ConfigItem.source");
		view.tagPathField("Server.networkCards", "NetworkCard.server");
		view.tagPointerField("ConfigSource.name");
		view.tagPointerField("Server.name");
		view.tagPointerField("NetworkCard.name");
		view.tagPointerField("Template.name");
		view.tagPointerField("Package.name");
		ViewUpdate update = view.startUpdate();
		try (GraphBuilder builder = update.startGraphUpdate()) {
			builder.node("ConfigSource").set("name", "CMDB");
			addServer(builder, "HERMES", "Ubuntu 16");
			addServer(builder, "APOLLO", "Ubuntu 18");
			addTemplate(builder, "Ubuntu 16");
			addTemplate(builder, "Ubuntu 18");
			addTemplate(builder, "Ubuntu 20");
		}
		update.complete();
	}

	@Test
	void createGraph2() throws Exception {
		View view = new View("http://localhost:8080");
		view.setRootType("ConfigSource");
		view.tagPathField("ConfigSource.items", "ConfigItem.source");
		view.tagPathField("Server.networkCards", "NetworkCard.server");
		// slight difference here: we tag name field on the interface
		view.tagPointerField("ConfigSource.name");
		view.tagPointerField("ConfigItem.name");
		view.tagPointerField("NetworkCard.name");
		ViewUpdate update = view.startUpdate();
		try (GraphBuilder builder = update.startGraphUpdate()) {
			builder.node("ConfigSource").set("name", "CMDB");
			addServer(builder, "HERMES", "Ubuntu 16");
			addServer(builder, "APOLLO", "Ubuntu 18");
			addTemplate(builder, "Ubuntu 16");
			addTemplate(builder, "Ubuntu 18");
			addTemplate(builder, "Ubuntu 20");
		}
		update.complete();
	}

	static void addServer(GraphBuilder builder, String name, String templateName) throws Exception {
		// TODO handle template name
		builder.node("Server")
		.set("name", name)
		.endNode();
	}

	static void addTemplate(GraphBuilder builder, String name) throws Exception {
		builder.node("Template")
		.set("name", name)
		.endNode();
	}

}
