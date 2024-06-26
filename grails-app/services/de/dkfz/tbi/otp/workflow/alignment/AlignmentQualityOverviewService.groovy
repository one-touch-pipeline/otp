/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.workflow.alignment

import grails.gorm.transactions.Transactional
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.dataprocessing.RoddyResultServiceFactoryService
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyResult
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.RoddyConfigService

import java.nio.file.Path

@Transactional
class AlignmentQualityOverviewService {

    FileService fileService

    RoddyConfigService roddyConfigService

    RoddyResultServiceFactoryService roddyResultServiceFactoryService

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#roddyResult.project, 'OTP_READ_ACCESS')")
    byte[] fetchConfigFileContent(RoddyResult roddyResult) {
        Path workDir = roddyResultServiceFactoryService.getService(roddyResult).getDirectoryPath(roddyResult)
        Path configFile = roddyConfigService.getConfigPath(workDir)

        return fileService.fileIsReadable(configFile) ? configFile.bytes : new byte[0]
    }
}
