/*
 * Copyright 2011-2025 The OTP authors
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
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.domainFactory.pipelines.roddyRna.RoddyRnaFactory
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.infrastructure.alignment.RnaAlignmentLinkFileService
import de.dkfz.tbi.otp.infrastructure.alignment.RnaAlignmentWorkFileService
import de.dkfz.tbi.otp.utils.CreateRoddyFileHelper

import java.nio.file.Path

@Rollback
@Integration
class LinkFilesToFinalDestinationService_RnaRoddyBamFileIntegrationSpec extends Specification implements RoddyRnaFactory {

    LinkFilesToFinalDestinationService service

    @TempDir
    Path tempDir
    RnaRoddyBamFile roddyBamFile

    void setupData() {
        service = new LinkFilesToFinalDestinationService()
        service.rnaAlignmentLinkFileService = new RnaAlignmentLinkFileService()
        service.rnaAlignmentLinkFileService.abstractBamFileService = Mock(AbstractBamFileService) {
            getBaseDirectory(_) >> tempDir.resolve('link')
        }
        service.rnaAlignmentWorkFileService = new RnaAlignmentWorkFileService()
        service.rnaAlignmentWorkFileService.abstractBamFileService = Mock(AbstractBamFileService) {
            getBaseDirectory(_) >> tempDir.resolve('work')
        }
        service.fileService = Mock(FileService)

        roddyBamFile = createBamFile()
    }

    void "test linkNewRnaResults"() {
        given:
        setupData()
        CreateRoddyFileHelper.createRoddyAlignmentResultFiles(service.rnaAlignmentWorkFileService, roddyBamFile)
        List<Path> linkedFiles = [
                service.rnaAlignmentLinkFileService.getBamFile(roddyBamFile),
                service.rnaAlignmentLinkFileService.getBaiFile(roddyBamFile),
                service.rnaAlignmentLinkFileService.getMd5sumFile(roddyBamFile),
                service.rnaAlignmentLinkFileService.getExecutionStoreDirectory(roddyBamFile),
                service.rnaAlignmentLinkFileService.getQADirectory(roddyBamFile),
                service.rnaAlignmentLinkFileService.getCorrespondingChimericBamFile(roddyBamFile),
                service.rnaAlignmentLinkFileService.getDirectoryPath(roddyBamFile).resolve("additionalArbitraryFile"),
        ]

        when:
        service.linkNewRnaResults(roddyBamFile)

        then:
        linkedFiles.each {
            1 * service.fileService.createLink(it, _, _, _) >> { Path link, x, y, z -> }
        }
    }
}
