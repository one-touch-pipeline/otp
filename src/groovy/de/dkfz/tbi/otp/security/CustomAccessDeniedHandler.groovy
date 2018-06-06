package de.dkfz.tbi.otp.security

import org.apache.log4j.Logger
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler

import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    Logger log = Logger.getLogger(CustomAccessDeniedHandler.class.name);

    @Override
    void handle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AccessDeniedException e) throws IOException, ServletException {
        log.debug("Corrupted request: ${httpServletRequest.getRequestURL()}")
        httpServletResponse.sendError(405,"Request was corrupted")
    }

}
