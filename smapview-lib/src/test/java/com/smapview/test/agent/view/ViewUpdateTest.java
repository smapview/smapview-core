package com.smapview.test.agent.view;

import java.util.logging.Level;

import org.junit.jupiter.api.Test;

import com.smapview.view.Common;
import com.smapview.view.GraphBuilder;
import com.smapview.view.View;
import com.smapview.view.ViewUpdate;

public class ViewUpdateTest {

	static int ipRoot = 0;
	
	static {
		Common.LOGGER.setLevel(Level.FINEST);
	}

	@Test
	void createGraph1() throws Exception {
		View view = new View("http://localhost:8080");
		view.setRootType("ConfigSource");
		view.addPathField("ConfigSource.items", "ConfigItem.source");
		view.addPathField("Server.networkCards", "NetworkCard.server");
		view.addPointerField("ConfigSource.name");
		view.addPointerField("Server.name");
		view.addPointerField("NetworkCard.name");
		view.addPointerField("Template.name");
		view.addPointerField("Package.name");
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
		view.addPathField("ConfigSource.items", "ConfigItem.source");
		view.addPathField("Server.networkCards", "NetworkCard.server");
		// slight difference here: we tag name field on the interface
		view.addPointerField("ConfigSource.name");
		view.addPointerField("ConfigItem.name");
		view.addPointerField("NetworkCard.name");
		ViewUpdate update = view.startUpdate();
		try (GraphBuilder builder = update.startGraphUpdate()) {
			builder.node("ConfigSource").set("name", "CMDB");
			addServer(builder, "HERMES", "Ubuntu 16", "ETH0");
			addServer(builder, "APOLLO", "Ubuntu 18", "ETH0");
			addTemplate(builder, "Ubuntu 16");
			addTemplate(builder, "Ubuntu 18");
			addTemplate(builder, "Ubuntu 20");
		}
		update.complete();
	}

	@Test
	void createGraph3() throws Exception {
		View view = new View("http://localhost:8080");
		view.setRootType("ConfigSource");
		view.addPathField("ConfigSource.items", "ConfigItem.source");
		view.addPathField("Server.networkCards", "NetworkCard.server");
		view.addPointerField("ConfigSource.name");
		view.addPointerField("ConfigItem.name");
		view.addPointerField("NetworkCard.name");
		ViewUpdate update = view.startUpdate();
		try (GraphBuilder builder = update.startGraphUpdate()) {
			builder.node("ConfigSource").set("name", "CMDB");
			addServer(builder, "HERMES", "Ubuntu 16", "ETH0");
			addServer(builder, "APOLLO", "Ubuntu 18", "ETH0", "ETH1");
			addTemplate(builder, "Ubuntu 16");
			addTemplate(builder, "Ubuntu 18");
			addTemplate(builder, "Ubuntu 20");
		}
		update.complete();
	}

	static void addServer(GraphBuilder builder, String name, String templateName, String... cardNames) throws Exception {
		// TODO handle template name
		builder.node("Server").set("name", name);
		for (String cardName : cardNames) {
			builder.node("NetworkCard")
			.set("name", cardName)
			.set("ipAddress", "192.168.0." + (++ipRoot))
			.endNode();
		}
		builder.endNode();
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
