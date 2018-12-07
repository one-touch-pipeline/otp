package de.dkfz.tbi.otp.security

import de.dkfz.odcf.audit.impl.*
import de.dkfz.odcf.audit.xml.layer.EventIdentification.EventOutcomeIndicator
import org.springframework.stereotype.Component
import org.springframework.security.core.context.*

import static de.dkfz.tbi.otp.security.DicomAuditUtils.*

@Component("dicomAuditConsoleHandler")
class DicomAuditConsoleHandler {
    // Hack: The dicomAuditConsolseHandler#log method is a pseudo condition
    // that always returns true and logs the access to the console as side effect.
    // There is sadly no other way to intercept the access to these URLs.
    boolean log() {
        DicomAuditLogger.logEmergencyOverrideStart(EventOutcomeIndicator.SUCCESS,
            getRealUserName(SecurityContextHolder.getContext().authentication.principal.username))
        return true
    }
}
