/*
 * Copyright 2011-2019 The OTP authors
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


import grails.testing.gorm.DataTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import de.dkfz.tbi.otp.Comment
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightCheckService
import de.dkfz.tbi.otp.utils.*

import java.nio.file.Path

class LinkFilesToFinalDestinationServiceSpec extends Specification implements IsRoddy, DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                AbstractMergedBamFile,
                Comment,
                DataFile,
                FileType,
                Individual,
                LibraryPreparationKit,
                MergingCriteria,
                MergingWorkPackage,
                Pipeline,
                ProcessingOption,
                Project,
                Realm,
                ReferenceGenome,
                ReferenceGenomeProjectSeqType,
                RoddyBamFile,
                RoddyMergedBamQa,
                RoddyWorkflowConfig,
                RoddyQualityAssessment,
                Run,
                FastqImportInstance,
                Sample,
                SampleType,
                SeqCenter,
                SeqPlatform,
                SeqPlatformGroup,
                SeqPlatformModelLabel,
                SeqTrack,
                SeqType,
                SoftwareTool,
        ]
    }

    @Rule
    TemporaryFolder temporaryFolder = new TemporaryFolder()

    void "linkToFinalDestinationAndCleanup, all Fine"() {
        given:
        new TestConfigService(temporaryFolder.newFolder())
        final String md5sum = HelperUtils.randomMd5sum

        RoddyBamFile roddyBamFile = createBamFile([
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.NEEDS_PROCESSING,
                md5sum             : null,
                fileSize           : -1,
        ])
        Realm realm = roddyBamFile.realm

        DomainFactory.createRoddyProcessingOptions(temporaryFolder.newFolder())

        LinkFilesToFinalDestinationService linkFilesToFinalDestinationService = new LinkFilesToFinalDestinationService([
                lsdfFilesService            : Mock(LsdfFilesService) {
                    1 * deleteFilesRecursive(realm, _) >> { Realm realm2, Collection<File> filesOrDirectories ->
                        String basePath = roddyBamFile.workDirectory.path
                        filesOrDirectories.each { File file ->
                            assert file.path.startsWith(basePath)
                        }
                    }
                },
                executeRoddyCommandService  : Mock(ExecuteRoddyCommandService) {
                    1 * correctPermissionsAndGroups(roddyBamFile, realm)
                },
                qcTrafficLightCheckService  : Mock(QcTrafficLightCheckService) {
                    1 * handleQcCheck(roddyBamFile, _) >> { AbstractMergedBamFile bamFile, Closure callbackIfAllFine ->
                        bamFile.workDirectory.mkdirs()
                        bamFile.workBamFile.text = "something"
                    }
                },
                md5SumService               : Mock(Md5SumService) {
                    1 * extractMd5Sum(_ as Path) >> md5sum
                },
                abstractMergedBamFileService: Mock(AbstractMergedBamFileService) {
                    1 * updateSamplePairStatusToNeedProcessing(roddyBamFile)
                },
        ])

        when:
        linkFilesToFinalDestinationService.linkToFinalDestinationAndCleanup(roddyBamFile, realm)

        then:
        roddyBamFile.fileOperationStatus == AbstractMergedBamFile.FileOperationStatus.PROCESSED
        roddyBamFile.md5sum == md5sum
        roddyBamFile.fileSize > 0
        roddyBamFile.fileExists
        roddyBamFile.dateFromFileSystem != null
        roddyBamFile.dateFromFileSystem instanceof Date
    }
}
