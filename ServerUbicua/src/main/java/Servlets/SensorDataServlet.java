package Servlets;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.sql.*;
import javax.naming.*;
import javax.sql.DataSource;

import mqtt.MQTTPublisher;
import mqtt.MQTTSuscriber;
import mqtt.MQTTBroker;

public class SensorDataServlet extends HttpServlet {

    private DataSource dataSource;

    private MQTTSuscriber subscriber;
    private MQTTPublisher publisher;

    @Override
    public void init() throws ServletException {
        try {
            // Obtener pool JDBC
            Context ctx = new InitialContext();
            dataSource = (DataSource) ctx.lookup("java:/comp/env/jdbc/ubicuabbdd");

            // **Instanciar publisher**
            publisher = new MQTTPublisher();

            // **Arrancar suscriptor**
            subscriber = new MQTTSuscriber(new MQTTBroker());

            subscriber.subscribeTopic("city/sensors/commands");

        } catch (Exception e) {
            throw new ServletException("Error inicializando MQTT o DB", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String date = request.getParameter("date");

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        if (date == null) {
            response.setStatus(400);
            out.print("{\"error\":\"Falta parámetro 'date'\"}");
            return;
        }

        String sql = "SELECT sensor_id, sensor_type, timestamp, data_json FROM sensor_data WHERE DATE(timestamp)=?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, date);
            ResultSet rs = stmt.executeQuery();

            // **Publicación MQTT real**
            publisher.publish(
                new MQTTBroker(),
                "city/logs", 
                "{ \"event\":\"query\", \"date\":\"" + date + "\" }"
            );

            // Construcción del JSON
            out.print("[");
            boolean first = true;
            while (rs.next()) {
                if (!first) out.print(",");
                first = false;

                out.print("{");
                out.print("\"sensor_id\":\"" + rs.getString("sensor_id") + "\",");
                out.print("\"sensor_type\":\"" + rs.getString("sensor_type") + "\",");
                out.print("\"timestamp\":\"" + rs.getTimestamp("timestamp") + "\",");
                out.print("\"data\":" + rs.getString("data_json"));
                out.print("}");
            }
            out.print("]");

        } catch (SQLException e) {
            response.setStatus(500);
            out.print("{\"error\":\"Error consultando la BD\"}");
        }
    }
}