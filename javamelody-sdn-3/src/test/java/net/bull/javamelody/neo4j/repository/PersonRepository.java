package net.bull.javamelody.neo4j.repository;

import net.bull.javamelody.neo4j.domain.Person;
import org.springframework.data.neo4j.repository.GraphRepository;

public interface PersonRepository extends GraphRepository<Person> {}