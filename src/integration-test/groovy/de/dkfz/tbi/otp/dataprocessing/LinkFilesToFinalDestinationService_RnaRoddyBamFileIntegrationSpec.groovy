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
package de.dkfz.tbi.otp.dataprocessing

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.domainFactory.pipelines.roddyRna.RoddyRnaFactory
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.utils.CreateRoddyFileHelper
import de.dkfz.tbi.otp.utils.LinkFileUtils

import java.nio.file.FileSystems
import java.nio.file.Path

@Rollback
@Integration
class LinkFilesToFinalDestinationService_RnaRoddyBamFileIntegrationSpec extends Specification implements RoddyRnaFactory {

    LinkFilesToFinalDestinationService service

    @Autowired
    RemoteShellHelper remoteShellHelper

    @TempDir
    Path tempDir
    RnaRoddyBamFile roddyBamFile
    TestConfigService configService

    void setupData() {
        service = new LinkFilesToFinalDestinationService()
        service.linkFileUtils = new LinkFileUtils()
        service.linkFileUtils.fileService = service.fileService
        service.linkFileUtils.fileService = new FileService()
        service.linkFileUtils.fileSystemService = Mock(FileSystemService) {
            _ * getRemoteFileSystem() >> FileSystems.default
            0 * _
        }

        roddyBamFile = createBamFile()

        configService.addOtpProperties(tempDir)
    }

    void cleanup() {
        configService.clean()
    }

    void "test linkNewRnaResults"() {
        given:
        setupData()
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)

        when:
        service.linkNewRnaResults(roddyBamFile)

        then:
        [
                roddyBamFile.finalBamFile,
                roddyBamFile.finalBaiFile,
                roddyBamFile.finalMd5sumFile,
                roddyBamFile.finalExecutionStoreDirectory,
                roddyBamFile.finalQADirectory,
                new File(roddyBamFile.baseDirectory, "additionalArbitraryFile"),
        ].each {
            assert it.exists()
        }
    }
}
