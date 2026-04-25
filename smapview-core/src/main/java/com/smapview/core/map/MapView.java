package com.smapview.core.map;

import java.util.List;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;

public class MapView implements AutoCloseable {

	final DatabaseConnection dbc;
	
	public MapView(MapDatabase db) throws MapDatabaseException {
		db.viewLock.readLock().lock();
		this.dbc = new DatabaseConnection(db);
	}

	Node getNode(String path) {
		return null;
	}
	
	List<Node> getLinkNodes(Node node, String linkName) {
		return null;
	}
	
	List<Node> searchText(String text) {
		return null;
	}
	
	MapGraph getGraph() {
		// TODO complete this
		return null;
	}
	
	ExecutionResult execQuery(String graphQL) {
		// TODO add the appropriate things to this wiring
		// TODO consider using NodeQuery
		RuntimeWiring wiring = RuntimeWiring.newRuntimeWiring().build();
		GraphQLSchema schema = new SchemaGenerator().makeExecutableSchema(getGraph().types, wiring);
		return GraphQL.newGraphQL(schema).build().execute(graphQL);
	}
	
	List<Node> queryNodes(String graphQL) {
		// TODO implement this method
		return null;
	}

	@Override
	public void close() throws Exception {
		dbc.db.viewLock.readLock().unlock();
		dbc.close();
	}
	
	/**
	 * Creates a new pool to update the map.
	 * 
	 * @param source  Identifies the data source used to perform the update.
	 * 
	 * @return The pool created for map update.
	 * 
	 * @throws MapDatabaseException If failing to read from or write to the database. 
	 */
	public NodePool createPool(String source) throws MapDatabaseException {
		dbc.db.poolLock.lock();
		return new NodePool(this, source);
	}
	
	MapRegistry getRegistry() {
		return dbc.db.registry;
	}
	
	public MapSchema getSchema() {
		return getRegistry().getSchema();
	}
	
	/**
	 * Gets the last pool statistics for a given source.
	 * 
	 * @param source  The data source used to perform the update.
	 * 
	 * @return The last-created pool statistics for the specified source.
	 */
	public PoolStats getLastPoolStats(String source) {
		// TODO complete this
		return null;
	}

}
