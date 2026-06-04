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

public record Measurement(double temperature, double humidity, int battery) {

    public static Measurement fromData(byte[] data) {
        int temp = ByteBuffer.wrap(data, 2, 2).getShort();
        int humidity = ByteBuffer.wrap(data, 4, 2).getShort();
        int battery = data[6];
        return new Measurement(temp / 100d, humidity / 100d, battery);
    }
}
