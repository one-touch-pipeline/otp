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
import de.dkfz.tbi.otp.dataprocessing.snvcalling.RoddySnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvLinkFileService
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaLinkFileService
import de.dkfz.tbi.otp.domainFactory.pipelines.analysis.AbstractAnalysisDomainFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.filestore.FilestoreService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.workflowExecution.WorkflowVersion

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
                RoddySnvCallingInstance,
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
            _ * getWorkFolderPath(_) >> tempDir.resolve("uuid")
        }
        IndividualService individualService = Mock(IndividualService) {
            _ * getViewByPidPath(_, _) >> tempDir.resolve("view-by-pid")
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
        newWorkflowSystem << [true, false]
    }

    protected BamFilePairAnalysis getInstance(boolean newWorkflowSystem) {
        return factory.createInstanceWithRoddyBamFiles(
                [workflowArtefact: createWorkflowArtefact(newWorkflowSystem ?
                        [producedBy: createWorkflowRun(workFolder: createWorkFolder())] :
                        [:])
                ]
        )
    }

    abstract protected String getWorkflowDirName()

    abstract protected AbstractAnalysisDomainFactory getFactory()
}
