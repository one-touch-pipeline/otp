package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.ProcessHelperService

/**
 * This service provides file operations
 *
 */
class ExecutionHelperService {

    ExecutionService executionService


    String getGroup(File directory) {
        assert directory: 'directory may not be null'
        ProcessHelperService.executeAndAssertExitCodeAndErrorOutAndReturnStdout("stat -c '%G' ${directory}").trim()
    }

    String setGroup(Realm realm, File directory, String group) {
        assert realm: 'realm may not be null'
        assert directory: 'directory may not be null'
        assert group: 'group may not be null'
        executionService.executeCommand(realm, "chgrp ${group} ${directory}")
    }

    String setPermission(Realm realm, File directory, String permission) {
        assert realm: 'realm may not be null'
        assert directory: 'directory may not be null'
        assert permission: 'permission may not be null'
        executionService.executeCommand(realm, "chmod  ${permission} ${directory}")
    }
}
