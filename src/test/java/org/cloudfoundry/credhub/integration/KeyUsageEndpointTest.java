package org.cloudfoundry.credhub.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import org.cloudfoundry.credhub.CredentialManagerApp;
import org.cloudfoundry.credhub.util.DatabaseProfileResolver;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.cloudfoundry.credhub.helper.RequestHelper.generateCertificateCredential;
import static org.cloudfoundry.credhub.util.AuthConstants.ALL_PERMISSIONS_TOKEN;
import static org.cloudfoundry.credhub.util.AuthConstants.USER_A_TOKEN;
import static org.hamcrest.core.IsAnything.anything;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = CredentialManagerApp.class)
@ActiveProfiles(value = "unit-test", resolver = DatabaseProfileResolver.class)
@Transactional
//@Ignore
public class KeyUsageEndpointTest {
  @Autowired
  private WebApplicationContext webApplicationContext;

  private MockMvc mockMvc;

  @Test
  public void GET_whenTheCredentialNameParameterIsMissing_returnsAnAppropriateError() throws Exception {
    mockMvc = MockMvcBuilders
      .webAppContextSetup(webApplicationContext)
      .apply(springSecurity())
      .build();

    MockHttpServletRequestBuilder getRequest = get(
      "/api/v1/key-usage")
      .header("Authorization", "Bearer " + ALL_PERMISSIONS_TOKEN);
    mockMvc.perform(getRequest)
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.active_key", anything()))
      .andExpect(jsonPath("$.inactive_keys", anything()))
      .andExpect(jsonPath("$.unknown_keys", anything()));
  }

  @Test
  public void GET_whenThereAreCredentialsInTheDatabase_returnsCorrectly() throws Exception {
    mockMvc = MockMvcBuilders
      .webAppContextSetup(webApplicationContext)
      .apply(springSecurity())
      .build();

    generateCertificateCredential(mockMvc, "/user-a/first-certificate", true, "test", null, USER_A_TOKEN);

    final MockHttpServletRequestBuilder getRequest = get(
      "/api/v1/key-usage")
      .header("Authorization", "Bearer " + ALL_PERMISSIONS_TOKEN);
    mockMvc.perform(getRequest)
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.active_key").value(1))
      .andExpect(jsonPath("$.inactive_keys").value(0))
      .andExpect(jsonPath("$.unknown_keys").value(0));
  }


}
