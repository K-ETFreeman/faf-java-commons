package com.faforever.commons.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Type("uniqueId")
@RestrictedVisibility("IsModerator")
public class AccountLink {
  @ToString.Include
  private String id;
  private Player user;
  @ToString.Include
  private LinkedServiceType serviceType;
  private String serviceId;
  @JsonProperty("public")
  private boolean public_;
  private boolean ownership;

  public boolean isPublic() {
    return public_;
  }

  public void setPublic(boolean public_) {
    this.public_ = public_;
  }
}
