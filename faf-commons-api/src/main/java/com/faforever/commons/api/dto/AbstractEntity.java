package com.faforever.commons.api.dto;

import com.faforever.commons.api.elide.ElideEntity;
import com.github.jasminb.jsonapi.annotations.Id;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.OffsetDateTime;

@SuppressWarnings("unchecked")
@Data
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class AbstractEntity<T extends AbstractEntity<T>> implements ElideEntity {
  @Id
  @ToString.Include
  @EqualsAndHashCode.Include
  protected String id;
  protected OffsetDateTime createTime;
  protected OffsetDateTime updateTime;

  public T setId(String id) {
    this.id = id;
    return (T) this;
  }

  public T setCreateTime(OffsetDateTime createTime) {
    this.createTime = createTime;
    return (T) this;
  }

  public T setUpdateTime(OffsetDateTime updateTime) {
    this.updateTime = updateTime;
    return (T) this;
  }

  /**
   * Supplement method for @EqualsAndHashCode
   * overriding the default lombok implementation
   */
  protected boolean canEqual(Object other) {
    return other instanceof AbstractEntity && this.getClass() == other.getClass();
  }
}
