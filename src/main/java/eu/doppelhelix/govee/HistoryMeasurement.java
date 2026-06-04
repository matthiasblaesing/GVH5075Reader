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
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static eu.doppelhelix.govee.Util.readIntHighbitSign;

public record HistoryMeasurement(OffsetDateTime timestamp, int minutesBack, double temperature, double humility) {

    public static List<HistoryMeasurement> fromData(OffsetDateTime referenceTimestamp, byte[] data) {
        List<HistoryMeasurement> result = new ArrayList<>();
        int minutesBack = ByteBuffer.wrap(data, 0, 2).getShort();
        for (int i = 0; ((i + 1) * 3 + 2) <= data.length; i++) {
            int offset = i * 3 + 2;
            if (data[offset] == -1 && data[offset + 1] == -1 && data[offset + 2] == -1) {
                continue;
            }
            int v1 = readIntHighbitSign(data, offset, 3);
            int t1 = v1 / 1000;
            int h1 = Math.abs(v1 % 1000);
            result.add(new HistoryMeasurement(referenceTimestamp.minusMinutes(minutesBack - i), minutesBack - i, t1 / 10d, h1 / 10d));
        }
        return result;
    }
}
