package com.faforever.commons.replay.body;

import com.faforever.commons.replay.shared.LoadUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ReplayBodyTokenizer {

  private static final int TOKEN_HEADER_LENGTH = 3;

  @Contract(pure = true)
  public static List<ReplayBodyToken> tokenize(ByteBuffer buffer) {
    ArrayList<ReplayBodyToken> tokens = new ArrayList<>();
    final int length = buffer.limit();
    while (buffer.position() < length) {
      final int tokenId = LoadUtils.getUnsignedByte(buffer);
      final int tokenLength = buffer.getShort() - TOKEN_HEADER_LENGTH;

      buffer.position(buffer.position() + tokenLength);
      tokens.add(new ReplayBodyToken(ReplayBodyToken.TokenId.values()[tokenId], buffer.position()));
    }

    return tokens;
  }
}
