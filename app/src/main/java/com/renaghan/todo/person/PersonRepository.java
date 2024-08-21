package com.renaghan.todo.person;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository extends JpaRepository<Person, Long> {
  Optional<Person> findByName(String name);

  Optional<Person> findByEmail(String email);

  List<Person> findByEmailNot(String email);
}
