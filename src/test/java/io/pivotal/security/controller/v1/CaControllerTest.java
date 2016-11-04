package io.pivotal.security.controller.v1;

import com.greghaskins.spectrum.Spectrum;
import io.pivotal.security.CredentialManagerApp;
import io.pivotal.security.CredentialManagerTestContextBootstrapper;
import io.pivotal.security.data.NamedCertificateAuthorityDataService;
import io.pivotal.security.entity.NamedCertificateAuthority;
import io.pivotal.security.generator.BCCertificateGenerator;
import io.pivotal.security.mapper.CAGeneratorRequestTranslator;
import io.pivotal.security.service.AuditLogService;
import io.pivotal.security.service.AuditRecordParameters;
import io.pivotal.security.view.CertificateAuthority;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.greghaskins.spectrum.Spectrum.beforeEach;
import static com.greghaskins.spectrum.Spectrum.describe;
import static com.greghaskins.spectrum.Spectrum.it;
import static io.pivotal.security.helper.SpectrumHelper.mockOutCurrentTimeProvider;
import static io.pivotal.security.helper.SpectrumHelper.wireAndUnwire;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(Spectrum.class)
@SpringApplicationConfiguration(classes = CredentialManagerApp.class)
@WebAppConfiguration
@BootstrapWith(CredentialManagerTestContextBootstrapper.class)
@ActiveProfiles("unit-test")
public class CaControllerTest {
  private static final Instant FROZEN_TIME_INSTANT = Instant.ofEpochSecond(1400000000L);
  private static final String UPDATED_AT_JSON = "\"updated_at\":\"" + FROZEN_TIME_INSTANT.toString() + "\"";
  private static final String CA_CREATION_JSON = "\"type\":\"root\",\"value\":{\"certificate\":\"my_cert\",\"private_key\":\"private_key\"}";
  private static final String CA_RESPONSE_JSON = "{" + UPDATED_AT_JSON + "," + CA_CREATION_JSON + "}";

  @Autowired
  protected WebApplicationContext context;

  @Mock
  private NamedCertificateAuthorityDataService namedCertificateAuthorityDataService;

  @InjectMocks
  @Autowired
  private CaController caController;

  @Autowired
  @InjectMocks
  CAGeneratorRequestTranslator caGeneratorRequestTranslator;

  @Mock
  BCCertificateGenerator certificateGenerator;

  @Spy
  @Autowired
  @InjectMocks
  AuditLogService auditLogService;

  private MockMvc mockMvc;
  private Consumer<Long> fakeTimeSetter;

  private String uniqueName;
  private String urlPath;
  private UUID uuid;
  private NamedCertificateAuthority fakeGeneratedCa;

  private ResultActions response;

  {
    wireAndUnwire(this);
    fakeTimeSetter = mockOutCurrentTimeProvider(this);

    beforeEach(() -> {
      mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
      fakeTimeSetter.accept(FROZEN_TIME_INSTANT.toEpochMilli());
      uniqueName = "my-folder/ca-identifier";
      urlPath = "/api/v1/ca/" + uniqueName;
    });

    describe("generating a ca", () -> {
      beforeEach(() -> {
        uuid = UUID.randomUUID();
        fakeGeneratedCa = new NamedCertificateAuthority(uniqueName)
            .setType("root")
            .setCertificate("my_cert")
            .setPrivateKey("private_key")
            .setUuid(uuid)
            .setUpdatedAt(FROZEN_TIME_INSTANT);
        doReturn(new CertificateAuthority(fakeGeneratedCa))
            .when(certificateGenerator).generateCertificateAuthority(any(CertificateSecretParameters.class));
        doReturn(
            fakeGeneratedCa
        ).when(namedCertificateAuthorityDataService).save(any(NamedCertificateAuthority.class));

        String requestJson = "{\"type\":\"root\",\"parameters\":{\"common_name\":\"test-ca\"}}";

        RequestBuilder requestBuilder = post(urlPath)
            .content(requestJson)
            .contentType(MediaType.APPLICATION_JSON_UTF8);

        response = mockMvc.perform(requestBuilder);
      });

      it("can generate a ca", () -> {
        response
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(content().json(CA_RESPONSE_JSON));
      });

      it("saves the generated ca in the DB", () -> {
        ArgumentCaptor<NamedCertificateAuthority> argumentCaptor = ArgumentCaptor.forClass(NamedCertificateAuthority.class);

        verify(namedCertificateAuthorityDataService, times(1)).save(argumentCaptor.capture());

        NamedCertificateAuthority savedCa = argumentCaptor.getValue();
        assertThat(savedCa.getName(), equalTo(fakeGeneratedCa.getName()));
        assertThat(savedCa.getCertificate(), equalTo(fakeGeneratedCa.getCertificate()));
        assertThat(savedCa.getPrivateKey(), equalTo(fakeGeneratedCa.getPrivateKey()));
      });

      it("creates an audit entry", () -> {
        verify(auditLogService).performWithAuditing(eq("ca_update"), isA(AuditRecordParameters.class), any(Supplier.class));
      });
    });

    it("returns 400 when json keys are invalid", () -> {
      final MockHttpServletRequestBuilder put = put("/api/v1/ca/test-ca")
          .accept(APPLICATION_JSON)
          .contentType(APPLICATION_JSON)
          .content("{" +
              "  \"type\":\"root\"," +
              "  \"bogus\":\"value\"" +
              "}");
      mockMvc.perform(put)
          .andExpect(status().isBadRequest())
          .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
          .andExpect(jsonPath("$.error").value("All keys are required to set a CA. Please validate your input and retry your request."));
    });

    describe("setting a ca", () -> {
      beforeEach(() -> {
        uuid = UUID.randomUUID();
        doReturn(
            new NamedCertificateAuthority(uniqueName)
                .setType("root")
                .setCertificate("my_cert")
                .setPrivateKey("private_key")
                .setUpdatedAt(FROZEN_TIME_INSTANT)
                .setUuid(uuid)
        ).when(namedCertificateAuthorityDataService).save(any(NamedCertificateAuthority.class));

        String requestJson = "{" + CA_CREATION_JSON + "}";
        RequestBuilder requestBuilder = put(urlPath)
            .content(requestJson)
            .contentType(MediaType.APPLICATION_JSON_UTF8);
        response = mockMvc.perform(requestBuilder);
      });

      it("returns the new root ca", () -> {
        String responseJson = "{" + UPDATED_AT_JSON + "," + CA_CREATION_JSON + "}";
        response.andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(content().json(responseJson));
      });

      it("writes the new root ca to the DB", () -> {
        ArgumentCaptor<NamedCertificateAuthority> argumentCaptor = ArgumentCaptor.forClass(NamedCertificateAuthority.class);
        verify(namedCertificateAuthorityDataService, times(1)).save(argumentCaptor.capture());

        NamedCertificateAuthority actual = argumentCaptor.getValue();

        assertThat(actual.getPrivateKey(), equalTo("private_key"));
        assertThat(actual.getCertificate(), equalTo("my_cert"));
      });

      it("creates an audit entry", () -> {
        verify(auditLogService).performWithAuditing(eq("ca_update"), isA(AuditRecordParameters.class), any(Supplier.class));
      });

      describe("overwriting a root ca", () -> {
        beforeEach(() -> {
          uuid = UUID.randomUUID();
          doReturn(
              new NamedCertificateAuthority(uniqueName)
                  .setType("root")
                  .setCertificate("original_cert")
                  .setPrivateKey("original_private_key")
                  .setUpdatedAt(FROZEN_TIME_INSTANT)
                  .setUuid(uuid)
          ).when(namedCertificateAuthorityDataService).find(eq(uniqueName));
        });

        it("returns the new cain the response", () -> {
          String responseJson = "{" + UPDATED_AT_JSON + "," + CA_CREATION_JSON + "}";
          response.andExpect(status().isOk())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
              .andExpect(content().json(responseJson));
        });

        it("stores the new ca in the database under the same name", () -> {
          ArgumentCaptor<NamedCertificateAuthority> argumentCaptor = ArgumentCaptor.forClass(NamedCertificateAuthority.class);
          verify(namedCertificateAuthorityDataService, times(1)).save(argumentCaptor.capture());

          NamedCertificateAuthority actual = argumentCaptor.getValue();

          assertThat(actual.getCertificate(), equalTo("my_cert"));
          assertThat(actual.getPrivateKey(), equalTo("private_key"));
        });

        it("creates an audit record", () -> {
          verify(auditLogService).performWithAuditing(eq("ca_update"), isA(AuditRecordParameters.class), any(Supplier.class));
        });
      });
    });

    describe("errors when setting a CA", () -> {
      it("put with only a certificate returns an error", () -> {
        requestWithError("{\"type\":\"root\",\"root\":{\"certificate\":\"my_certificate\"}}");
      });

      it("put with only private returns an error", () -> {
        requestWithError("{\"type\":\"root\",\"root\":{\"private_key\":\"my_private_key\"}}");
      });

      it("put without keys returns an error", () -> {
        requestWithError("{\"type\":\"root\",\"root\":{}}");
      });

      it("put with empty request returns an error", () -> {
        requestWithError("{\"type\":\"root\"}");
      });

      it("put cert with garbage returns an error", () -> {
        String requestJson = "{\"root\": }";

        RequestBuilder requestBuilder = put(urlPath)
            .content(requestJson)
            .contentType(MediaType.APPLICATION_JSON_UTF8);

        mockMvc.perform(requestBuilder)
            .andExpect(status().isBadRequest());
      });
    });

    describe("getting a ca", () -> {
      beforeEach(() -> {
        uuid = UUID.randomUUID();
        NamedCertificateAuthority storedCa = new NamedCertificateAuthority(uniqueName)
            .setType("root")
            .setCertificate("my-certificate")
            .setPrivateKey("my-priv")
            .setUuid(uuid)
            .setUpdatedAt(FROZEN_TIME_INSTANT);
        doReturn(storedCa).when(namedCertificateAuthorityDataService).find(eq(uniqueName));
        doReturn(storedCa).when(namedCertificateAuthorityDataService).findOneByUuid(eq("my-uuid"));
      });

      describe("by name", () -> {
        beforeEach(() -> {
          response = mockMvc.perform(get(urlPath));
        });

        it("returns the ca", () -> {
          String expectedJson = "{"
              + UPDATED_AT_JSON + "," +
              "\"type\":\"root\"," +
              "\"value\":{" +
                "\"certificate\":\"my-certificate\"," +
                "\"private_key\":\"my-priv\"}," +
              "\"id\":\"" + uuid.toString() +
              "\"}";
          response.andExpect(status().isOk())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
              .andExpect(content().json(expectedJson, true));
        });

        it("persists an audit entry when getting a ca", () -> {
          verify(auditLogService).performWithAuditing(eq("ca_access"), isA(AuditRecordParameters.class), any(Supplier.class));
        });
      });

      describe("by id", () -> {
        beforeEach(() -> {
          MockHttpServletRequestBuilder get = get("/api/v1/ca?id=my-uuid")
              .accept(APPLICATION_JSON);

          response = mockMvc.perform(get);
        });

        it("returns the ca", () -> {
          String expectedJson = "{" +
              UPDATED_AT_JSON + "," +
              "\"type\":\"root\"," +
              "\"value\":{" +
                "\"certificate\":\"my-certificate\"," +
                "\"private_key\":\"my-priv\"" +
              "}," +
              "\"id\":\"" + uuid.toString() + "\"" +
              "}";
          response.andExpect(status().isOk())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
              .andExpect(content().json(expectedJson, true));
        });

        it("persists an audit entry when getting a ca", () -> {
          verify(auditLogService).performWithAuditing(eq("ca_access"), isA(AuditRecordParameters.class), any(Supplier.class));
        });
      });
    });

    it("returns bad request for PUT with invalid type", () -> {
      String uuid = UUID.randomUUID().toString();
      String requestJson = "{\"type\":" + uuid + ",\"value\":{\"certificate\":\"my_cert\",\"private_key\":\"private_key\"}}";

      String invalidTypeJson = "{\"error\": \"The request does not include a valid type. Please validate your input and retry your request.\"}";
      RequestBuilder requestBuilder = put(urlPath)
          .content(requestJson)
          .contentType(MediaType.APPLICATION_JSON_UTF8);

      mockMvc.perform(requestBuilder)
          .andExpect(status().isBadRequest())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
          .andExpect(content().json(invalidTypeJson));
    });

    it("returns bad request for generate POST with invalid type", () -> {
      String requestJson = "{\"type\":\"invalid-type\"}";

      String invalidTypeJson = "{\"error\": \"The request does not include a valid type. Please validate your input and retry your request.\"}";
      RequestBuilder requestBuilder = post(urlPath)
          .content(requestJson)
          .contentType(MediaType.APPLICATION_JSON_UTF8);

      mockMvc.perform(requestBuilder)
          .andExpect(status().isBadRequest())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
          .andExpect(content().json(invalidTypeJson));
    });

    it("get returns 404 when not found", () -> {
      String notFoundJson = "{\"error\": \"CA not found. Please validate your input and retry your request.\"}";

      RequestBuilder requestBuilder = get(urlPath)
          .contentType(MediaType.APPLICATION_JSON_UTF8);

      mockMvc.perform(requestBuilder)
          .andExpect(status().isNotFound())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
          .andExpect(content().json(notFoundJson));
    });
  }

  private void requestWithError(String requestJson) throws Exception {
    String notFoundJson = "{\"error\": \"All keys are required to set a CA. Please validate your input and retry your request.\"}";

    RequestBuilder requestBuilder = put(urlPath)
        .content(requestJson)
        .contentType(MediaType.APPLICATION_JSON_UTF8);

    mockMvc.perform(requestBuilder)
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
        .andExpect(content().json(notFoundJson));
  }
}
