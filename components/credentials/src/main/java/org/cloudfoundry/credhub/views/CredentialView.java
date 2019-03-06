package org.cloudfoundry.credhub.views;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.cloudfoundry.credhub.credential.CredentialValue;
import org.cloudfoundry.credhub.domain.CertificateCredentialVersion;
import org.cloudfoundry.credhub.domain.CredentialVersion;
import org.cloudfoundry.credhub.domain.JsonCredentialVersion;
import org.cloudfoundry.credhub.domain.PasswordCredentialVersion;
import org.cloudfoundry.credhub.domain.RsaCredentialVersion;
import org.cloudfoundry.credhub.domain.SshCredentialVersion;
import org.cloudfoundry.credhub.domain.UserCredentialVersion;
import org.cloudfoundry.credhub.domain.ValueCredentialVersion;

public class CredentialView {

  private Instant versionCreatedAt;
  private UUID uuid;
  private String name;
  private String type;
  private CredentialValue value;

  public CredentialView() {
    super(); /* Jackson */
  }

  public CredentialView(
    final Instant versionCreatedAt, final UUID uuid, final String name, final String type, final CredentialValue value) {
    super();
    this.versionCreatedAt = versionCreatedAt;
    this.uuid = uuid;
    this.name = name;
    this.type = type;
    this.value = value;
  }

  public static CredentialView fromEntity(final CredentialVersion credentialVersion) {
    final CredentialView result;
    if (ValueCredentialVersion.class.isInstance(credentialVersion)) {
      result = new ValueView((ValueCredentialVersion) credentialVersion);
    } else if (PasswordCredentialVersion.class.isInstance(credentialVersion)) {
      result = new PasswordView((PasswordCredentialVersion) credentialVersion);
    } else if (CertificateCredentialVersion.class.isInstance(credentialVersion)) {
      result = new CertificateView((CertificateCredentialVersion) credentialVersion);
    } else if (SshCredentialVersion.class.isInstance(credentialVersion)) {
      result = new SshView((SshCredentialVersion) credentialVersion);
    } else if (RsaCredentialVersion.class.isInstance(credentialVersion)) {
      result = new RsaView((RsaCredentialVersion) credentialVersion);
    } else if (JsonCredentialVersion.class.isInstance(credentialVersion)) {
      result = new JsonView((JsonCredentialVersion) credentialVersion);
    } else if (UserCredentialVersion.class.isInstance(credentialVersion)) {
      result = new UserView((UserCredentialVersion) credentialVersion);
    } else {
      throw new IllegalArgumentException();
    }
    return result;
  }

  @JsonProperty("version_created_at")
  public Instant getVersionCreatedAt() {
    return versionCreatedAt;
  }

  @JsonProperty
  public String getType() {
    return type;
  }

  @JsonProperty("id")
  public String getUuid() {
    return uuid == null ? "" : uuid.toString();
  }

  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("value")
  public CredentialValue getValue() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    CredentialView that = (CredentialView) o;

    return new EqualsBuilder()
      .append(versionCreatedAt, that.versionCreatedAt)
      .append(uuid, that.uuid)
      .append(name, that.name)
      .append(type, that.type)
      .append(value, that.value)
      .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
      .append(versionCreatedAt)
      .append(uuid)
      .append(name)
      .append(type)
      .append(value)
      .toHashCode();
  }
}
