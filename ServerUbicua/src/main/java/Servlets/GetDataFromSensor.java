package servlets;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import logic.Log;
import logic.DatosSensor;
import database.ConectionDDBB;

/**
 * Servlet implementation class GetData
 */
@WebServlet("/GetSensorData")
public class GetDataFromSensor extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String SId = request.getParameter("SId");
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        Connection con = null;
        try {
            ConectionDDBB conectionDDBB = new ConectionDDBB();
            con = conectionDDBB.obtainConnection(true);

            ArrayList<DatosSensor> datos = new ArrayList<>();
            String sql = "SELECT current_state, pedestrian_waiting, time FROM traffic_light WHERE sensor_id = ? ORDER BY time DESC LIMIT 3";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, SId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    DatosSensor ds = new DatosSensor();
                    ds.setCurrentState(rs.getString("current_state"));
                    ds.setPedestrianWaiting(rs.getBoolean("pedestrian_waiting"));
                    ds.setTime(rs.getString("time"));
                    datos.add(ds);
                }
            }

            out.println(new Gson().toJson(datos));
        } catch (Exception e) {
            out.println("[]");  // mejor devolver JSON vacío para evitar error JS
            Log.log.error("Exception: " + e);
        } finally {
            if (con != null) {
                new ConectionDDBB().closeConnection(con);
            }
            out.close();
        }
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}

