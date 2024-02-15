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
package de.dkfz.tbi.otp.workflow.fastqc

import grails.testing.gorm.DataTest
import spock.lang.*

import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.domainFactory.FastqcDomainFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataViewFileService
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
    private RawSequenceFile rawSequenceFile1
    private RawSequenceFile rawSequenceFile2
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

    private void createData(boolean canBeCopied = false) {
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
                apiVersion: createWorkflowApiVersion(workflow: workflow),
                workflowVersion   : '0.1.1',
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
                outputRole  : INPUT_ROLE, // "FASTQ"
                artefactType: ArtefactType.FASTQ,
        ])
        seqTrack = createSeqTrack([
                workflowArtefact: artefact,
        ])
        rawSequenceFile1 = createFastqFile([
                seqTrack: seqTrack,
        ])
        rawSequenceFile2 = createFastqFile([
                seqTrack: seqTrack,
        ])
        fastqcProcessedFile1 = createFastqcProcessedFile([
                sequenceFile: rawSequenceFile1,
                fileCopied  : canBeCopied,
        ])
        fastqcProcessedFile2 = createFastqcProcessedFile([
                sequenceFile     : rawSequenceFile2,
                workDirectoryName: fastqcProcessedFile1.workDirectoryName,
                fileCopied       : canBeCopied,
        ])

        job = new FastqcExecuteWesPipelineJob()
    }

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                FastqcProcessedFile,
                WorkflowArtefact,
                WorkflowStep,
                ProcessingOption,
        ]
    }

    void "getRunSpecificParameters, should return the correct parameters"() {
        given:
        createData()

        final List<Path> inputPaths = [Paths.get('fastq1'), Paths.get('fastq2')]

        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            1 * getOutputArtefacts(step, OUTPUT_ROLE) >> [fastqcProcessedFile1, fastqcProcessedFile2]
        }
        job.rawSequenceDataViewFileService = Mock(RawSequenceDataViewFileService) {
            1 * getFilePath(rawSequenceFile1) >> inputPaths[0]
            1 * getFilePath(rawSequenceFile2) >> inputPaths[1]
            0 * _
        }

        when:
        Map<Path, Map<String, String>> parameters = job.getRunSpecificParameters(step, baseDir)

        then:
        parameters == [
                (baseDir): [
                        input    : inputPaths.join(','),
                        outputDir: baseDir.toString(),
                ]
        ]
    }

    @Unroll
    void "shouldWeskitJobSend, when fastqc reports can be copied, then copy fastqc"() {
        given:
        createData(canBeCopied)

        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            1 * getOutputArtefacts(step, OUTPUT_ROLE) >> [fastqcProcessedFile1, fastqcProcessedFile2]
            0 * _
        }
        job.fastqcReportService = Mock(FastqcReportService) {
            n * copyExistingFastqcReportsNewSystem(_)
            0 * _
        }
        job.logService = Mock(LogService) {
            n * addSimpleLogEntry(step, "fastqc reports found, copy them")
            m * addSimpleLogEntry(step, "no fastqc reports found, create wes call")
            0 * _
        }

        expect:
        job.shouldWeskitJobSend(step) != canBeCopied

        where:
        canBeCopied | n | m
        true        | 1 | 0
        false       | 0 | 1
    }
}
