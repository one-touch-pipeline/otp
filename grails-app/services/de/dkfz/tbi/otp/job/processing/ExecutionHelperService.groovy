/*
 * Copyright 2011-2023 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.job.processing

import grails.gorm.transactions.Transactional
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.ProcessOutput
import de.dkfz.tbi.otp.utils.exceptions.OtpRuntimeException

import java.nio.file.Path

/**
 * This service provides file operations
 */
@Transactional
@Deprecated
class ExecutionHelperService {

    @Autowired
    RemoteShellHelper remoteShellHelper

    /**
     * @deprecated use {@link Project#unixGroup}
     */
    @Deprecated
    String getGroup(File directory) {
        assert directory: 'directory may not be null'
        ProcessOutput result = remoteShellHelper.executeCommandReturnProcessOutput("stat -c '%G' ${directory}")
        if (result.exitCode != 0) {
            throw new OtpRuntimeException("Getting group failed: ${result.stderr}; exit code: ${result.exitCode}")
        }
        return result.stdout.trim()
    }

    /**
     * @deprecated use {@link FileService#setGroupViaBash(Path, String)}
     */
    @Deprecated
    String setGroup(File directory, String group) {
        assert directory: 'directory may not be null'
        assert group: 'group may not be null'
        ProcessOutput result = remoteShellHelper.executeCommandReturnProcessOutput("chgrp -h ${group} ${directory}")
        if (result.exitCode != 0) {
            throw new OtpRuntimeException("Setting group '${group}' failed: ${result.stderr}; exit code: ${result.exitCode}")
        }
        return result.stdout
    }

    /**
     * @deprecated use {@link FileService#setPermissionViaBash(Path, String)}
     */
    @Deprecated
    String setPermission(File directory, String permission) {
        assert directory: 'directory may not be null'
        assert permission: 'permission may not be null'
        ProcessOutput result = remoteShellHelper.executeCommandReturnProcessOutput("chmod  ${permission} ${directory}")
        if (result.exitCode != 0) {
            throw new OtpRuntimeException("Setting permission failed: ${result.stderr}; exit code: ${result.exitCode}")
        }
        return result.stdout
    }
}
