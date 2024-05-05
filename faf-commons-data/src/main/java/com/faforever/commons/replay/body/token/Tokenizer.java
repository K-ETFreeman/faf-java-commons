package com.faforever.commons.replay.body.token;

import com.google.common.io.LittleEndianDataInputStream;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.*;

@Slf4j
public class Tokenizer {

  private static final int TOKEN_HEADER_LENGTH = 3;

  public static List<Token> tokenize(LittleEndianDataInputStream dataStream) throws IOException {
    ArrayList<Token> tokens = new ArrayList<>();
    while (dataStream.available() > 0) {
      int tokenId = dataStream.readUnsignedByte();
      int tokenLength = dataStream.readUnsignedShort();

      byte[] tokenContent = dataStream.readNBytes(tokenLength - TOKEN_HEADER_LENGTH);

      tokens.add(new Token(Token.TokenId.values()[tokenId], tokenLength, tokenContent));
    }

    return tokens;
  }
}
