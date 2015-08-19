<%@ page session="false" %>
<%@ page import="org.springframework.context.ApplicationContext" %>
<%@ page import="org.springframework.context.support.ClassPathXmlApplicationContext" %>
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
<%@ page import="net.bull.javamelody.neo4j.domain.Person" %>
<%@ page import="net.bull.javamelody.neo4j.repository.PersonRepository" %>
<%@ page import="org.springframework.context.support.AbstractApplicationContext" %>
<%@ page import="org.springframework.data.neo4j.conversion.Result" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.neo4j.kernel.Traversal" %>
<%@ page import="org.neo4j.graphdb.traversal.Evaluators" %>

Node ids :
<ul>
    <%
        ApplicationContext context = null;
        try {
            context = new ClassPathXmlApplicationContext( new String[] {
                    "net/bull/javamelody/monitoring-spring-data-neo4j.xml", "spring-context.xml"} );

            PersonRepository personRepository = (PersonRepository) context.getBeansOfType(net.bull.javamelody.neo4j.repository.PersonRepository.class).get("personRepository");

            Person jon = new Person("Jon");
            personRepository.save(jon);
            Person emil = new Person("Emil");
            personRepository.save(emil);
            Person rod = new Person("Rod");
            personRepository.save(rod);

            emil.knows(jon);
            emil.knows(rod);

            // Persist created relationships to graph database
            personRepository.save(emil);

            %><%--Result<Person> result = personRepository.findAll();
            for (Person person : result) {
                %>
                <li>
                    <% out.println(person.getId() + ": " + person.getName()); %>
                </li>
                <%
            }--%><%

        } finally {
            if(context != null){
                ((AbstractApplicationContext) context).close();
            }
        }
    %>
</ul>
neo4j done

<br/>
<a href="../index.jsp">back</a>
