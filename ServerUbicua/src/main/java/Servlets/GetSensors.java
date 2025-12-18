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
import database.ConectionDDBB;

/**
 * Servlet implementation class GetData
 */
@WebServlet("/GetSensors")
public class GetSensors extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String StrId = request.getParameter("StrId");
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        Connection con = null;
        try {
            ConectionDDBB conectionDDBB = new ConectionDDBB();
            con = conectionDDBB.obtainConnection(true);

            ArrayList<String> sensors = new ArrayList<>();
            String sql = "SELECT DISTINCT sensor_id FROM sensors WHERE street_id = ?";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, StrId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    sensors.add(rs.getString("sensor_id"));
                }
            }

            out.println(new Gson().toJson(sensors));
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

