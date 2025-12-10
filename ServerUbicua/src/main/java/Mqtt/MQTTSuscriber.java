package mqtt;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import database.Topics;
import database.ConectionDDBB;
import logic.Log;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Date;

public class MQTTSuscriber implements MqttCallback {

    private MqttClient client;
    private String brokerUrl;
    private String clientId;
    private String username;
    private String password;

    public MQTTSuscriber(MQTTBroker broker) {
        this.brokerUrl = broker.getBroker();
        this.clientId = broker.getClientId();
        this.username = broker.getUsername();
        this.password = broker.getPassword();
    }

    public void subscribeTopic(String topic) {
        try {
            MemoryPersistence persistence = new MemoryPersistence();
            client = new MqttClient(brokerUrl, MQTTBroker.getSubscriberClientId(), persistence);

            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setUserName(username);
            connOpts.setPassword(password.toCharArray());
            connOpts.setCleanSession(false); // Para mantener la suscripción
            connOpts.setAutomaticReconnect(true); // Reconexión automática
            connOpts.setConnectionTimeout(10);

            client.setCallback(this);
            client.connect(connOpts);

            client.subscribe(topic, 1); // QoS 1 para asegurarse de recibir
            Log.logmqtt.info("Subscribed to {}", topic);

        } catch (MqttException e) {
            Log.logmqtt.error("Error subscribing to topic: {}", e);
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        Log.logmqtt.warn("MQTT Connection lost, cause: {}", cause.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = message.toString();
        Log.logmqtt.info("{}: {}", topic, payload);

        try {
            JsonElement je = JsonParser.parseString(payload);
            if (je.isJsonObject()) {
                JsonObject jo = je.getAsJsonObject();

                String sensorId = null;
                if (jo.has("sensor_id") && !jo.get("sensor_id").isJsonNull()) sensorId = jo.get("sensor_id").getAsString();
                else if (jo.has("S_id") && !jo.get("S_id").isJsonNull()) sensorId = jo.get("S_id").getAsString();

                Timestamp ts = null;
                if (jo.has("time") && !jo.get("time").isJsonNull()) {
                    try {
                        ts = Timestamp.from(Instant.parse(jo.get("time").getAsString()));
                    } catch (DateTimeParseException | IllegalArgumentException ex) {
                        // ignore and use server time
                    }
                }

                ConectionDDBB conector = new ConectionDDBB();
                Connection con = null;
                try {
                    con = conector.obtainConnection(true);

                    // traffic_light
                    if (jo.has("current_state") || jo.has("cycle_position")) {
                        // Ensure sensor exists: insert or update minimal metadata if provided in payload
                        if (sensorId != null) {
                            PreparedStatement psSensor = con.prepareStatement(
                                    "INSERT INTO sensors (sensor_id, street_id, sensor_type, latitude, longitude, district, neighborhood) VALUES (?, ?, ?, ?, ?, ?, ?) "
                                            + "ON CONFLICT (sensor_id) DO UPDATE SET "
                                            + "street_id = COALESCE(EXCLUDED.street_id, sensors.street_id), "
                                            + "sensor_type = COALESCE(EXCLUDED.sensor_type, sensors.sensor_type), "
                                            + "latitude = COALESCE(EXCLUDED.latitude, sensors.latitude), "
                                            + "longitude = COALESCE(EXCLUDED.longitude, sensors.longitude), "
                                            + "district = COALESCE(EXCLUDED.district, sensors.district), "
                                            + "neighborhood = COALESCE(EXCLUDED.neighborhood, sensors.neighborhood)");
                            psSensor.setString(1, sensorId);
                            if (jo.has("street_id") && !jo.get("street_id").isJsonNull()) psSensor.setString(2, jo.get("street_id").getAsString()); else psSensor.setNull(2, java.sql.Types.CHAR);
                            if (jo.has("sensor_type") && !jo.get("sensor_type").isJsonNull()) psSensor.setString(3, jo.get("sensor_type").getAsString()); else psSensor.setNull(3, java.sql.Types.VARCHAR);
                            if (jo.has("latitude") && !jo.get("latitude").isJsonNull()) psSensor.setDouble(4, jo.get("latitude").getAsDouble()); else psSensor.setNull(4, java.sql.Types.DOUBLE);
                            if (jo.has("longitude") && !jo.get("longitude").isJsonNull()) psSensor.setDouble(5, jo.get("longitude").getAsDouble()); else psSensor.setNull(5, java.sql.Types.DOUBLE);
                            if (jo.has("district") && !jo.get("district").isJsonNull()) psSensor.setString(6, jo.get("district").getAsString()); else psSensor.setNull(6, java.sql.Types.VARCHAR);
                            if (jo.has("neighborhood") && !jo.get("neighborhood").isJsonNull()) psSensor.setString(7, jo.get("neighborhood").getAsString()); else psSensor.setNull(7, java.sql.Types.VARCHAR);
                            try { psSensor.executeUpdate(); } catch (SQLException ex) { Log.logmqtt.warn("Could not upsert sensor metadata: {}", ex.toString()); }
                            psSensor.close();
                        } else {
                            Log.logmqtt.warn("Received traffic_light payload without sensor_id, skipping insert");
                            return;
                        }

                        PreparedStatement ps = con.prepareStatement(
                                "INSERT INTO traffic_light (sensor_id, time, current_state, cycle_position, time_remaining, traffic_light_type, circulation_dir, pedestrian_waiting, pedestrian_button_pressed, malfunction_detected, cycle_count, state_changed, last_state_change) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                        ps.setString(1, sensorId);
                        ps.setTimestamp(2, ts != null ? ts : new Timestamp(new Date().getTime()));
                        if (jo.has("current_state") && !jo.get("current_state").isJsonNull()) ps.setString(3, jo.get("current_state").getAsString()); else ps.setNull(3, java.sql.Types.VARCHAR);
                        if (jo.has("cycle_position") && !jo.get("cycle_position").isJsonNull()) ps.setInt(4, jo.get("cycle_position").getAsInt()); else ps.setNull(4, java.sql.Types.INTEGER);
                        if (jo.has("time_remaining") && !jo.get("time_remaining").isJsonNull()) ps.setInt(5, jo.get("time_remaining").getAsInt()); else ps.setNull(5, java.sql.Types.INTEGER);
                        if (jo.has("traffic_light_type") && !jo.get("traffic_light_type").isJsonNull()) ps.setString(6, jo.get("traffic_light_type").getAsString()); else ps.setNull(6, java.sql.Types.VARCHAR);
                        if (jo.has("circulation_dir") && !jo.get("circulation_dir").isJsonNull()) ps.setString(7, jo.get("circulation_dir").getAsString()); else ps.setNull(7, java.sql.Types.VARCHAR);
                        if (jo.has("pedestrian_waiting") && !jo.get("pedestrian_waiting").isJsonNull()) ps.setBoolean(8, jo.get("pedestrian_waiting").getAsBoolean()); else ps.setNull(8, java.sql.Types.BOOLEAN);
                        if (jo.has("pedestrian_button_pressed") && !jo.get("pedestrian_button_pressed").isJsonNull()) ps.setBoolean(9, jo.get("pedestrian_button_pressed").getAsBoolean()); else ps.setNull(9, java.sql.Types.BOOLEAN);
                        if (jo.has("malfunction_detected") && !jo.get("malfunction_detected").isJsonNull()) ps.setBoolean(10, jo.get("malfunction_detected").getAsBoolean()); else ps.setNull(10, java.sql.Types.BOOLEAN);
                        if (jo.has("cycle_count") && !jo.get("cycle_count").isJsonNull()) ps.setInt(11, jo.get("cycle_count").getAsInt()); else ps.setNull(11, java.sql.Types.INTEGER);
                        if (jo.has("state_changed") && !jo.get("state_changed").isJsonNull()) ps.setBoolean(12, jo.get("state_changed").getAsBoolean()); else ps.setNull(12, java.sql.Types.BOOLEAN);
                        if (jo.has("last_state_change") && !jo.get("last_state_change").isJsonNull()) {
                            try { ps.setTimestamp(13, Timestamp.from(Instant.parse(jo.get("last_state_change").getAsString()))); } catch (Exception e) { ps.setNull(13, java.sql.Types.TIMESTAMP); }
                        } else ps.setNull(13, java.sql.Types.TIMESTAMP);
                        ps.executeUpdate();
                        Log.logmqtt.info("Inserted traffic_light record for sensor {}", sensorId);
                        return;
                    }

                    

                    // measurements
                    if (jo.has("value") || jo.has("measurements")) {
                        if (jo.has("measurements") && jo.get("measurements").isJsonArray()) {
                            JsonArray arr = jo.getAsJsonArray("measurements");
                            PreparedStatement ps = con.prepareStatement("INSERT INTO measurements (time, value) VALUES (?, ?)");
                            for (JsonElement e : arr) {
                                if (e.isJsonObject() && e.getAsJsonObject().has("value")) {
                                    int v = e.getAsJsonObject().get("value").getAsInt();
                                    Timestamp t = ts != null ? ts : new Timestamp(new Date().getTime());
                                    ps.setTimestamp(1, t);
                                    ps.setInt(2, v);
                                    ps.addBatch();
                                }
                            }
                            ps.executeBatch();
                            Log.logmqtt.info("Inserted {} measurements", arr.size());
                            return;
                        } else if (jo.has("value")) {
                            int v = jo.get("value").getAsInt();
                            PreparedStatement ps = con.prepareStatement("INSERT INTO measurements (time, value) VALUES (?, ?)");
                            ps.setTimestamp(1, ts != null ? ts : new Timestamp(new Date().getTime()));
                            ps.setInt(2, v);
                            ps.executeUpdate();
                            Log.logmqtt.info("Inserted measurement {}", v);
                            return;
                        }
                    }

                } catch (SQLException ex) {
                    Log.logmqtt.error("DB error inserting message: {}", ex.toString());
                } finally {
                    conector.closeConnection(con);
                }
            }
        } catch (JsonSyntaxException ex) {
            Log.logmqtt.warn("Received non-JSON payload: {}", payload);
        } catch (Exception ex) {
            Log.logmqtt.error("Error processing incoming message: {}", ex.toString());
        }

        // Fallback - store raw payload in Topics table/object for later processing
        Topics newTopic = new Topics();
        newTopic.setValue(payload);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
    }
}
