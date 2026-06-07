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

import java.lang.System.Logger.Level;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.bluez.Device1;
import org.bluez.GattCharacteristic1;
import org.bluez.GattService1;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.ObjectManager;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.Variant;

import static eu.doppelhelix.govee.Util.toByteArray;

public class GVH5075Client implements AutoCloseable {

    private static final System.Logger LOG = System.getLogger(GVH5075Client.class.getName());

    private static final String BLUEZ_BUSNAME = "org.bluez";
    private static final String UUID_SVC_GENERIC_ACCESS = "00001800-0000-1000-8000-00805f9b34fb";
    private static final String UUID_SVC_GOVEE_DATA = "494e5445-4c4c-495f-524f-434b535f4857";
    private static final String UUID_NAME = "00002a00-0000-1000-8000-00805f9b34fb";
    private static final String UUID_DEVICE = "494e5445-4c4c-495f-524f-434b535f2011";
    private static final String UUID_COMMAND = "494e5445-4c4c-495f-524f-434b535f2012";
    private static final String UUID_DATA = "494e5445-4c4c-495f-524f-434b535f2013";
    private static final String UUID_SVC_GOVEE_AUTH = "00010203-0405-0607-0809-0a0b0c0d1910";
    private static final String UUID_AUTH_NOTIFY = "00010203-0405-0607-0809-0a0b0c0d2b10";
    private static final String UUID_AUTH_WRITE = "00010203-0405-0607-0809-0a0b0c0d2b11";
    private static final String UUID_AUTH_CONFIG = "00010203-0405-0607-0809-0a0b0c0d2b12";
    private static final CharAddr ADDR_NAME = new CharAddr(UUID_SVC_GENERIC_ACCESS, UUID_NAME);
    private static final CharAddr ADDR_DEVICE = new CharAddr(UUID_SVC_GOVEE_DATA, UUID_DEVICE);
    private static final CharAddr ADDR_COMMAND = new CharAddr(UUID_SVC_GOVEE_DATA, UUID_COMMAND);
    private static final CharAddr ADDR_DATA = new CharAddr(UUID_SVC_GOVEE_DATA, UUID_DATA);
    private static final CharAddr ADDR_AUTH_NOTIFY = new CharAddr(UUID_SVC_GOVEE_AUTH, UUID_AUTH_NOTIFY);
    private static final CharAddr ADDR_AUTH_WRITE = new CharAddr(UUID_SVC_GOVEE_AUTH, UUID_AUTH_WRITE);
    private static final CharAddr ADDR_AUTH_CONFIG = new CharAddr(UUID_SVC_GOVEE_AUTH, UUID_AUTH_CONFIG);
    private static final Set<CharAddr> REQUIRED_CHARACTERISTICS = Set.of(
            ADDR_NAME,
            ADDR_DEVICE, ADDR_COMMAND, ADDR_DATA /*,
            ADDR_AUTH_NOTIFY, ADDR_AUTH_WRITE, ADDR_AUTH_CONFIG - the authentication characteristics are required on new Endpoints */
    );

    private static final byte[] REQUEST_CURRENT_MEASUREMENT = new byte[]{(byte) 0xaa, (byte) 0x01};
    private static final byte[] REQUEST_HISTORIC_MEASUREMENTS = new byte[]{(byte) 0x33, (byte) 0x01};
    private static final byte[] REQUEST_HISTORIC_MEASUREMENTS_DONE = new byte[]{(byte) 0xee, (byte) 0x01};
    private static final byte[] REQUEST_ALARM_HUMIDTY = new byte[]{(byte) 0xaa, (byte) 0x03};
    private static final byte[] REQUEST_ALARM_TEMPERATURE = new byte[]{(byte) 0xaa, (byte) 0x04};
    private static final byte[] REQUEST_OFFSET_HUMIDTY = new byte[]{(byte) 0xaa, (byte) 0x06};
    private static final byte[] REQUEST_OFFSET_TEMPERATURE = new byte[]{(byte) 0xaa, (byte) 0x07};
    private static final byte[] REQUEST_BATTERY_LEVEL = new byte[]{(byte) 0xaa, (byte) 0x08};
    private static final byte[] REQUEST_MAC_AND_SERIAL = new byte[]{(byte) 0xaa, (byte) 0x0c};
    private static final byte[] REQUEST_HARDWARE = new byte[]{(byte) 0xaa, (byte) 0x0d};
    private static final byte[] REQUEST_FIRMWARE = new byte[]{(byte) 0xaa, (byte) 0x0e};
    private static final byte[] REQUEST_MAC_ADDRESS = new byte[]{(byte) 0xaa, (byte) 0x0f};
    private static final byte[] SEND_ALARM_HUMIDTY = new byte[]{(byte) 0x33, (byte) 0x03};
    private static final byte[] SEND_ALARM_TEMPERATURE = new byte[]{(byte) 0x33, (byte) 0x04};
    private static final byte[] SEND_OFFSET_HUMIDTY = new byte[]{(byte) 0x33, (byte) 0x06};
    private static final byte[] SEND_OFFSET_TEMPERATURE = new byte[]{(byte) 0x33, (byte) 0x07};

    // the PSK is used for the initial handshake, which results in a session
    // key that is ued for the further communication
    private static final byte[] PSK = "MakingLifeSmarte".getBytes(StandardCharsets.US_ASCII);

    private final DBusConnection connection;
    private final Device1 dev;
    private final GattCharacteristic1 deviceCharacteristic;
    private final GattCharacteristic1 dataCharacteristic;
    private final GattCharacteristic1 commandCharacteristic;
    private final GattCharacteristic1 nameCharacteristic;
    private final ValueListener deviceValueListener = new ValueListener("DEVICES");
    private final AutoCloseable deviceValueListenerRegistration;
    private final ValueListener dataValueListener = new ValueListener("DATA");
    private final AutoCloseable dataValueListenerRegistration;
    private final ValueListener commandValueListener = new ValueListener("COMMAND");
    private final AutoCloseable commandValueListenerRegistration;
    private final EncryptionHelper connectionEncryption;

    public GVH5075Client(DBusConnection connection, DBusPath path) throws DBusException, InterruptedException, Exception {
        this.connection = connection;

        this.dev = connection.getRemoteObject(BLUEZ_BUSNAME, path, Device1.class);

        dev.Connect();

        while (!dev.isServicesResolved()) {
            Thread.sleep(100);
        }

        ObjectManager objectManager = connection.getRemoteObject(BLUEZ_BUSNAME, "/", ObjectManager.class);

        Map<CharAddr, GattCharacteristic1> deviceCharacteristics = new HashMap<>();

        Map<DBusPath, Map<String, Map<String, Variant<?>>>> objects = objectManager.GetManagedObjects();
        for (Map.Entry<DBusPath, Map<String, Map<String, Variant<?>>>> e : objects.entrySet()) {
            if (e.getValue().containsKey(GattCharacteristic1.class.getName())) {
                Map<String, Variant<?>> characteristicsData = e.getValue().get(GattCharacteristic1.class.getName());
                DBusPath servicePath = (DBusPath) characteristicsData.get("Service").getValue();
                Map<String, Map<String, Variant<?>>> serviceProperties = objects
                        .entrySet()
                        .stream()
                        .filter(f -> servicePath.getPath().equals(f.getKey().getPath()))
                        .findFirst()
                        .map(f -> f.getValue())
                        .orElse(null);
                String serviceUUID = (String) serviceProperties.get(GattService1.class.getName()).get("UUID").getValue();
                DBusPath devicePath = (DBusPath) serviceProperties.get(GattService1.class.getName()).get("Device").getValue();
                if (path.getPath().equals(devicePath.getPath())) {
                    GattCharacteristic1 char1 = connection.getRemoteObject(BLUEZ_BUSNAME, e.getKey(), GattCharacteristic1.class);
                    deviceCharacteristics.put(
                            new CharAddr(serviceUUID, char1.getUUID()),
                            char1
                    );
                }
            }
        }

        deviceCharacteristic = deviceCharacteristics.get(ADDR_DEVICE);
        dataCharacteristic = deviceCharacteristics.get(ADDR_DATA);
        commandCharacteristic = deviceCharacteristics.get(ADDR_COMMAND);
        nameCharacteristic = deviceCharacteristics.get(ADDR_NAME);

        deviceValueListenerRegistration = connection.addSigHandler(Properties.PropertiesChanged.class, deviceCharacteristic, deviceValueListener);
        dataValueListenerRegistration = connection.addSigHandler(Properties.PropertiesChanged.class, dataCharacteristic, dataValueListener);
        commandValueListenerRegistration = connection.addSigHandler(Properties.PropertiesChanged.class, commandCharacteristic, commandValueListener);

        deviceCharacteristic.StartNotify();
        dataCharacteristic.StartNotify();
        commandCharacteristic.StartNotify();

        if (deviceCharacteristics.containsKey(ADDR_AUTH_NOTIFY) && deviceCharacteristics.containsKey(ADDR_AUTH_WRITE)) {
            GattCharacteristic1 authNotifyCharacteristic = deviceCharacteristics.get(ADDR_AUTH_NOTIFY);
            GattCharacteristic1 authWriteCharacteristic = deviceCharacteristics.get(ADDR_AUTH_WRITE);

            connectionEncryption = encryptionHandshake(connection, authNotifyCharacteristic, authWriteCharacteristic);
        } else {
            connectionEncryption = null;
        }

    }

    private EncryptionHelper encryptionHandshake(DBusConnection connection1, GattCharacteristic1 authNotifyCharacteristic, GattCharacteristic1 authWriteCharacteristic) throws Exception {
        ValueListener authNotifyListener = new ValueListener("AUTH NOTIFY");
        try (AutoCloseable reg = connection1.addSigHandler(Properties.PropertiesChanged.class, authNotifyCharacteristic, authNotifyListener)) {
            authNotifyCharacteristic.StartNotify();

            EncryptionHelper pskEncryption = new EncryptionHelper(PSK);

            byte[] tx1 = createTxPacket((byte) 1);
            byte[] encryptedTx1 = pskEncryption.encrypt(tx1);

            byte[] tx2 = createTxPacket((byte) 2);
            byte[] encryptedTx2 = pskEncryption.encrypt(tx2);

            LOG.log(Level.DEBUG, () -> "Sending TX1: " + HexFormat.ofDelimiter(" ").formatHex(tx1));
            Future<Object> result = authNotifyListener.getNextValue();
            authWriteCharacteristic.WriteValue(encryptedTx1, Map.of("type", new Variant<>("command")));
            byte[] rx1 = pskEncryption.decrypt(toByteArray(result.get()));
            LOG.log(Level.DEBUG, () -> "Received as RX1: " + HexFormat.ofDelimiter(" ").formatHex(rx1));
            if(rx1[0] != ((byte) 0xE7) || rx1[1] != ((byte) 0x01)) {
                throw new IllegalStateException("Encryption Handshake failed");
            }
            byte[] sessionKey = Arrays.copyOfRange(rx1, 2, 18);
            LOG.log(Level.DEBUG, () -> "Sending TX2: " + HexFormat.ofDelimiter(" ").formatHex(tx2));
            result = authNotifyListener.getNextValue();
            authWriteCharacteristic.WriteValue(encryptedTx2, Map.of("type", new Variant<>("command")));
            byte[] rx2 = pskEncryption.decrypt(toByteArray(result.get()));
            LOG.log(Level.DEBUG, () -> "Received as RX1: " + HexFormat.ofDelimiter(" ").formatHex(rx2));
            if (rx2[0] != ((byte) 0xE7) || rx2[1] != ((byte) 0x02)) {
                throw new IllegalStateException("Encryption Handshake failed (2)");
            }
            return new EncryptionHelper(sessionKey);
        } finally {
            authNotifyCharacteristic.StopNotify();
        }
    }

    public byte[] createTxPacket(byte phase) {
        byte[] tx = new byte[20];
        tx[0] = (byte) 0xE7;
        tx[1] = phase;
        for (int i = 0; i < (tx.length - 1); i++) {
            tx[19] ^= tx[i];
        }
        return tx;
    }


    public String getName() throws TimeoutException, InterruptedException, ExecutionException {
        return new String(nameCharacteristic.ReadValue(Map.of()));
    }

    public int getBatteryLevel() throws TimeoutException, InterruptedException, ExecutionException {
        return requestData(REQUEST_BATTERY_LEVEL)[2];
    }

    public double getOffsetHumitidy() throws TimeoutException, InterruptedException, ExecutionException {
        return ByteBuffer.wrap(requestData(REQUEST_OFFSET_HUMIDTY), 2, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() / 100d;
    }

    public double getOffsetTemperature() throws TimeoutException, InterruptedException, ExecutionException {
        return ByteBuffer.wrap(requestData(REQUEST_OFFSET_TEMPERATURE), 2, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() / 100d;
    }

    public String getHardwareRevision() throws TimeoutException, InterruptedException, ExecutionException {
        return new String(requestData(REQUEST_HARDWARE), 2, 7);
    }

    public String getFirmwareRevision() throws TimeoutException, InterruptedException, ExecutionException {
        return new String(requestData(REQUEST_FIRMWARE), 2, 7);
    }

    public Alarm getAlarmHumidity() throws TimeoutException, InterruptedException, ExecutionException {
        byte[] alarmReply = requestData(REQUEST_ALARM_HUMIDTY);
        return Alarm.fromData(alarmReply);
    }

    public Alarm getAlarmTemperature() throws TimeoutException, InterruptedException, ExecutionException {
        byte[] alarmReply = requestData(REQUEST_ALARM_TEMPERATURE);
        return Alarm.fromData(alarmReply);
    }

    public void setAlarmHumidity(Alarm alarm) throws InterruptedException, ExecutionException, TimeoutException {
        Objects.nonNull(alarm);
        sendUpdate(SEND_ALARM_HUMIDTY, alarm.encode());
    }

    public void setAlarmTemperature(Alarm alarm) throws InterruptedException, ExecutionException, TimeoutException {
        Objects.nonNull(alarm);
        sendUpdate(SEND_ALARM_TEMPERATURE, alarm.encode());
    }

    public void setOffsetHumidity(double value) throws InterruptedException, ExecutionException, TimeoutException {
        byte[] param = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort((short) (value * 100)).array();
        sendUpdate(SEND_OFFSET_HUMIDTY, param);
    }

    public void setOffsetTemperature(double value) throws InterruptedException, ExecutionException, TimeoutException {
        byte[] param = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort((short) (value * 100)).array();
        sendUpdate(SEND_OFFSET_TEMPERATURE, param);
    }

    public Measurement getCurrentMeasurement() throws InterruptedException, ExecutionException, TimeoutException {
        Future<Object> result = commandValueListener.getNextValue();
        sendCommand(commandCharacteristic, REQUEST_CURRENT_MEASUREMENT);
        Object res = result.get(5, TimeUnit.SECONDS);
        byte[] measurement = decrypt(toByteArray(res));
        checkResult(REQUEST_CURRENT_MEASUREMENT, measurement);
        return Measurement.fromData(measurement);
    }

    public List<HistoryMeasurement> getHistoricMeasurements(int startMinutesBack, int endMinutesBack) throws InterruptedException, ExecutionException, TimeoutException {
        if(startMinutesBack < endMinutesBack) {
            throw new IllegalArgumentException("startMinutesBack (" + startMinutesBack + ") must be bigger than endMinutesBack (" + endMinutesBack + ")");
        }
        List<HistoryMeasurement> measurements = new ArrayList<>();
        int stepLength = 200; // Timeouts are observed when complete set is queried
        for(int i = endMinutesBack; i < startMinutesBack; i += stepLength) {
            while (true) {
                try {
                    int start = Math.min(i + stepLength, startMinutesBack);
                    int end = i;
                    Future<Object> result = commandValueListener.getNextValue();
                    dataValueListener.startBuffering();
                    sendCommand(commandCharacteristic, REQUEST_HISTORIC_MEASUREMENTS, new byte[]{(byte) (0xFF & (start >> 8)), (byte) (0xFF & start), (byte) (0xFF & (end >> 8)), (byte) (0xFF & end)});
                    Object res = result.get(5, TimeUnit.SECONDS);
                    result = commandValueListener.getNextValue();

                    OffsetDateTime odt = OffsetDateTime.now();
                    res = result.get(10, TimeUnit.SECONDS);
                    byte[] data = decrypt(toByteArray(res));
                    int expectedCount = expectedCount = ByteBuffer.wrap(data, 2, 2).getShort();

                    List<Object> resultData = dataValueListener.getBufferAndClear();
                    if (resultData.size() != expectedCount) {
                        throw new IllegalStateException("Got: " + resultData.size() + " / " + expectedCount);
                    }
                    resultData.forEach(c -> {
                        byte[] row = decrypt(toByteArray(c));
                        measurements.addAll(HistoryMeasurement.fromData(odt, row));
                    });
                    break;
                } catch (TimeoutException | IllegalStateException ex) {
                }
            }
        }

        measurements.sort(Comparator.comparing(hm -> hm.minutesBack()));

        return measurements;
    }

    @Override
    public void close() throws Exception {
        try (
                deviceValueListenerRegistration;
                dataValueListenerRegistration;
                commandValueListenerRegistration
        ) {
            deviceCharacteristic.StopNotify();
            dataCharacteristic.StopNotify();
            commandCharacteristic.StopNotify();
        }
        dev.Disconnect();
    }

    private byte[] requestData(byte[] command) throws TimeoutException, InterruptedException, ExecutionException, IllegalStateException {
        int MAX_TRIES = 2;
        for(int j = 0; j < MAX_TRIES; j++) {
            Future<Object> result = deviceValueListener.getNextValue();
            sendCommand(deviceCharacteristic, command);
            try {
                byte[] res = decrypt(toByteArray(result.get(5, TimeUnit.SECONDS)));
                checkResult(command, res);
                return res;
            } catch (TimeoutException ex) {
                LOG.log(Level.DEBUG, () -> "Retry request: " + HexFormat.ofDelimiter(" ").formatHex(command));
            }
        }
        throw new IllegalStateException("Maximum retries exceeded: " + MAX_TRIES);
    }

    private void sendCommand(GattCharacteristic1 char1, byte[]... commands) {
        int requiredLength = 0;
        for(byte[] command: commands) {
            requiredLength += command.length;
        }
        byte[] request = new byte[Math.max(requiredLength, 19) + 1];
        int offset = 0;
        for(byte[] command: commands) {
            System.arraycopy(command, 0, request, offset, command.length);
            offset += command.length;
        }
        byte checksum = 0;
        for (int i = 0; i < request.length; i++) {
            checksum ^= request[i];
        }
        request[request.length - 1] = checksum;
        LOG.log(System.Logger.Level.DEBUG, () -> "Send >>> " + HexFormat.of().withDelimiter(" ").formatHex(request));
        byte[] requestEncrypted;
        if (connectionEncryption != null) {
            requestEncrypted = connectionEncryption.encrypt(request);
            LOG.log(System.Logger.Level.DEBUG, () -> "Send >>> " + HexFormat.of().withDelimiter(" ").formatHex(requestEncrypted));
        } else {
            requestEncrypted = request; // No encryption for older devices/firmwares
        }
        char1.WriteValue(requestEncrypted, Map.of("type", new Variant<>("request")));
    }

    private void sendUpdate(byte[] command, byte[] argument) throws InterruptedException, ExecutionException, TimeoutException {
        Future<Object> result = deviceValueListener.getNextValue();
        sendCommand(deviceCharacteristic, command, argument);
        byte[] res = decrypt(toByteArray(result.get(5, TimeUnit.SECONDS)));
        checkResult(command, res);
    }

    public void checkResult(byte[] command, byte[] res) throws IllegalStateException {
        int diff = Arrays.mismatch(command, res);
        if (diff > -1 /* identity */ && diff < 2 /* command is two bytes, these must be identical */) {
            HexFormat format = HexFormat.ofDelimiter(" ");
            throw new IllegalStateException("Did not receive confirmation expected. Prefix match: " + format.formatHex(command) + " expected in " + format.formatHex(res));
        }
    }

    /**
     * Decrypt the provided packet input, if encryption is enabled
     *
     * @param input
     * @return
     */
    private byte[] decrypt(byte[] input) {
        if(input == null) {
            return null;
        }
        if(connectionEncryption != null) {
            return connectionEncryption.decrypt(input);
        } else {
            return input;
        }
    }

    /**
     * UUID Pair to identify characteristic
     */
    private record CharAddr(String serice, String characteristic) {}
}
