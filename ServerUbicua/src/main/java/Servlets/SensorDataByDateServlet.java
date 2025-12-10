package Servlets;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mqtt.MQTTPublisher;
import mqtt.MQTTSuscriber;
import mqtt.MQTTBroker;

import logic.Log;

@WebServlet("/GetDataByDate")
public class SensorDataByDateServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private DataSource dataSource;
    private MQTTSuscriber subscriber;
    private MQTTPublisher publisher;
    private Gson gson = new Gson();

    @Override
    public void init() throws ServletException {
        try {
            // JNDI lookup para el pool JDBC
            Context ctx = new InitialContext();
            dataSource = (DataSource) ctx.lookup("java:/comp/env/jdbc/ubicuabbdd");

            // MQTT publisher / subscriber (como en tu código original)
            publisher = new MQTTPublisher();
            subscriber = new MQTTSuscriber(new MQTTBroker());
            subscriber.subscribeTopic("city/sensors/commands");

        } catch (Exception e) {
            throw new ServletException("Error inicializando MQTT o DB", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String dateStr = request.getParameter("date"); // esperado: "yyyy-MM-dd"
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        if (dateStr == null || dateStr.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"Falta parámetro 'date' (formato yyyy-MM-dd)\"}");
            return;
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dateStr); // lanza DateTimeParseException si no va
        } catch (Exception ex) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"Formato de fecha inválido. Usa yyyy-MM-dd\"}");
            return;
        }

        // rango [date 00:00:00, nextDay 00:00:00)
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        String sql = "SELECT sensor_id, sensor_type, timestamp, data_json " +
                     "FROM sensor_data " +
                     "WHERE timestamp >= ? AND timestamp < ? " +
                     "ORDER BY timestamp ASC";

        List<Map<String, Object>> results = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // setTimestamp usando zona del sistema; ajusta si necesitas UTC
            stmt.setTimestamp(1, Timestamp.valueOf(start));
            stmt.setTimestamp(2, Timestamp.valueOf(end));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("sensor_id", rs.getString("sensor_id"));
                    row.put("sensor_type", rs.getString("sensor_type"));
                    row.put("timestamp", rs.getTimestamp("timestamp").toInstant().toString());

                    String dataJson = rs.getString("data_json");
                    // Si data_json contiene JSON, insertarlo como JsonElement para que no venga como string escapado
                    try {
                        JsonElement parsed = JsonParser.parseString(dataJson);
                        row.put("data", parsed);
                    } catch (Exception pe) {
                        // si no es JSON válido, lo devolvemos como string
                        row.put("data", dataJson);
                    }

                    results.add(row);
                }
            }

            // publicar evento MQTT con info de la consulta
            try {
                String logPayload = "{\"event\":\"query\", \"date\":\"" + dateStr + "\"}";
                publisher.publish(new MQTTBroker(), "city/logs", logPayload);
            } catch (Exception mqttEx) {
                // no fallamos la petición por un fallo en MQTT; pero puedes loguearlo
                // System.err.println("MQTT publish failed: " + mqttEx.getMessage());
            }

            // Serializar con Gson. Nota: Gson convertirá los JsonElement correctamente.
            out.print(gson.toJson(results));

        } catch (SQLException e) {
            // Registrar la excepción
            Log.log.error("Error consultando la BD", e);

            // Si la excepción indica que la tabla no existe (Postgres SQLState 42P01),
            // devolvemos un array vacío para no romper clientes. Para otros errores
            // devolvemos 500 para no ocultar problemas graves.
            String sqlState = e.getSQLState();
            if ("42P01".equals(sqlState) || (sqlState == null && e.getMessage() != null && e.getMessage().toLowerCase().contains("relation"))) {
                out.print("[]");
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print("{\"error\":\"Error consultando la BD\"}");
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // opcional: reenviar a GET o no permitido
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }
}
