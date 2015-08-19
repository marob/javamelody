package net.bull.javamelody;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.event.KernelEventHandler;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.graphdb.traversal.BidirectionalTraversalDescription;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.kernel.TransactionBuilder;
import org.neo4j.kernel.impl.nioneo.store.StoreId;

public class GraphDatabaseWrapper implements GraphDatabaseService, GraphDatabaseAPI {

    private final GraphDatabaseAPI proxy;

    public GraphDatabaseWrapper(GraphDatabaseAPI graphDatabaseAPI) {
        proxy = GraphWrapper.SINGLETON.createProxy(graphDatabaseAPI);
    }

    @Override
    public Node createNode() {
        return proxy.createNode();
    }

    @Override
    public Node createNode(Label... labels) {
        return proxy.createNode(labels);
    }

    @Override
    public Node getNodeById(long id) {
        return proxy.getNodeById(id);
    }

    @Override
    public Relationship getRelationshipById(long id) {
        return proxy.getRelationshipById(id);
    }

    @Override
    public Iterable<Node> getAllNodes() {
        return proxy.getAllNodes();
    }

    @Override
    public ResourceIterable<Node> findNodesByLabelAndProperty(Label label, String key, Object value) {
        return proxy.findNodesByLabelAndProperty(label, key, value);
    }

    @Override
    public Iterable<RelationshipType> getRelationshipTypes() {
        return proxy.getRelationshipTypes();
    }

    @Override
    public boolean isAvailable(long timeout) {
        return proxy.isAvailable(timeout);
    }

    @Override
    public void shutdown() {
        proxy.shutdown();
    }

    @Override
    public Transaction beginTx() {
        return proxy.beginTx();
    }

    @Override
    public <T> TransactionEventHandler<T> registerTransactionEventHandler(TransactionEventHandler<T> handler) {
        return proxy.registerTransactionEventHandler(handler);
    }

    @Override
    public <T> TransactionEventHandler<T> unregisterTransactionEventHandler(TransactionEventHandler<T> handler) {
        return proxy.unregisterTransactionEventHandler(handler);
    }

    @Override
    public KernelEventHandler registerKernelEventHandler(KernelEventHandler handler) {
        return proxy.registerKernelEventHandler(handler);
    }

    @Override
    public KernelEventHandler unregisterKernelEventHandler(KernelEventHandler handler) {
        return proxy.unregisterKernelEventHandler(handler);
    }

    @Override
    public Schema schema() {
        return proxy.schema();
    }

    @Override
    public IndexManager index() {
        return proxy.index();
    }

    @Override
    public TraversalDescription traversalDescription() {
        return proxy.traversalDescription();
    }

    @Override
    public BidirectionalTraversalDescription bidirectionalTraversalDescription() {
        return proxy.bidirectionalTraversalDescription();
    }

    @Override
    public DependencyResolver getDependencyResolver() {
        return proxy.getDependencyResolver();
    }

    @Override
    public StoreId storeId() {
        return proxy.storeId();
    }

    @Override
    public TransactionBuilder tx() {
        return proxy.tx();
    }

    @Override
    public String getStoreDir() {
        return proxy.getStoreDir();
    }
}
