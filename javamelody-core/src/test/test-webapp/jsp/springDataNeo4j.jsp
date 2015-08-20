<%@ page session="false" %>
<%@ page import="net.bull.javamelody.neo4j.domain.Person" %>
<%@ page import="net.bull.javamelody.neo4j.repository.PersonRepository" %>
<%@ page import="org.springframework.context.ApplicationContext" %>
<%@ page import="org.springframework.context.support.AbstractApplicationContext" %>
<%@ page import="org.springframework.context.support.ClassPathXmlApplicationContext" %>

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
