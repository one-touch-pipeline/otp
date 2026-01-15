/*
 * Copyright 2011-2026 The OTP authors
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
import grails.testing.services.ServiceUnitTest
import spock.lang.*

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqInstance
import de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqLinkFileService
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelCallingInstance
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelLinkFileService
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaInstance
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaLinkFileService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvLinkFileService
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaLinkFileService
import de.dkfz.tbi.otp.domainFactory.pipelines.analysis.AbstractAnalysisDomainFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.filestore.FilestoreService
import de.dkfz.tbi.otp.infrastructure.alignment.AlignmentWorkFileServiceFactoryService
import de.dkfz.tbi.otp.infrastructure.alignment.PanCancerWorkFileService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.exceptions.FileInconsistencyException
import de.dkfz.tbi.otp.workflowExecution.WorkflowVersion

import java.nio.file.Files
import java.nio.file.Path

abstract class AbstractAnalysisWorkFileServiceSpec<T extends AbstractAnalysisWorkFileService> extends Specification implements ServiceUnitTest<T>, DataTest, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                AceseqInstance,
                FastqFile,
                FastqImportInstance,
                FileType,
                IndelCallingInstance,
                MergingWorkPackage,
                RoddyBamFile,
                SnvCallingInstance,
                RoddyWorkflowConfig,
                RunYapsaInstance,
                SampleTypePerProject,
                SophiaInstance,
        ]
    }

    @TempDir
    Path tempDir

    void "getWorkflowDirectoryName returns correct directory name"() {
        expect:
        service.workflowDirectoryName == workflowDirName
    }

    void "constructInstanceName returns correct instance name"() {
        given:
        TestConfigService testConfigService = new TestConfigService()
        service.configService = testConfigService

        and: 'fixed clock'
        testConfigService.fixClockTo(2000, 1, 1, 0, 0, 0)

        and: 'create workflow version'
        WorkflowVersion workflowVersion = new WorkflowVersion([
                workflowVersion: 'workflowVersion',
        ])

        and: 'define expected value'
        String expected = [
                "results",
                "_",
                workflowDirName,
                "-",
                workflowVersion.workflowVersion,
                "_",
                "2000-01-01-00-00-00",
        ].join('')

        expect:
        service.constructInstanceName(workflowVersion) == expected
    }

    @Unroll
    void "test getDirectoryPath for newWorkflowSystem #newWorkflowSystem"() {
        given:
        service.filestoreService = Mock(FilestoreService) {
            countUuid * getWorkFolderPath(_) >> tempDir.resolve("uuid")
        }
        IndividualService individualService = Mock(IndividualService) {
            countOldSystem * getViewByPidPath(_, _) >> tempDir.resolve("view-by-pid")
        }
        service.analysisLinkFileServiceFactoryService = new AnalysisLinkFileServiceFactoryService([
                aceseqLinkFileService  : new AceseqLinkFileService(individualService: individualService),
                indelLinkFileService   : new IndelLinkFileService(individualService: individualService),
                runYapsaLinkFileService: new RunYapsaLinkFileService(individualService: individualService),
                snvLinkFileService     : new SnvLinkFileService(individualService: individualService),
                sophiaLinkFileService  : new SophiaLinkFileService(individualService: individualService),
        ])
        BamFilePairAnalysis instance = getInstance(newWorkflowSystem)

        expect:
        service.getDirectoryPath(instance) == newWorkflowSystem ?
                tempDir.resolve("uuid") :
                tempDir.resolve("view-by-pid")

        where:
        newWorkflowSystem | countUuid | countOldSystem
        true              | 1         | 0
        false             | 0         | 1
    }

    void "validateInputBamFiles, when all fine, then do not throw exception"() {
        given:
        BamFilePairAnalysis analysis = setUpForValidateInputBamFiles(true)

        when:
        service.validateInputBamFiles(analysis)

        then:
        noExceptionThrown()
    }

    @Unroll
    @IgnoreIf({ System.getProperty("os.name").toLowerCase().contains("windows") })
    void "validateInputBamFiles, when #name, then throw exception"() {
        given:
        BamFilePairAnalysis analysis = setUpForValidateInputBamFiles(!firstBamFile)
        changeClosure(analysis, tempDir)
        String bamFileWithProblem = firstBamFile ? analysis.sampleType1BamFile : analysis.sampleType2BamFile

        when:
        service.validateInputBamFiles(analysis)

        then:
        FileInconsistencyException e = thrown()
        e.message.contains(bamFileWithProblem)
        e.cause.message.contains(assertMessagePart)

        where:
        name                           | changeClosure                                                                                                                                       || firstBamFile | assertMessagePart
        'md5sum changed of bam 1'      | { BamFilePairAnalysis analysis2, Path tempDir2 -> analysis2.sampleType1BamFile.md5sum = "0" }                                                       || true         | "bamFile.md5sum"
        'md5sum changed of bam 2'      | { BamFilePairAnalysis analysis2, Path tempDir2 -> analysis2.sampleType2BamFile.md5sum = "0" }                                                       || false        | "bamFile.md5sum"
        'file size of bam 1 is 0'      | { BamFilePairAnalysis analysis2, Path tempDir2 -> analysis2.sampleType1BamFile.fileSize = 0 }                                                       || true         | "bamFile.fileSize"
        'file size of bam 2 is 0'      | { BamFilePairAnalysis analysis2, Path tempDir2 -> analysis2.sampleType2BamFile.fileSize = 0 }                                                       || false        | "bamFile.fileSize"
        'file size of bam 1 is wrong'  | { BamFilePairAnalysis analysis2, Path tempDir2 -> analysis2.sampleType1BamFile.fileSize = 1 }                                                       || true         | "Files.size"
        'file size of bam 2 is wrong'  | { BamFilePairAnalysis analysis2, Path tempDir2 -> analysis2.sampleType2BamFile.fileSize = 1 }                                                       || false        | "Files.size"
        'file of bam 1 is not a file'  | { BamFilePairAnalysis analysis2, Path tempDir2 -> Files.delete(tempDir2.resolve("bam1.bam")); Files.createDirectory(tempDir2.resolve("bam1.bam")) } || true         | "Files.isRegularFile" // codenarc-disable-line ExplicitFlushForDeleteRule
        'file of bam 2 is not a file'  | { BamFilePairAnalysis analysis2, Path tempDir2 -> Files.delete(tempDir2.resolve("bam2.bam")); Files.createDirectory(tempDir2.resolve("bam2.bam")) } || false        | "Files.isRegularFile" // codenarc-disable-line ExplicitFlushForDeleteRule
        'file of bam 1 is not a file'  | { BamFilePairAnalysis analysis2, Path tempDir2 -> Files.setPosixFilePermissions(tempDir2.resolve("bam1.bam"), [] as Set) }                          || true         | "Files.isReadable"
        'file of bam 2 is not a file'  | { BamFilePairAnalysis analysis2, Path tempDir2 -> Files.setPosixFilePermissions(tempDir2.resolve("bam2.bam"), [] as Set) }                          || false        | "Files.isReadable"
        'file of bam 1 does not exist' | { BamFilePairAnalysis analysis2, Path tempDir2 -> Files.delete(tempDir2.resolve("bam1.bam")) }                                                      || true         | "Files.exists" // codenarc-disable-line ExplicitFlushForDeleteRule
        'file of bam 2 does not exist' | { BamFilePairAnalysis analysis2, Path tempDir2 -> Files.delete(tempDir2.resolve("bam2.bam")) }                                                      || false        | "Files.exists" // codenarc-disable-line ExplicitFlushForDeleteRule
    }

    protected BamFilePairAnalysis getInstance(boolean newWorkflowSystem) {
        return factory.createInstanceWithRoddyBamFiles([
                workflowArtefact: createWorkflowArtefact([
                        producedBy: createWorkflowRun([
                                workFolder: newWorkflowSystem ? createWorkFolder() : null,
                        ])
                ])
        ])
    }

    private BamFilePairAnalysis setUpForValidateInputBamFiles(boolean calledForBoth) {
        BamFilePairAnalysis analysis = getInstance(false)

        service.alignmentWorkFileServiceFactoryService = Mock(AlignmentWorkFileServiceFactoryService) {
            0 * _
        }

        [
                1: analysis.sampleType1BamFile,
                2: analysis.sampleType2BamFile,
        ].collect { int i, AbstractBamFile bamFile ->
            Path file = tempDir.resolve("bam${i}.bam")
            file.text = "file${i}"
            bamFile.fileSize = Files.size(file)

            if (i == 1 || calledForBoth) {
                1 * service.alignmentWorkFileServiceFactoryService.getService(bamFile) >> Mock(PanCancerWorkFileService) {
                    1 * getBamFile(bamFile) >> file
                    0 * _
                }
            }
        }
        return analysis
    }

    abstract protected String getWorkflowDirName()

    abstract protected AbstractAnalysisDomainFactory getFactory()
}
