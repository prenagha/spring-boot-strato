package com.renaghan.todo.registration;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;

public class InvitationCodeValidator implements ConstraintValidator<ValidInvitationCode, String> {

  private final Set<String> validInvitationCodes;

  public InvitationCodeValidator(
      @Value("${custom.invitation-codes:none}") Set<String> validInvitationCodes) {
    this.validInvitationCodes = validInvitationCodes;
  }

  @Override
  public void initialize(ValidInvitationCode constraintAnnotation) {
    // intentionally left empty
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {

    if (value == null || value.isEmpty()) {
      return false;
    }

    return validInvitationCodes.contains(value);
  }
}
