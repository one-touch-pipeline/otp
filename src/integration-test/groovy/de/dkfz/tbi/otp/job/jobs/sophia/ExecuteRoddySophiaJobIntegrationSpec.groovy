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
package de.dkfz.tbi.otp.job.jobs.sophia

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*

import java.nio.file.FileSystems
import java.nio.file.Path

@Rollback
@Integration
class ExecuteRoddySophiaJobIntegrationSpec extends Specification {

    TestConfigService configService

    @Rule
    TemporaryFolder temporaryFolder

    void "prepareAndReturnWorkflowSpecificCValues, when roddySophiaInstance is null, throw assert"() {
        when:
        new ExecuteRoddySophiaJob().prepareAndReturnWorkflowSpecificCValues(null)

        then:
        AssertionError e = thrown()
        e.message.contains('assert sophiaInstance')
    }

    void "prepareAndReturnWorkflowSpecificCValues, when all fine, return correct value list"() {
        given:
        Path path = temporaryFolder.newFolder().toPath()
        configService.addOtpProperties(path)
        IndividualService individualService = Mock(IndividualService) {
            getViewByPidPath(_, _) >> path
        }
        ExecuteRoddySophiaJob job = new ExecuteRoddySophiaJob([
                sophiaService    : Spy(SophiaService) {
                    1 * validateInputBamFiles(_) >> { }
                },
                fileSystemService: Mock(FileSystemService) {
                    _ * getRemoteFileSystem(_) >> FileSystems.default
                },
                fileService      : new FileService([
                        remoteShellHelper: Mock(RemoteShellHelper) {
                            _ * executeCommandReturnProcessOutput(_, _) >> { Realm realm, String command ->
                                return new ProcessOutput(command, '', 0)
                            }
                        }
                ]),
        ])
        job.sophiaService.individualService = individualService

        SophiaInstance sophiaInstance = DomainFactory.createSophiaInstanceWithRoddyBamFiles()

        RoddyBamFile bamFileDisease = sophiaInstance.sampleType1BamFile as RoddyBamFile
        RoddyBamFile bamFileControl = sophiaInstance.sampleType2BamFile as RoddyBamFile
        RoddyMergedBamQa bamFileDiseaseMergedBamQa = DomainFactory.createRoddyMergedBamQa(bamFileDisease)
        RoddyMergedBamQa bamFileControlMergedBamQa = DomainFactory.createRoddyMergedBamQa(bamFileControl)

        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(bamFileDisease)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(bamFileControl)
        CreateRoddyFileHelper.createInsertSizeFiles(sophiaInstance)

        bamFileDisease.mergingWorkPackage.bamFileInProjectFolder = bamFileDisease
        assert bamFileDisease.mergingWorkPackage.save(flush: true)

        bamFileControl.mergingWorkPackage.bamFileInProjectFolder = bamFileControl
        assert bamFileControl.mergingWorkPackage.save(flush: true)

        Path finalBamFileControlPath = job.sophiaService.getWorkDirectory(sophiaInstance).resolve("${bamFileControl.sampleType.dirName}_${bamFileControl.individual.pid}_merged.mdup.bam")
        Path finalBamFileDiseasePath = job.sophiaService.getWorkDirectory(sophiaInstance).resolve("${bamFileDisease.sampleType.dirName}_${bamFileDisease.individual.pid}_merged.mdup.bam")

        List<String> expectedList = [
                "controlMedianIsize:${bamFileControlMergedBamQa.insertSizeMedian}",
                "tumorMedianIsize:${bamFileDiseaseMergedBamQa.insertSizeMedian}",
                "controlStdIsizePercentage:${bamFileControlMergedBamQa.insertSizeCV}",
                "tumorStdIsizePercentage:${bamFileDiseaseMergedBamQa.insertSizeCV}",
                "controlProperPairPercentage:${bamFileControlMergedBamQa.percentProperlyPaired}",
                "tumorProperPairPercentage:${bamFileDiseaseMergedBamQa.percentProperlyPaired}",
                "bamfile_list:${finalBamFileControlPath};${finalBamFileDiseasePath}",
                "sample_list:${bamFileControl.sampleType.dirName};${bamFileDisease.sampleType.dirName}",
                "insertsizesfile_list:${bamFileControl.finalInsertSizeFile};${bamFileDisease.finalInsertSizeFile}",
                "possibleTumorSampleNamePrefixes:${bamFileDisease.sampleType.dirName}",
                "possibleControlSampleNamePrefixes:${bamFileControl.sampleType.dirName}",
                "controlDefaultReadLength:${bamFileControl.maximalReadLength}",
                "tumorDefaultReadLength:${bamFileDisease.maximalReadLength}",
                "selectSampleExtractionMethod:version_2",
                "matchExactSampleName:true",
                "allowSampleTerminationWithIndex:false",
                "useLowerCaseFilenameForSampleExtraction:false",
        ]

        when:
        List<String> returnedList = job.prepareAndReturnWorkflowSpecificCValues(sophiaInstance)

        then:
        expectedList == returnedList

        cleanup:
        configService.clean()
    }

    @Unroll
    void "prepareAndReturnWorkflowSpecificParameter, return always empty String"() {
        expect:
        new ExecuteRoddySophiaJob().prepareAndReturnWorkflowSpecificParameter(value).empty

        where:
        value << [
                null,
                new SophiaInstance(),
        ]
    }

    void "validate, when all fine, set processing state to finished"() {
        given:
        Path path = temporaryFolder.newFolder().toPath()
        configService.addOtpProperties(path)
        IndividualService individualService = Mock(IndividualService) {
            getViewByPidPath(_, _) >> path
        }
        ExecuteRoddySophiaJob job = new ExecuteRoddySophiaJob([
                configService             : configService,
                executeRoddyCommandService: Mock(ExecuteRoddyCommandService) {
                    1 * correctPermissionsAndGroups(_, _)
                },
                sophiaService             : Spy(SophiaService) {
                    1 * validateInputBamFiles(_) >> { }
                },
        ])
        job.sophiaService.individualService = individualService

        SophiaInstance sophiaInstance = DomainFactory.createSophiaInstanceWithRoddyBamFiles()

        CreateRoddyFileHelper.createSophiaResultFiles(sophiaInstance, individualService)

        when:
        job.validate(sophiaInstance)

        then:
        sophiaInstance.processingState == AnalysisProcessingStates.FINISHED

        cleanup:
        configService.clean()
    }

    void "validate, when sophiaInstance is null, throw assert"() {
        when:
        new ExecuteRoddySophiaJob().validate(null)

        then:
        AssertionError e = thrown()
        e.message.contains('The input sophiaInstance must not be null. Expression')
    }

    void "validate, when correctPermissionsAndGroups fail, throw assert"() {
        given:
        Path path = temporaryFolder.newFolder().toPath()
        configService.addOtpProperties(path)
        IndividualService individualService = Mock(IndividualService) {
            getViewByPidPath(_, _) >> path
        }
        String md5sum = HelperUtils.uniqueString
        ExecuteRoddySophiaJob job = new ExecuteRoddySophiaJob([
                configService             : configService,
                executeRoddyCommandService: Mock(ExecuteRoddyCommandService) {
                    1 * correctPermissionsAndGroups(_, _) >> {
                        throw new AssertionError(md5sum)
                    }
                },
        ])
        SophiaInstance sophiaInstance = DomainFactory.createSophiaInstanceWithRoddyBamFiles()

        CreateRoddyFileHelper.createSophiaResultFiles(sophiaInstance, individualService)

        when:
        job.validate(sophiaInstance)

        then:
        AssertionError e = thrown()
        e.message.contains(md5sum)
        sophiaInstance.processingState != AnalysisProcessingStates.FINISHED

        cleanup:
        configService.clean()
    }

    @Unroll
    void "validate, when file not exist, throw assert"() {
        given:
        Path path = temporaryFolder.newFolder().toPath()
        configService.addOtpProperties(path)
        IndividualService individualService = Mock(IndividualService) {
            getViewByPidPath(_, _) >> path
        }
        ExecuteRoddySophiaJob job = new ExecuteRoddySophiaJob([
                configService             : configService,
                executeRoddyCommandService: Mock(ExecuteRoddyCommandService) {
                    1 * correctPermissionsAndGroups(_, _)
                },
        ])
        job.sophiaService = new SophiaService()
        job.sophiaService.individualService = individualService

        SophiaInstance sophiaInstance = DomainFactory.createSophiaInstanceWithRoddyBamFiles()

        CreateRoddyFileHelper.createSophiaResultFiles(sophiaInstance, individualService)

        Path fileToDelete = fileClousure(sophiaInstance, job.sophiaService)
        new FileService().deleteDirectoryRecursively(fileToDelete)

        when:
        job.validate(sophiaInstance)

        then:
        AssertionError e = thrown()
        e.message.contains(fileToDelete.toString())
        sophiaInstance.processingState != AnalysisProcessingStates.FINISHED

        cleanup:
        configService.clean()

        where:
        fileClousure << [
                { SophiaInstance it, SophiaService service ->
                    it.workExecutionStoreDirectory.toPath()
                },
                { SophiaInstance it, SophiaService service ->
                    it.workExecutionDirectories.first().toPath()
                },
                { SophiaInstance it, SophiaService service ->
                    service.getFinalAceseqInputFile(it)
                },
        ]
    }
}
