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

import java.util.List;


public class Util {
    static int readIntHighbitSign(byte[] data, int offset, int length) {
        int result = 0;
        boolean invert = false;
        for (int i = 0; i < length; i++) {
            int value = 0xFF & data[offset + i];
            if (i == 0 && (value & 0x80) > 0) {
                invert = true;
                value ^= 0x80;
            }
            result <<= 8;
            result |= value;
        }
        if (invert) {
            result = result * -1;
        }
        return result;
    }

    static byte[] toByteArray(Object inputObj) {
        if (inputObj instanceof List inputList) {
            if(inputList.isEmpty()) {
                return new byte[0];
            } else if (inputList.get(0) instanceof Byte) {
                List<Byte> input = (List<Byte>) inputList;
                byte[] result = new byte[input.size()];
                for (int i = 0; i < input.size(); i++) {
                    result[i] = input.get(i);
                }
                return result;
            } else {
                throw new IllegalArgumentException("Input list does not contain bytes: " + inputList);
            }
        } else {
            throw new IllegalArgumentException("Input is not a list: " + inputObj);
        }
    }
}
