package com.renaghan.todo.collaboration;

import com.renaghan.todo.tracing.TracingEvent;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;

@Component
public class TodoSharingListener {

  private final MailSender mailSender;
  private final TodoCollaborationService todoCollaborationService;
  private final boolean autoConfirmCollaborations;
  private final String confirmEmailFromAddress;
  private final String externalUrl;
  private final ApplicationEventPublisher eventPublisher;

  private static final Logger LOG = LoggerFactory.getLogger(TodoSharingListener.class.getName());

  public TodoSharingListener(
      MailSender mailSender,
      TodoCollaborationService todoCollaborationService,
      @Value("${custom.auto-confirm-collaborations}") boolean autoConfirmCollaborations,
      @Value("${custom.confirm-email-from-address}") String confirmEmailFromAddress,
      @Value("${custom.external-url}") String externalUrl,
      ApplicationEventPublisher eventPublisher) {
    this.mailSender = mailSender;
    this.todoCollaborationService = todoCollaborationService;
    this.autoConfirmCollaborations = autoConfirmCollaborations;
    this.confirmEmailFromAddress = confirmEmailFromAddress;
    this.externalUrl = externalUrl;
    this.eventPublisher = eventPublisher;
  }

  @SqsListener(value = "${custom.sharing-queue}")
  public void listenToSharingMessages(TodoCollaborationNotification payload)
      throws InterruptedException {
    LOG.info("Incoming todo sharing payload: {}", payload);

    // event which is then async written to dynamodb breadcrumb table
    this.eventPublisher.publishEvent(
        new TracingEvent(
            this, "collab:request:" + payload.getTodoId(), payload.getCollaboratorEmail()));

    String body =
        String.format(
            """
    Hi %s,\s

    someone shared a Todo from %s with you.

    Information about the shared Todo item:\s

    Title: %s\s
    Description: %s\s
    Priority: %s\s

    You can accept the collaboration by clicking this link: %s/todo/%s/collaborations/%s/confirm?token=%s\s

    Kind regards,\s
    Renaghan todo-app""",
            payload.getCollaboratorEmail(),
            externalUrl,
            payload.getTodoTitle(),
            payload.getTodoDescription(),
            payload.getTodoPriority(),
            externalUrl,
            payload.getTodoId(),
            payload.getCollaboratorId(),
            payload.getToken());

    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(confirmEmailFromAddress);
    message.setTo(payload.getCollaboratorEmail());
    message.setSubject("A todo was shared with you");
    message.setText(body);
    mailSender.send(message);

    LOG.info("Successfully informed collaborator about shared todo {}", body);

    if (autoConfirmCollaborations) {
      LOG.info("Auto-confirmed collaboration request for todo: {}", payload.getTodoId());
      Thread.sleep(2_500);
      todoCollaborationService.confirmCollaboration(
          payload.getCollaboratorEmail(),
          payload.getTodoId(),
          payload.getCollaboratorId(),
          payload.getToken());
    }
  }
}
