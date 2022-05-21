/*
 * Copyright 2022 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite;

import lombok.Value;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.ipc.http.HttpSender;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Value
public class Checksum {
    private static final byte[] HEX_ARRAY = "0123456789abcdef".getBytes(StandardCharsets.US_ASCII);

    String algorithm;
    byte[] value;

    public String getHexValue() {
        byte[] hexChars = new byte[value.length * 2];
        for (int j = 0; j < value.length; j++) {
            int v = value[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }

    public static Checksum fromHex(String algorithm, String hex) {
        if(hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex string must contain a set of hex pairs (length divisible by 2).");
        }

        byte[] value = new byte[hex.length() / 2];
        for (int i = 0; i < value.length; i++) {
            int index = i * 2;
            int n = Integer.parseInt(hex.substring(index, index + 2), 16);
            value[i] = (byte) n;
        }

        return new Checksum(algorithm, value);
    }

    public static SourceFile md5(SourceFile sourceFile) {
        return checksum(sourceFile, "MD5");
    }

    public static SourceFile sha256(SourceFile sourceFile) {
        return checksum(sourceFile, "SHA-256");
    }

    public static SourceFile checksum(SourceFile sourceFile, @Nullable String algorithm) {
        if(algorithm == null) {
            return sourceFile;
        }

        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            try (InputStream is = Files.newInputStream(sourceFile.getSourcePath());
                 DigestInputStream dis = new DigestInputStream(is, md)) {
                //noinspection StatementWithEmptyBody
                while (dis.read() != -1) {
                    // read decorated stream to EOF
                }
            }
            return sourceFile.withChecksum(new Checksum(algorithm, md.digest()));
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
