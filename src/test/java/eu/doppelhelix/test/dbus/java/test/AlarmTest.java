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

package eu.doppelhelix.test.dbus.java.test;

import eu.doppelhelix.govee.Alarm;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AlarmTest {

    @Test
    public void testParsing() {
        String testReplyString = "aa 04 00 44 fd c4 09 00 00 00 00 00 00 00 00 00 00 00 00 da";
        byte[] testReply = HexFormat.ofDelimiter(" ").parseHex(testReplyString);
        Alarm alarm = Alarm.fromData(testReply);
        assertFalse(alarm.active());
        assertEquals(alarm.lower(), -7.0);
        assertEquals(alarm.upper(), 25.0);
    }

    @Test
    public void testEncoding() {
        Alarm alarm = new Alarm(true, -7.0, 25.0);
        byte[] result = alarm.encode();
        assertEquals("01 44 fd c4 09", HexFormat.ofDelimiter(" ").formatHex(result));
    }

}
