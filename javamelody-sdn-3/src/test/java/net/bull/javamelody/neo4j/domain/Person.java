package net.bull.javamelody.neo4j.domain;

import org.neo4j.graphdb.Direction;
import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import java.util.HashSet;
import java.util.Set;

@NodeEntity
public class Person {
    @GraphId
    @Id
    private Long id;

    @Indexed
    private String name;

    @RelatedTo(type = "KNOWS", direction = Direction.BOTH, elementClass = Person.class)
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