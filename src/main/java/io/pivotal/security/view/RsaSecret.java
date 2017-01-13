package io.pivotal.security.view;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.pivotal.security.entity.NamedRsaSecret;

import java.time.Instant;
import java.util.UUID;

public class RsaSecret extends Secret {
  @JsonProperty("value")
  private RsaBody rsaBody;

  public RsaSecret(Instant versionCreatedAt, UUID uuid, String name, String publicKey, String privateKey) {
    super(versionCreatedAt, uuid, name);
    setRsaBody(new RsaBody(publicKey, privateKey));
  }

  public RsaSecret(NamedRsaSecret namedRsaSecret) {
    this(
        namedRsaSecret.getVersionCreatedAt(),
        namedRsaSecret.getUuid(),
        namedRsaSecret.getName(),
        namedRsaSecret.getPublicKey(),
        namedRsaSecret.getPrivateKey()
    );
  }

  @Override
  public String getType() {
    return NamedRsaSecret.SECRET_TYPE;
  }

  public RsaBody getRsaBody() {
    return rsaBody;
  }

  public RsaSecret setRsaBody(RsaBody rsaBody) {
    this.rsaBody = rsaBody;
    return this;
  }
}
