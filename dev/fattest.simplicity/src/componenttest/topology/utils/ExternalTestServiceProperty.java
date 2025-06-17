/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.topology.utils;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.util.Arrays;
import java.util.Objects;

import org.apache.commons.codec.binary.Base64;

/**
 * This is a map of key value pairs that represent an external test service.
 * Includes additional methods to decode and decrypt values.
 *
 * Key - holds service property key
 * Value - holds the raw value as it was returned from consul
 */
class ExternalTestServiceProperty {

    private final String key;
    private final String rawValue;

    private byte[] decodedByteArray = null;
    private String decodedValue = null;
    private String decryptedValue = null;

    ExternalTestServiceProperty(String key, String rawValue) {
        this.key = Objects.requireNonNull(key);
        this.rawValue = Objects.requireNonNull(rawValue);
    }

    String getKey() {
        return key;
    }

    String getRawValue() {
        return rawValue;
    }

    byte[] getDecodedByteArray() {
        if (Objects.nonNull(decodedByteArray)) {
            return decodedByteArray;
        }

        // Was not encoded, use rawValue as is
        if (!Base64.isBase64(rawValue)) {
            return decodedByteArray = rawValue.getBytes();
        }

        // Decode to byte array
        return decodedByteArray = Base64.decodeBase64(rawValue);
    }

    String getDecodedValue() {
        if (Objects.nonNull(decodedValue)) {
            return decodedValue;
        }

        // Was not encoded, use rawValue as is
        if (!Base64.isBase64(rawValue)) {
            return decodedValue = rawValue;
        }

        // Decode value to String
        try {
            return decodedValue = Charset.forName("UTF-8")
                            .newDecoder()
                            .onMalformedInput(CodingErrorAction.REPORT)
                            .onUnmappableCharacter(CodingErrorAction.REPORT)
                            .decode(ByteBuffer.wrap(getDecodedByteArray()))
                            .toString();
        } catch (CharacterCodingException e) {
            throw new RuntimeException("Could not decode value", e);
        }
    }

    String getDecryptedValue() {
        if (Objects.nonNull(decryptedValue)) {
            return decryptedValue;
        }

        // Decrypter does it's own check to verify whether or not the value needs to be decrypted.
        try {
            return decryptedValue = ExternalTestServiceDecrypter.decrypt(getDecodedValue());
        } catch (Exception e) {
            throw new RuntimeException("Could not decrypt value", e);
        }
    }

    @Override
    public String toString() {
        return "ExternalTestServiceProperty"
               + " [key=" + key
               + ", rawValue=" + rawValue
               + ", decodedByteArray=" + Arrays.toString(decodedByteArray)
               + ", decodedValue=" + decodedValue
               + ", decryptedValue=" + (decryptedValue == null ? null : "***obscured***") + "]";
    }

}
