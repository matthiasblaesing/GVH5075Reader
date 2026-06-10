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

import eu.doppelhelix.govee.GVH5075Client.ScanningData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GVH5075ClientTest {

    @Test
    public void testDecodeScanningData() {
        byte[] testData = new byte[]{0x00, 0x03, 0x7d, 0x48, 0x60, 0x00};
        ScanningData sd = GVH5075Client.ScanningData.fromData(testData);
        assertEquals(22.8, sd.temperature());
        assertEquals(68.0, sd.humidity());
        assertEquals(96, sd.batteryPercentage());

    }

}
