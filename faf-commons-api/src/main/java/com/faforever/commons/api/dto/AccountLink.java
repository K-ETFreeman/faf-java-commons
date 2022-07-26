package com.faforever.commons.api.dto;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.github.jasminb.jsonapi.annotations.Id;
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
  @Id
  @ToString.Include
  private String id;
  private Player user;
  @ToString.Include
  private LinkedServiceType serviceType;
  private String serviceId;
  private boolean public_;
  private boolean ownership;

  @JsonGetter("public")
  public boolean isPublic() {
    return public_;
  }

  @JsonSetter("public")
  public void setPublic(boolean public_) {
    this.public_ = public_;
  }
}
