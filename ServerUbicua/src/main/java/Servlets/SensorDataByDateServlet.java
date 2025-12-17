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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import logic.Log;

@WebServlet("/GetDataByDate")
public class SensorDataByDateServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private DataSource dataSource;
    private Gson gson = new Gson();

    @Override
    public void init() throws ServletException {
        try {
            // JNDI lookup para el pool JDBC
            Context ctx = new InitialContext();
            dataSource = (DataSource) ctx.lookup("java:/comp/env/jdbc/ubicuabbdd");
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

        String sql = "SELECT sensor_id, time, current_state " +
                     "FROM traffic_light " +
                     "WHERE time >= ? AND time < ? " +
                     "ORDER BY time ASC";

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
                    row.put("time", rs.getTimestamp("time").toLocalDateTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                    row.put("current_state", rs.getString("current_state"));

                    // Si current_state contiene JSON, insertarlo como JsonElement para que no venga como string escapado
                    // try {
                    //     JsonElement parsed = JsonParser.parseString(currentState);
                    //     row.put("data", parsed);
                    // } catch (Exception pe) {
                    //     // si no es JSON válido, lo devolvemos como string
                    //     row.put("data", currentState);
                    // }

                    results.add(row);
                }
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
