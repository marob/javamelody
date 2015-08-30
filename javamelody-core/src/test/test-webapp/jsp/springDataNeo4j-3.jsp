<%@ page session="false" %>
<%@ page import="net.bull.javamelody.neo4j.domain.Person" %>
<%@ page import="net.bull.javamelody.neo4j.repository.PersonRepository" %>
<%@ page import="org.springframework.context.ApplicationContext" %>
<%@ page import="org.springframework.context.support.AbstractApplicationContext" %>
<%@ page import="org.springframework.context.support.ClassPathXmlApplicationContext" %>

<%
    ApplicationContext context = null;
    try {
        context = new ClassPathXmlApplicationContext( new String[] {
                "net/bull/javamelody/monitoring-spring-data-neo4j.xml", "spring-data-neo4j-3-context.xml"} );

        PersonRepository personRepository = (PersonRepository) context.getBeansOfType(net.bull.javamelody.neo4j.repository.PersonRepository.class).get("personRepository");

        Person jon = new Person("Jon");
        personRepository.save(jon);
        Person emil = new Person("Emil");
        personRepository.save(emil);
        Person rod = new Person("Rod");
        personRepository.save(rod);

        emil.knows(jon);
        emil.knows(rod);

        personRepository.save(emil);
    } finally {
        if(context != null){
            // Fermer le contexte Spring pour déclencher la fermeture de la base Neo4J (sinon, elle reste ouverte et est lockée)
            ((AbstractApplicationContext) context).close();
        }
    }
%>
Spring Data Neo4J done

<br/>
<a href="../index.jsp">back</a>
