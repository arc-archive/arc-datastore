package com.mulesoft.arc.arcdatastore.backend;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by jarrod on 08/07/16.
 */
public class LetsEncrypt extends HttpServlet {

    private static final Logger log = Logger.getLogger(LetsEncrypt.class.getName());

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if ("/mmeiOutsneZs_KU20ij0Rjleqwxh0U46UGcMcziIQ8E".equals(path)) {
            sendHash(resp, "mmeiOutsneZs_KU20ij0Rjleqwxh0U46UGcMcziIQ8E._DW7L_TfipACEMVC7uKg7PeC6aIV1ON1GkYI5EvOTQ0");
        } else if ("/ctbJ-yVLqcwAGGGg82KXr3qPlRhN25SIhgLmga4feBg".equals(path)) {
            sendHash(resp, "ctbJ-yVLqcwAGGGg82KXr3qPlRhN25SIhgLmga4feBg._DW7L_TfipACEMVC7uKg7PeC6aIV1ON1GkYI5EvOTQ0");
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void sendHash(HttpServletResponse resp, String response) throws IOException {
        resp.getWriter().print(response);
    }
}
