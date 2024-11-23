package org.apache.maven.artifact.versioning;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

public class ComparableVersionDeserializer extends StdDeserializer<ComparableVersion> {

  public static final ComparableVersionDeserializer INSTANCE = new ComparableVersionDeserializer();

  private ComparableVersionDeserializer() {
    super(ComparableVersion.class);
  }

  @Override
  public ComparableVersion deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
    return new ComparableVersion(p.readValueAsTree().get("value").traverse().readValueAs(String.class));
  }
}
