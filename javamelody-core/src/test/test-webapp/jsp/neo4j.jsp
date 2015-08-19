<%@ page session="false" %>
<%@ page import="org.neo4j.graphdb.GraphDatabaseService" %>
<%@ page import="org.neo4j.graphdb.factory.GraphDatabaseFactory" %>
<%@ page import="org.neo4j.graphdb.Node" %>
<%@ page import="org.neo4j.graphdb.Transaction" %>
<%@ page import="net.bull.javamelody.GraphDatabaseWrapper" %>
<%@ page import="org.neo4j.cypher.ExecutionEngine" %>
<%@ page import="org.neo4j.cypher.ExecutionResult" %>
<%@ page import="org.neo4j.helpers.collection.IteratorUtil" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="org.neo4j.kernel.logging.BufferingLogger" %>
<%@ page import="org.neo4j.kernel.GraphDatabaseAPI" %>

Node ids :
<ul>
    <%
        GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase("/tmp/graphdb");
        try {
            // Replace by proxy
            graphDb = new GraphDatabaseWrapper((GraphDatabaseAPI) graphDb);

            Transaction transaction = graphDb.beginTx();
            Node createdNode = graphDb.createNode();
            createdNode.setProperty("message", "Hello World!");

            // Neo4j 2.2.4
            %><%--Result result = graphDb.execute("match (n) return n");
            while (result.hasNext()) {
                Map<String, Object> row = result.next();
                for (Map.Entry<String, Object> column : row.entrySet()) {
                    Node node = (Node) column.getValue();
                    %>
                    <li>
                        <% out.println(node.getId() + ": " + node.getProperty("message")); %>
                    </li>
                    <%
                }
            }--%><%

            // Neo4j 2.1.8
            ExecutionEngine engine = new ExecutionEngine(graphDb, new BufferingLogger());
            ExecutionResult result = engine.execute("match (n) return n");
            Iterator<Node> nodesResult = result.javaColumnAs("n");
            Iterable<Node> nodes = IteratorUtil.asIterable(nodesResult);
            for (Node node : nodes) {
                %>
                <li>
                    <% out.println(node.getId() + ": " + node.getProperty("message")); %>
                </li>
                <%
            }

            transaction.success();
            transaction.close();
        } finally {
            graphDb.shutdown();
        }
    %>
</ul>
neo4j done

<br/>
<a href="../index.jsp">back</a>
