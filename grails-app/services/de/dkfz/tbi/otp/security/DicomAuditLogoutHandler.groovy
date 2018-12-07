package de.dkfz.tbi.otp.security

import de.dkfz.odcf.audit.impl.*
import de.dkfz.odcf.audit.xml.layer.EventIdentification.EventOutcomeIndicator
import org.springframework.security.core.*
import org.springframework.security.web.authentication.logout.*
import javax.servlet.http.*

import static de.dkfz.tbi.otp.security.DicomAuditUtils.*

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
