package com.renaghan.todo.dashboard;

import com.renaghan.todo.person.Person;
import com.renaghan.todo.person.PersonRepository;
import com.renaghan.todo.todo.TodoRepository;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DashboardService {

  private final PersonRepository personRepository;
  private final TodoRepository todoRepository;

  public DashboardService(PersonRepository personRepository, TodoRepository todoRepository) {
    this.personRepository = personRepository;
    this.todoRepository = todoRepository;
  }

  public List<CollaboratorDto> getAvailableCollaborators(String email) {
    List<Person> collaborators = personRepository.findByEmailNot(email);

    return collaborators.stream()
        .map(person -> new CollaboratorDto(person.getId(), person.getName()))
        .collect(Collectors.toList());
  }

  public List<TodoDto> getAllOwnedAndSharedTodos(String email) {

    List<TodoDto> ownedTodos =
        todoRepository.findAllByOwnerEmailOrderByIdAsc(email).stream()
            .map(todo -> new TodoDto(todo, false))
            .collect(Collectors.toList());

    List<TodoDto> collaborativeTodos =
        todoRepository.findAllByCollaboratorsEmailOrderByIdAsc(email).stream()
            .map(todo -> new TodoDto(todo, true))
            .collect(Collectors.toList());

    ownedTodos.addAll(collaborativeTodos);

    return ownedTodos;
  }
}
