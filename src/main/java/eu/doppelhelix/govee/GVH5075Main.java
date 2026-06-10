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

/**
 * Java Implementierung auf Basis von_
 * - https://github.com/Heckie75/govee-h5075-thermo-hygrometer/tree/main
 * - https://github.com/NHaag87/govee-api/blob/main/API_documentation/H5105_protocol.md
 * - https://github.com/wcbonner/GoveeBTTempLogger/tree/master
 */

package eu.doppelhelix.govee;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.bluez.Adapter1;
import org.bluez.Device1;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.ObjectManager;
import org.freedesktop.dbus.messages.MethodCall;
import org.freedesktop.dbus.types.UInt16;
import org.freedesktop.dbus.types.Variant;

public class GVH5075Main {

    private static final System.Logger LOG = System.getLogger(GVH5075Main.class.getName());

    private static final String BLUEZ_BUSNAME = "org.bluez";


    public static void main(String[] args) throws Exception {

        LoggingConfiguration.init();
//        LoggingConfiguration.enableDebugLogging();

        boolean scanOnly = false;

        MethodCall.setDefaultTimeout(60_000);
        try (DBusConnection dbusConn = DBusConnectionBuilder.forSystemBus().build()) {
            DBusPath goveeDevice = findGoveeDevices(dbusConn, scanOnly);

            if (!scanOnly) {
                try (GVH5075Client client = new GVH5075Client(dbusConn, goveeDevice)) {
//                client.setAlarmHumidity(new Alarm(false, 20, 40));
//                client.setAlarmHumidity(new Alarm(false, -5.5, 23.5));
//                client.setOffsetHumidity(0);
//                client.setOffsetTemperature(0);
                    dumpInfo(client);
                    dumpCurrentMeasurements(client);
//                dumpHistoricMeasurements(client);
                }
            }

        }
    }

    private static DBusPath findGoveeDevices(final DBusConnection dbusConn, boolean scanOnly) throws DBusException, InterruptedException {

        ObjectManager objectManager = dbusConn.getRemoteObject(BLUEZ_BUSNAME, "/", ObjectManager.class);

        List<Adapter1> bluetoothAdapter = new ArrayList<>();
        for (Entry<DBusPath, Map<String, Map<String, Variant<?>>>> e : objectManager.GetManagedObjects().entrySet()) {
            if (e.getValue().containsKey(Adapter1.class.getName())) {
                bluetoothAdapter.add(dbusConn.getRemoteObject(BLUEZ_BUSNAME, e.getKey(), Adapter1.class));
            }
        }

        bluetoothAdapter.forEach(ad -> {
            LOG.log(Level.DEBUG, () -> "Started discovery for adapter: " + ad.getName() + " / " + ad.getAddress());
            ad.StartDiscovery();
        });

        Map<String, Instant> lastSeenValues = new HashMap<>();

        while (bluetoothAdapter.stream().allMatch(ad -> ad.isDiscovering())) {
            Thread.sleep(100);
            for (Entry<DBusPath, Map<String, Map<String, Variant<?>>>> e : objectManager.GetManagedObjects().entrySet()) {
                if (e.getValue().containsKey(Device1.class.getName())) {
                    Map<String, Variant<?>> device1Data = e.getValue().get(Device1.class.getName());
                    Variant<?> nameVariant = device1Data.get("Name");
                    Variant<?> addressVariant = device1Data.get("Address");
//                    Variant<?> address = e.getValue().get(Device1.class.getName()).get("Address");

                    if (nameVariant != null
                            && addressVariant != null
                            && nameVariant.getValue() instanceof String name
                            && addressVariant.getValue() instanceof String address
                            && name.startsWith("GVH5075")) {

                        if(! scanOnly) {
                            return e.getKey();
                        }

                        Instant lastSeen = lastSeenValues.get(address);
                        if (lastSeen == null || Instant.now().minus(10, ChronoUnit.SECONDS).isAfter(lastSeen)) {
                            Short rssi = Optional
                                    .ofNullable(device1Data.get("RSSI"))
                                    .map(variant -> (Short) variant.getValue())
                                    .orElse(null);

                            GVH5075Client.ScanningData scanningData = null;

                            if(device1Data.get("ManufacturerData") instanceof Variant mdVariant
                                    && mdVariant.getValue() instanceof Map md
                                    && md.get(new UInt16(GVH5075Client.SCAN_COMPANY_IDENTIFIER)) instanceof Variant goveeDataVariant
                                    && goveeDataVariant.getValue() instanceof List listData) {
                                scanningData = GVH5075Client.decodeScanningData(listData);
                            }

                            System.out.printf("%s\t%s\t%s\t%s°C\t%s%%\t%s%%%n",
                                    name,
                                    address,
                                    rssi == null ? "-" : rssi,
                                    scanningData != null ? scanningData.temperature() : "-",
                                    scanningData != null ? scanningData.humidity() : "-",
                                    scanningData != null ? scanningData.batteryPercentage() : "-"
                            );

                            lastSeenValues.put(address, Instant.now());
                        }
                    }
                }
            }
        }

        return null;
    }

    private static void dumpCurrentMeasurements(GVH5075Client client) throws ExecutionException, InterruptedException {
        for(int i = 0; i < 5; i++) {
            try {
                Measurement m = client.getCurrentMeasurement();
                System.out.printf("Temperatur: %.1f °C, Luftfeuchtigkeit: %.0f %%, Batterie: %d %%%n", m.temperature(), m.humidity(), m.battery());
                Thread.sleep(5000);
            } catch (TimeoutException ex) {
            }
        }
    }

    private static void dumpHistoricMeasurements(GVH5075Client client) throws ExecutionException, InterruptedException, TimeoutException {
        
        client.getHistoricMeasurements(20 * 24 * 60, 0).forEach(System.out::println);
    }

    private static void dumpInfo(GVH5075Client client) throws ExecutionException, InterruptedException, TimeoutException {
        System.out.printf("Name:              %s%n", client.getName());
        System.out.printf("Alarm Humidity:    %s%n", client.getAlarmHumidity());
        System.out.printf("Alarm Temperatur:  %s%n", client.getAlarmTemperature());
        System.out.printf("Offset Humidity:   %.2f%n", client.getOffsetHumitidy());
        System.out.printf("Offset Temperatur  %.2f%n", client.getOffsetTemperature());
        System.out.printf("Hardware-Revision: %s%n", client.getHardwareRevision());
        System.out.printf("Firmware-Revision: %s%n", client.getFirmwareRevision());
        System.out.printf("Battery:           %d %%%n", client.getBatteryLevel());
    }


    record DeviceAddress(Device1 device, DBusPath path) {

    }

}
