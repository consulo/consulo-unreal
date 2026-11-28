/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.unreal.uasset.reader;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Little-endian reader of the primitives an {@code FArchive} writes: {@code bool} takes four bytes, {@code FString} is an
 * {@code int32} length (negative for UTF-16) followed by the characters and a terminating zero.
 *
 * @author VISTALL
 * @since 2026-11-24
 */
final class UAssetArchive {
    private final ByteBuffer myBuffer;

    private List<String> myNames = List.of();

    UAssetArchive(ByteBuffer buffer) {
        myBuffer = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN);
    }

    void setNames(List<String> names) {
        myNames = names;
    }

    int size() {
        return myBuffer.limit();
    }

    int position() {
        return myBuffer.position();
    }

    void seek(long offset) throws UAssetReadException {
        if (offset < 0 || offset > myBuffer.limit()) {
            throw new UAssetReadException("Offset " + offset + " is outside of the file (" + myBuffer.limit() + " bytes)");
        }
        myBuffer.position((int) offset);
    }

    void skip(int count) throws UAssetReadException {
        seek((long) myBuffer.position() + count);
    }

    int readInt32() throws UAssetReadException {
        try {
            return myBuffer.getInt();
        }
        catch (BufferUnderflowException e) {
            throw unexpectedEnd();
        }
    }

    long readInt64() throws UAssetReadException {
        try {
            return myBuffer.getLong();
        }
        catch (BufferUnderflowException e) {
            throw unexpectedEnd();
        }
    }

    int readUInt16() throws UAssetReadException {
        try {
            return Short.toUnsignedInt(myBuffer.getShort());
        }
        catch (BufferUnderflowException e) {
            throw unexpectedEnd();
        }
    }

    boolean readBool() throws UAssetReadException {
        return readInt32() != 0;
    }

    /**
     * A count that prefixes an array, checked against the remaining bytes so that a corrupt file cannot request a huge
     * allocation.
     */
    int readCount(int minBytesPerElement) throws UAssetReadException {
        int count = readInt32();
        if (count < 0 || (long) count * minBytesPerElement > myBuffer.remaining()) {
            throw new UAssetReadException("Invalid element count " + count + " at offset " + (myBuffer.position() - 4));
        }
        return count;
    }

    byte[] readBytes(int count) throws UAssetReadException {
        if (count < 0 || count > myBuffer.remaining()) {
            throw unexpectedEnd();
        }
        byte[] bytes = new byte[count];
        myBuffer.get(bytes);
        return bytes;
    }

    String readFString() throws UAssetReadException {
        int length = readInt32();
        if (length == 0) {
            return "";
        }
        // a wide string takes two bytes per character
        long byteLength = length < 0 ? -(long) length * 2 : length;
        if (byteLength > myBuffer.remaining()) {
            throw new UAssetReadException("Invalid string length " + length + " at offset " + (myBuffer.position() - 4));
        }
        if (length < 0) {
            byte[] chars = readBytes(-length * 2);
            // drop the terminating zero
            return new String(chars, 0, chars.length - 2, StandardCharsets.UTF_16LE);
        }
        byte[] chars = readBytes(length);
        return new String(chars, 0, chars.length - 1, StandardCharsets.ISO_8859_1);
    }

    /**
     * An {@code FName} as linkers write it: an index into the name table and an instance number, where number {@code N > 0}
     * means the suffix {@code _<N - 1>}.
     */
    String readFName() throws UAssetReadException {
        int index = readInt32();
        int number = readInt32();
        if (index < 0 || index >= myNames.size()) {
            throw new UAssetReadException("Name index " + index + " is outside of the name table (" + myNames.size() + " names)");
        }
        String name = myNames.get(index);
        return number > 0 ? name + "_" + (number - 1) : name;
    }

    private UAssetReadException unexpectedEnd() {
        return new UAssetReadException("Unexpected end of file at offset " + myBuffer.position());
    }
}
