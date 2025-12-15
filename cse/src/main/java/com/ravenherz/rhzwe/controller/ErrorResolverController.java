package com.ravenherz.rhzwe.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOException;

@Controller
public class ErrorResolverController extends AbstractController{

    @RequestMapping(value = "/error", method = RequestMethod.GET)
    public void handleError(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        int statusCode = -1;
        if (status != null) {
            try {
                statusCode = Integer.parseInt(status.toString());
            } catch (Exception ignored) {}
        }
        error(statusCode, request, response);
    }

}
