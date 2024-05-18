package com.faforever.commons.replay.body;

import com.google.common.io.LittleEndianDataInputStream;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;

@Slf4j
public class ReplayBodyTokenizer {

  private static final int TOKEN_HEADER_LENGTH = 3;

  @Contract(pure = true)
  public static List<ReplayBodyToken> tokenize(@NotNull LittleEndianDataInputStream dataStream) throws IOException {
    ArrayList<ReplayBodyToken> tokens = new ArrayList<>();
    while (dataStream.available() > 0) {
      int tokenId = dataStream.readUnsignedByte();
      int tokenLength = dataStream.readUnsignedShort();

      byte[] tokenContent = dataStream.readNBytes(tokenLength - TOKEN_HEADER_LENGTH);

      tokens.add(new ReplayBodyToken(ReplayBodyToken.TokenId.values()[tokenId], tokenLength, tokenContent));
    }

    return tokens;
  }
}
