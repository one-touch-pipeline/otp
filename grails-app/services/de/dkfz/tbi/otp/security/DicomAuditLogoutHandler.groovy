package de.dkfz.tbi.otp.security

import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.logout.LogoutHandler

import de.dkfz.odcf.audit.impl.DicomAuditLogger
import de.dkfz.odcf.audit.xml.layer.EventIdentification.EventOutcomeIndicator

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import static de.dkfz.tbi.otp.security.DicomAuditUtils.getRealUserName

/**
 * A handler class for Spring Security logout events,
 * forwarding the logout to the Dicom audit logging framework.
 */
class DicomAuditLogoutHandler implements LogoutHandler {
    @Override
    void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        DicomAuditLogger.logUserLogout(EventOutcomeIndicator.SUCCESS, getRealUserName(authentication.principal.username))
    }
}
