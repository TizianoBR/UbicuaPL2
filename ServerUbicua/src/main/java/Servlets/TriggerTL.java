package servlets;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import logic.Log;
import mqtt.MQTTPublisher;
import mqtt.MQTTBroker;

/**
 * Servlet implementation class GetData
 */
@WebServlet("/TriggerTL")
public class TriggerTL extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        // Publish message and return a simple JSON result so the frontend can evaluate success
        try {
            String msg = "{\"boton\": 1}";
            Log.log.info("Ready to send " + msg);
            MQTTPublisher.publish(new MQTTBroker(), "ST_1103", msg);
            Log.log.info("msg sent");
            response.setStatus(HttpServletResponse.SC_OK);
            try (PrintWriter out = response.getWriter()) {
                out.print("{\"success\":true}");
                out.flush();
            }
        } catch (Exception e) {
            Log.log.error("msg could not be sent", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try (PrintWriter out = response.getWriter()) {
                out.print("{\"success\":false,\"error\":\"publish_failed\"}");
                out.flush();
            }
        }
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}

