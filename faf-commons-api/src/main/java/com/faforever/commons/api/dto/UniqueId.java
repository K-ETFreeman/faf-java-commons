package com.faforever.commons.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Type("uniqueId")
@RestrictedVisibility("IsModerator")
public class UniqueId extends AbstractEntity<UniqueId> {

  @EqualsAndHashCode.Include
  @ToString.Include
  private String hash;
  private String uuid;
  private String memorySerialNumber;
  private String deviceId;
  private String manufacturer;
  private String name;
  private String processorId;
  @JsonProperty("SMBIOSBIOSVersion")
  private String SMBIOSBIOSVersion;
  private String serialNumber;
  private String volumeSerialNumber;
}
