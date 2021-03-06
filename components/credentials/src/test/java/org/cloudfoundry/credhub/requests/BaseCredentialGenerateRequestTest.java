package org.cloudfoundry.credhub.requests;

import org.cloudfoundry.credhub.ErrorMessages;
import org.cloudfoundry.credhub.exceptions.ParameterizedValidationException;
import org.cloudfoundry.credhub.helpers.JsonTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class BaseCredentialGenerateRequestTest {

  @Test
  public void validate_whenTheRequestIsValid_doesNotHaveAnyConstraintValidations() {
    final String json = "{"
      + "\"name\":\"some-name\","
      + "\"type\":\"password\","
      + "\"overwrite\":true"
      + "}";

    final BaseCredentialGenerateRequest request = JsonTestHelper
      .deserialize(json, BaseCredentialGenerateRequest.class);
    request.validate();
  }

  @Test
  public void validate_whenTypeIsNotSet_throwsInvalidTypeWithGeneratePromptError() throws Exception {
    try {
      final String json = "{"
        + "\"name\":\"some-name\","
        + "\"overwrite\":true"
        + "}";

      final BaseCredentialGenerateRequest request = JsonTestHelper
        .deserialize(json, BaseCredentialGenerateRequest.class);
      request.validate();
    } catch (final ParameterizedValidationException e) {
      assertThat(e.getMessage(), equalTo(ErrorMessages.INVALID_TYPE_WITH_GENERATE_PROMPT));
    }
  }


  @Test
  public void validate_whenTypeIsValue_throwsException() {
    try {
      final String json = "{"
        + "\"name\":\"some-name\","
        + "\"type\":\"value\","
        + "\"overwrite\":true"
        + "}";

      final BaseCredentialGenerateRequest request = JsonTestHelper
        .deserialize(json, BaseCredentialGenerateRequest.class);
      request.validate();
    } catch (final ParameterizedValidationException e) {
      assertThat(e.getMessage(), equalTo(ErrorMessages.CANNOT_GENERATE_TYPE));
    }
  }

  @Test
  public void validate_whenTypeIsJson_throwsException() {
    try {
      final String json = "{"
        + "\"name\":\"some-name\","
        + "\"type\":\"json\","
        + "\"overwrite\":true"
        + "}";

      final BaseCredentialGenerateRequest request = JsonTestHelper
        .deserialize(json, BaseCredentialGenerateRequest.class);
      request.validate();
    } catch (final ParameterizedValidationException e) {
      assertThat(e.getMessage(), equalTo(ErrorMessages.CANNOT_GENERATE_TYPE));
    }
  }

  @Test
  public void validate_whenTypeIsTotallyWrong_throwsException() {
    try {
      final String json = "{"
        + "\"name\":\"some-name\","
        + "\"type\":\"banana\","
        + "\"overwrite\":true"
        + "}";

      final BaseCredentialGenerateRequest request = JsonTestHelper
        .deserialize(json, BaseCredentialGenerateRequest.class);
      request.validate();
    } catch (final ParameterizedValidationException e) {
      assertThat(e.getMessage(), equalTo(ErrorMessages.INVALID_TYPE_WITH_GENERATE_PROMPT));
    }
  }
}
