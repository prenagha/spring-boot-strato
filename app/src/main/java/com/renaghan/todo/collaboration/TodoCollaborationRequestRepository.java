package com.renaghan.todo.collaboration;

import com.renaghan.todo.person.Person;
import com.renaghan.todo.todo.Todo;
import org.springframework.data.repository.CrudRepository;

public interface TodoCollaborationRequestRepository
    extends CrudRepository<TodoCollaborationRequest, Long> {
  TodoCollaborationRequest findByTodoAndCollaborator(Todo todo, Person person);

  TodoCollaborationRequest findByTodoIdAndCollaboratorId(Long todoId, Long collaboratorId);
}
