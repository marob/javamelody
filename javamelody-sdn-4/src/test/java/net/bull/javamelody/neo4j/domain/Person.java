package net.bull.javamelody.neo4j.domain;

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.springframework.data.annotation.Id;

import java.util.HashSet;
import java.util.Set;

@NodeEntity
public class Person {
    @GraphId
    @Id
    private Long id;

    private String name;

    @Relationship(type = "KNOWS", direction = Relationship.UNDIRECTED)
    private Set<Person> friends = new HashSet<Person>();

    public Person() {}
    public Person(String name) { this.name = name; }

    public void knows(Person friend) { friends.add(friend); }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
}