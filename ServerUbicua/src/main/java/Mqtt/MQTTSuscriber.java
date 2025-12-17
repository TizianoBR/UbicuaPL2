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

                // Permite leer campos anidados dentro de un objeto "data" si existen allí
                JsonObject dataObj = (jo.has("data") && jo.get("data").isJsonObject()) ? jo.getAsJsonObject("data") : null;
                // Ubicación puede venir en objetos "location" o "loc"
                JsonObject locationObj = (jo.has("location") && jo.get("location").isJsonObject()) ? jo.getAsJsonObject("location") : null;
                JsonObject locObj = (jo.has("loc") && jo.get("loc").isJsonObject()) ? jo.getAsJsonObject("loc") : null;

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
                    if (jo.has("current_state") || jo.has("state") || jo.has("cycle_position")) {
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
                            JsonElement streetIdEl = getField(jo, dataObj, locationObj, locObj, "street_id");
                            streetIdEl = streetIdEl != null ? streetIdEl : getField(jo, dataObj, locationObj, locObj, "Str_id");
                            JsonElement sensorTypeEl = getField(jo, dataObj, locationObj, locObj, "sensor_type");
                            sensorTypeEl = sensorTypeEl != null ? sensorTypeEl : getField(jo, dataObj, locationObj, locObj, "S_type");
                            JsonElement latEl = getField(jo, dataObj, locationObj, locObj, "latitude");
                            latEl = latEl != null ? latEl : getField(jo, dataObj, locationObj, locObj, "lat");
                            JsonElement lonEl = getField(jo, dataObj, locationObj, locObj, "longitude");
                            lonEl = lonEl != null ? lonEl : getField(jo, dataObj, locationObj, locObj, "long");
                            JsonElement districtEl = getField(jo, dataObj, locationObj, locObj, "district");
                            districtEl = districtEl != null ? districtEl : getField(jo, dataObj, locationObj, locObj, "zona");
                            JsonElement neighborhoodEl = getField(jo, dataObj, locationObj, locObj, "neighborhood");
                            neighborhoodEl = neighborhoodEl != null ? neighborhoodEl : getField(jo, dataObj, locationObj, locObj, "barrio");

                            if (streetIdEl != null) psSensor.setString(2, streetIdEl.getAsString()); else psSensor.setNull(2, java.sql.Types.CHAR);
                            if (sensorTypeEl != null) psSensor.setString(3, sensorTypeEl.getAsString()); else psSensor.setNull(3, java.sql.Types.VARCHAR);
                            if (latEl != null) psSensor.setDouble(4, latEl.getAsDouble()); else psSensor.setNull(4, java.sql.Types.DOUBLE);
                            if (lonEl != null) psSensor.setDouble(5, lonEl.getAsDouble()); else psSensor.setNull(5, java.sql.Types.DOUBLE);
                            if (districtEl != null) psSensor.setString(6, districtEl.getAsString()); else psSensor.setNull(6, java.sql.Types.VARCHAR);
                            if (neighborhoodEl != null) psSensor.setString(7, neighborhoodEl.getAsString()); else psSensor.setNull(7, java.sql.Types.VARCHAR);
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
                        JsonElement currentStateEl = getField(jo, dataObj, locationObj, locObj, "current_state");
                        currentStateEl = currentStateEl != null ? currentStateEl : getField(jo, dataObj, locationObj, locObj, "state");
                        JsonElement cyclePosEl = getField(jo, dataObj, locationObj, locObj, "cycle_position");
                        JsonElement timeRemainingEl = getField(jo, dataObj, locationObj, locObj, "time_remaining");
                        JsonElement tlTypeEl = getField(jo, dataObj, locationObj, locObj, "traffic_light_type");
                        tlTypeEl = tlTypeEl != null ? tlTypeEl : getField(jo, dataObj, locationObj, locObj, "tl_type");
                        JsonElement circulationEl = getField(jo, dataObj, locationObj, locObj, "circulation_dir");
                        JsonElement pedWaitEl = getField(jo, dataObj, locationObj, locObj, "pedestrian_waiting");
                        pedWaitEl = pedWaitEl != null ? pedWaitEl : getField(jo, dataObj, locationObj, locObj, "wait");
                        JsonElement pedBtnEl = getField(jo, dataObj, locationObj, locObj, "pedestrian_button_pressed");
                        JsonElement malfunctionEl = getField(jo, dataObj, locationObj, locObj, "malfunction_detected");
                        JsonElement cycleCountEl = getField(jo, dataObj, locationObj, locObj, "cycle_count");
                        JsonElement stateChangedEl = getField(jo, dataObj, locationObj, locObj, "state_changed");
                        JsonElement lastStateChangeEl = getField(jo, dataObj, locationObj, locObj, "last_state_change");

                        if (currentStateEl != null) ps.setString(3, currentStateEl.getAsString()); else ps.setNull(3, java.sql.Types.VARCHAR);
                        if (cyclePosEl != null) ps.setInt(4, cyclePosEl.getAsInt()); else ps.setNull(4, java.sql.Types.INTEGER);
                        if (timeRemainingEl != null) ps.setInt(5, timeRemainingEl.getAsInt()); else ps.setNull(5, java.sql.Types.INTEGER);
                        if (tlTypeEl != null) ps.setString(6, tlTypeEl.getAsString()); else ps.setNull(6, java.sql.Types.VARCHAR);
                        if (circulationEl != null) ps.setString(7, circulationEl.getAsString()); else ps.setNull(7, java.sql.Types.VARCHAR);
                        if (pedWaitEl != null) ps.setBoolean(8, pedWaitEl.getAsBoolean()); else ps.setNull(8, java.sql.Types.BOOLEAN);
                        if (pedBtnEl != null) ps.setBoolean(9, pedBtnEl.getAsBoolean()); else ps.setNull(9, java.sql.Types.BOOLEAN);
                        if (malfunctionEl != null) ps.setBoolean(10, malfunctionEl.getAsBoolean()); else ps.setNull(10, java.sql.Types.BOOLEAN);
                        if (cycleCountEl != null) ps.setInt(11, cycleCountEl.getAsInt()); else ps.setNull(11, java.sql.Types.INTEGER);
                        if (stateChangedEl != null) ps.setBoolean(12, stateChangedEl.getAsBoolean()); else ps.setNull(12, java.sql.Types.BOOLEAN);
                        if (lastStateChangeEl != null) {
                            try { ps.setTimestamp(13, Timestamp.from(Instant.parse(lastStateChangeEl.getAsString()))); } catch (Exception e) { ps.setNull(13, java.sql.Types.TIMESTAMP); }
                        } else ps.setNull(13, java.sql.Types.TIMESTAMP);
                        ps.executeUpdate();
                        Log.logmqtt.info("Inserted traffic_light record for sensor {}", sensorId);
                        return;
                    }

                    

                    // measurements
                    if (jo.has("value") || jo.has("measurements")) {
                        JsonElement measurementsEl = getField(jo, dataObj, locationObj, locObj, "measurements");
                        JsonElement valueEl = getField(jo, dataObj, locationObj, locObj, "value");

                        if (measurementsEl != null && measurementsEl.isJsonArray()) {
                            JsonArray arr = measurementsEl.getAsJsonArray();
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
                        } else if (valueEl != null) {
                            int v = valueEl.getAsInt();
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

    // Obtiene un campo desde el objeto raíz o, si no existe allí, desde objetos anidados "data", "location" o "loc"
    private JsonElement getField(JsonObject root, JsonObject dataObj, JsonObject locationObj, JsonObject locObj, String key) {
        if (root != null && root.has(key) && !root.get(key).isJsonNull()) return root.get(key);
        if (dataObj != null && dataObj.has(key) && !dataObj.get(key).isJsonNull()) return dataObj.get(key);
        if (locationObj != null && locationObj.has(key) && !locationObj.get(key).isJsonNull()) return locationObj.get(key);
        if (locObj != null && locObj.has(key) && !locObj.get(key).isJsonNull()) return locObj.get(key);
        return null;
    }
}
