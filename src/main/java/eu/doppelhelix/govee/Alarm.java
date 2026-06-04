/* Copyright (c) 2026 Matthias Bläsing, All Rights Reserved
 *
 * The contents of this file is dual-licensed under 2
 * alternative Open Source/Free licenses: LGPL 2.1 or later and
 * Apache License 2.0.
 *
 * You can freely decide which license you want to apply to
 * the project.
 *
 * You may obtain a copy of the LGPL License at:
 *
 * http://www.gnu.org/licenses/licenses.html
 *
 * A copy is also included in the downloadable source code package
 * containing JNA, in file "LGPL2.1".
 *
 * You may obtain a copy of the Apache License at:
 *
 * http://www.apache.org/licenses/
 */

package eu.doppelhelix.govee;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public record Alarm(boolean active, double lower, double upper) {

    public static Alarm fromData(byte[] data) {
        boolean active = data[2] > 0;
        int lower = ByteBuffer.wrap(data, 3, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
        int upper = ByteBuffer.wrap(data, 5, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
        return new Alarm(active, lower / 100d, upper / 100d);
    }

    public byte[] encode() {
        byte[] result = new byte[5];
        ByteBuffer bb = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
        result[0] = (byte) (active ? 1 : 0);
        bb.putShort(1, (short) (lower * 100));
        bb.putShort(3, (short) (upper * 100));
        return result;
    }
}
