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

import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoggingConfiguration {

    private static final Set<Logger> coreLoggers = Set.of(
            Logger.getLogger(ValueListener.class.getName()),
            Logger.getLogger(GVH5075Main.class.getName()),
            Logger.getLogger(GVH5075Client.class.getName())
    );

    public static void init() {
        for (Handler h : Logger.getLogger("").getHandlers()) {
            h.setLevel(java.util.logging.Level.FINER);
        }
    }

    public static void enableDebugLogging() {
        coreLoggers.forEach(l -> l.setLevel(Level.FINER));
    }
}
