/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.otp.workflow.fastqc

import grails.testing.gorm.DataTest
import spock.lang.*

import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.domainFactory.FastqcDomainFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.filestore.FilestoreService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflowExecution.*

import java.nio.file.*

class FastqcExecuteWesPipelineJobSpec extends Specification implements DataTest, FastqcDomainFactory, WorkflowSystemDomainFactory {

    static final String WORKFLOW = WesFastQcWorkflow.WORKFLOW
    static final String INPUT_ROLE = WesFastQcWorkflow.INPUT_FASTQ
    static final String OUTPUT_ROLE = WesFastQcWorkflow.OUTPUT_FASTQC

    private FastqcExecuteWesPipelineJob job
    private WorkflowRun run
    private WorkflowStep step
    private WorkflowArtefact artefact
    private SeqTrack seqTrack
    private FileSystem fileSystem = FileSystems.default
    private DataFile dataFile1
    private DataFile dataFile2
    private FastqcProcessedFile fastqcProcessedFile1
    private FastqcProcessedFile fastqcProcessedFile2
    private Workflow workflow
    private WorkflowVersion version

    private Path sourceDir
    private Path targetDir
    private Path sourceFastqc1
    private Path sourceFastqc2
    private Path sourceFastqcMd5sum1
    private Path sourceFastqcMd5sum2
    private Path targetFastqc1
    private Path targetFastqc2

    @TempDir
    Path tempDir
    Path baseDir

    private void createData() {
        fileSystem = FileSystems.default
        baseDir = tempDir.resolve("base")
        sourceDir = tempDir.resolve("src")
        targetDir = tempDir.resolve("tgt")
        sourceFastqc1 = sourceDir.resolve('fastq1')
        sourceFastqc2 = sourceDir.resolve('fastq2')
        sourceFastqcMd5sum1 = sourceDir.resolve('md5sum1')
        sourceFastqcMd5sum2 = sourceDir.resolve('md5sum2')
        targetFastqc1 = targetDir.resolve('fastq1')
        targetFastqc2 = targetDir.resolve('fastq2')

        workflow = createWorkflow([
                name: WORKFLOW
        ])
        version = createWorkflowVersion([
                workflow       : workflow,
                workflowVersion: '0.1.1',
        ])
        run = createWorkflowRun([
                workflow       : workflow,
                workflowVersion: version,
                workDirectory  : tempDir.resolve("workflowrun_${nextId}"),
        ])
        step = createWorkflowStep([
                workflowRun: run,
        ])
        artefact = createWorkflowArtefact([
                producedBy  : run,
                outputRole  : INPUT_ROLE, //"FASTQ"
                artefactType: ArtefactType.FASTQ,
        ])
        seqTrack = createSeqTrack([
                workflowArtefact: artefact,
        ])
        dataFile1 = createDataFile([
                seqTrack: seqTrack,
        ])
        dataFile2 = createDataFile([
                seqTrack: seqTrack,
        ])
        fastqcProcessedFile1 = createFastqcProcessedFile([
                dataFile: dataFile1,
        ])
        fastqcProcessedFile2 = createFastqcProcessedFile([
                dataFile         : dataFile2,
                workDirectoryName: fastqcProcessedFile1.workDirectoryName,
        ])

        job = new FastqcExecuteWesPipelineJob()
    }

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqcProcessedFile,
                WorkflowArtefact,
                WorkflowStep,
                Realm,
                ProcessingOption,
        ]
    }

    void "getRunSpecificParameters, should return the correct parameters"() {
        given:
        createData()

        final List<Path> inputPaths  = [ Paths.get('fastq1'),      Paths.get('fastq2')]
        final List<Path> outputPaths = [ baseDir.resolve(0.toString()), baseDir.resolve(1.toString())]

        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            1 * getOutputArtefacts(step, OUTPUT_ROLE) >> [fastqcProcessedFile1, fastqcProcessedFile2]
        }
        job.lsdfFilesService = Mock(LsdfFilesService) {
            1 * getFileViewByPidPathAsPath(dataFile1) >> Paths.get('fastq1')
            1 * getFileViewByPidPathAsPath(dataFile2) >> Paths.get('fastq2')
            0 * _
        }

        when:
        Map<Path, Map<String, String>> parameters = job.getRunSpecificParameters(step, baseDir)

        then:
        parameters.size() == 2
        parameters.eachWithIndex { Path path, Map<String, String> param, idx ->
            assert param["input"] == inputPaths[idx].toString()
            assert param["outputDir"] == outputPaths[idx].toString()
            assert path == outputPaths[idx]
        }
    }

    @Unroll
    void "shouldWeskitJobSend, when fastqc reports can be copied, then copy fastqc"() {
        given:
        createData()

        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            1 * getOutputArtefacts(step, OUTPUT_ROLE) >> [fastqcProcessedFile1, fastqcProcessedFile2]
        }
        job.fastqcReportService = Mock(FastqcReportService) {
            1 * canFastqcReportsBeCopied([fastqcProcessedFile1, fastqcProcessedFile2]) >> canBeCopied
            n * copyExistingFastqcReports(step.realm, _, _)
        }

        job.filestoreService = Mock(FilestoreService) {
            n * getWorkFolderPath(step.workflowRun)
        }
        job.logService = Mock(LogService) {
            n * addSimpleLogEntry(step, "Copying fastqc reports for Weskit")
        }

        expect:
        job.shouldWeskitJobSend(step) != canBeCopied

        where:
        canBeCopied | n
        true        | 1
        false       | 0
    }
}
