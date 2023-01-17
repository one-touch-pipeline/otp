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
package de.dkfz.tbi.otp.alignment.cellRanger

import groovy.json.JsonOutput
import spock.lang.Unroll

import de.dkfz.tbi.otp.alignment.AbstractAlignmentWorkflowTest
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerConfigurationService
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.CellRangerFactory
import de.dkfz.tbi.otp.job.processing.JobSubmissionOption
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.SessionUtils

import java.time.Duration

abstract class AbstractCellRangerAlignmentWorkflowTests extends AbstractAlignmentWorkflowTest implements CellRangerFactory {

    static final int CELLS = 1000

    Sample sample
    SeqType seqType
    CellRangerMergingWorkPackage mwp

    CellRangerConfigurationService cellRangerConfigurationService

    List<String> fastqFiles = [
            "fastqFiles/10x/normal/paired/pbmc_1k_v3_S1_L001_R1_001.fastq.gz",
            "fastqFiles/10x/normal/paired/pbmc_1k_v3_S1_L001_R2_001.fastq.gz",
            "fastqFiles/10x/normal/paired/pbmc_1k_v3_S1_L002_R1_001.fastq.gz",
            "fastqFiles/10x/normal/paired/pbmc_1k_v3_S1_L002_R2_001.fastq.gz",
    ]

    abstract Map<String, Integer> getTestParameters()

    @Override
    void setup() {
        SessionUtils.withTransaction {
            Project project = createProject(realm: realm)
            Individual individual = DomainFactory.createIndividual(project: project)
            sample = createSample(individual: individual)

            seqType = createSeqType()

            ToolName toolName = createToolName(path: "cellranger")
            ReferenceGenomeIndex referenceGenomeIndex = createReferenceGenomeIndex(
                    toolName        : toolName,
                    path            : "1.2.0",
                    referenceGenome : createReferenceGenome(path: "hg_GRCh38"),
                    indexToolVersion: "1.2.0",
            )

            ConfigPerProjectAndSeqType conf = createConfig(
                    seqType         : seqType,
                    project         : project,
                    programVersion  : "cellranger/6.1.1",
            )

            mwp = createMergingWorkPackage(
                    needsProcessing     : true,
                    sample              : sample,
                    config              : conf,
                    referenceGenome     : referenceGenomeIndex.referenceGenome,
                    referenceGenomeIndex: referenceGenomeIndex,
            )

            setUpRefGenomeDir(mwp, new File(referenceGenomeDirectory, 'hg_GRCh38'))

            createMergingCriteriaLazy(project: project, seqType: seqType)

            findOrCreatePipeline()
        }
    }

    List<SeqTrack> createNSeqTracks(int n) {
        assert n > 0: "create at least one SeqTrack"
        assert n <= (fastqFiles.size() / 2): "more FastQs expected than provided"
        return (0..(n * 2 - 1)).step(2).collect { Integer i ->
            return createSeqTrack(fastqFiles[i], fastqFiles[i + 1])
        }
    }

    SeqTrack createSeqTrack(String fastq1, String fastq2) {
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithTwoDataFiles(mwp, [
                seqType              : seqType,
                dataInstallationState: SeqTrack.DataProcessingState.FINISHED,
                sample               : sample,
        ], [:], [:])

        DataFile.findAllBySeqTrack(seqTrack).eachWithIndex { DataFile dataFile, int index ->
            dataFile.vbpFileName = dataFile.fileName = "fastq_${seqTrack.individual.pid}_${seqTrack.sampleType.name}_${seqTrack.laneId}_${index + 1}.fastq.gz"
            dataFile.save(flush: true)
            DomainFactory.createMetaDataKeyAndEntry(dataFile, MetaDataColumn.SAMPLE_NAME.name(), "asdfg")
        }

        linkFastqFiles(seqTrack, [
                new File(inputRootDirectory, fastq1),
                new File(inputRootDirectory, fastq2),
        ])
        return seqTrack
    }

    void checkResults() {
        SessionUtils.withTransaction {
            SingleCellBamFile singleCellBamFile = CollectionUtils.exactlyOneElement(SingleCellBamFile.all)

            assert singleCellBamFile.fileOperationStatus == AbstractMergedBamFile.FileOperationStatus.PROCESSED
            assert singleCellBamFile.fileSize
            assert singleCellBamFile.qualityAssessmentStatus == AbstractBamFile.QaProcessingStatus.FINISHED
            assert singleCellBamFile.overallQualityAssessment
            assert singleCellBamFile.qcTrafficLightStatus == AbstractMergedBamFile.QcTrafficLightStatus.UNCHECKED
        }
    }

    @Unroll
    void "test CellRanger with #p.nLanes lanes, with expectedCells #p.expected and enforcedCells #p.enforced"() {
        given:
        SessionUtils.withTransaction {
            mwp.refresh()
            mwp.seqTracks = createNSeqTracks(p.nLanes)
            mwp.expectedCells = p.expected
            mwp.enforcedCells = p.enforced
            mwp.save(flush: true)
        }

        when:
        execute()

        then:
        checkResults()

        when: //check also setting mwp as final
        SessionUtils.withTransaction {
            doWithAuth(ADMIN) {
                cellRangerConfigurationService.selectMwpAsFinal(CellRangerMergingWorkPackage.get(mwp.id))
            }
        }

        then:
        SessionUtils.withTransaction {
            SingleCellBamFile singleCellBamFile = CollectionUtils.exactlyOneElement(SingleCellBamFile.all)

            assert singleCellBamFile.mergingWorkPackage.status == CellRangerMergingWorkPackage.Status.FINAL
            true
        }

        where:
        p << [testParameters]
    }

    @Override
    List<String> getWorkflowScripts() {
        return [
                "scripts/workflows/CellRangerWorkflow.groovy",
        ]
    }

    @Override
    Duration getTimeout() {
        return Duration.ofHours(5)
    }

    @Override
    String getJobSubmissionOptions() {
        JsonOutput.toJson([
                (JobSubmissionOption.WALLTIME): Duration.ofHours(5).toString(),
                (JobSubmissionOption.MEMORY)  : "60g",
                (JobSubmissionOption.CORES)   : "16",
        ])
    }
}
