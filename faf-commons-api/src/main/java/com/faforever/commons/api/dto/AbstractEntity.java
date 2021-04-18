package com.faforever.commons.api.dto;

import com.faforever.commons.api.elide.ElideEntity;
import com.github.jasminb.jsonapi.annotations.Id;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.OffsetDateTime;

@Data
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class AbstractEntity implements ElideEntity {
  @Id
  @ToString.Include
  @EqualsAndHashCode.Include
  protected String id;
  protected OffsetDateTime createTime;
  protected OffsetDateTime updateTime;

  /**
   * Supplement method for @EqualsAndHashCode
   * overriding the default lombok implementation
   */
  protected boolean canEqual(Object other) {
    return other instanceof AbstractEntity && this.getClass() == other.getClass();
  }
}
