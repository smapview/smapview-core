package com.smapview.xtest.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.smapview.view.GraphSchemaException;
import com.smapview.view.TestViewBuilder;
import com.smapview.view.View;

public class ViewTest {
		
	@Test
	void missingRootType() throws Exception {
		assertThrows(IllegalStateException.class, 
				() -> TestViewBuilder.newWithSchema("servers").build());
	}

	@Test
	void missingPointerField() throws Exception {
		assertThrows(GraphSchemaException.class,
				() -> TestViewBuilder.newWithSchema("servers")
				.setRootType("ConfigSource")
				.build());
	}

	@Test
	void addPathFields() throws Exception {
		View view = TestViewBuilder.newWithSchema("servers")
				.setRootType("ConfigSource")
				.addPathField("ConfigSource.items", "ConfigItem.source")
				.addPointerField("ConfigSource.name")
				.addPointerField("ConfigItem.name")
				.build();
		assertEquals(5, view.listNodeTypes().size());
	}

	@Test
	void addAbstractPath() throws Exception {
		assertThrows(IllegalArgumentException.class, 
				() -> TestViewBuilder.newWithSchema("servers")
				.setRootType("ConfigSource")
				.addPathField("ConfigSource.items", "ConfigItem.source")
				.addPathField("ConfigSource.wrong", "HasNoPossibleType.source")
				.addPointerField("ConfigSource.name")
				.addPointerField("ConfigItem.name")
				.build());
	}

	@Test
	void addMorePathFields() throws Exception {
		View view = TestViewBuilder.newWithSchema("servers")
				.setRootType("ConfigSource")
				.addPathField("ConfigSource.items", "ConfigItem.source")
				.addPathField("Server.networkCards", "NetworkCard.server")
				.addPointerField("ConfigSource.name")
				.addPointerField("ConfigItem.name")
				.addPointerField("NetworkCard.name")
				.build();
		assertEquals(6, view.listNodeTypes().size());
	}

}
