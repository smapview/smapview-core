package com.smapview.xtest.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.logging.Level;

import org.junit.jupiter.api.Test;

import com.smapview.view.Common;
import com.smapview.view.TestViewBuilder;
import com.smapview.view.View;

public class ViewTest {
	
	static {
		Common.LOGGER.setLevel(Level.FINEST);
	}
	
	@Test
	void createViewEndpoint() throws Exception {
		View view = TestViewBuilder.newBuilder().setSchema("servers").build();
		assertEquals(0, view.getTypeMap().size());
	}

	@Test
	void setRootType() throws Exception {
		View view = TestViewBuilder.newBuilder().setSchema("servers").build();
		view.setRootType("ConfigSource");
		assertEquals(1, view.getTypeMap().size());
	}

	@Test
	void addPathFields() throws Exception {
		View view = TestViewBuilder.newBuilder().setSchema("servers").build();
		view.setRootType("ConfigSource");
		view.addPathField("ConfigSource.items", "ConfigItem.source");
		view.addPathField("Server.networkCards", "NetworkCard.server");
		assertEquals(6, view.getTypeMap().size());
	}

}
