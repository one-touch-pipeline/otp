package de.dkfz.tbi.otp.job.processing

import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.utils.LocalShellHelper
import de.dkfz.tbi.otp.utils.ProcessOutput

/**
 * This service provides file operations
 */
class ExecutionHelperService {

    @Autowired
    RemoteShellHelper remoteShellHelper


    String getGroup(File directory) {
        assert directory: 'directory may not be null'
        LocalShellHelper.executeAndAssertExitCodeAndErrorOutAndReturnStdout("stat -c '%G' ${directory}").trim()
    }

    String setGroup(Realm realm, File directory, String group) {
        assert realm: 'realm may not be null'
        assert directory: 'directory may not be null'
        assert group: 'group may not be null'
        ProcessOutput result = remoteShellHelper.executeCommandReturnProcessOutput(realm, "chgrp ${group} ${directory}")
        if (result.exitCode != 0 ) {
            throw new RuntimeException("Setting group failed: ${result.stderr}; exit code: ${result.exitCode}")
        }
        return result.stdout
    }

    String setPermission(Realm realm, File directory, String permission) {
        assert realm: 'realm may not be null'
        assert directory: 'directory may not be null'
        assert permission: 'permission may not be null'
        ProcessOutput result = remoteShellHelper.executeCommandReturnProcessOutput(realm, "chmod  ${permission} ${directory}")
        if (result.exitCode != 0 ) {
            throw new RuntimeException("Setting permission failed: ${result.stderr}; exit code: ${result.exitCode}")
        }
        return result.stdout
    }
}
