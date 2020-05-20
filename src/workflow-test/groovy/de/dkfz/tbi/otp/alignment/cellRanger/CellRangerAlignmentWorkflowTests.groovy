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
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.CellRangerFactory
import de.dkfz.tbi.otp.job.processing.JobSubmissionOption
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.SessionUtils

import java.time.Duration

class CellRangerAlignmentWorkflowTests extends AbstractAlignmentWorkflowTest implements CellRangerFactory {

    Sample sample
    SeqType seqType
    CellRangerMergingWorkPackage mwp

    List<String> fastqFiles = [
            "fastqFiles/10x/normal/paired/pbmc_1k_v3_S1_L001_R1_001.fastq.gz",
            "fastqFiles/10x/normal/paired/pbmc_1k_v3_S1_L001_R2_001.fastq.gz",
            "fastqFiles/10x/normal/paired/pbmc_1k_v3_S1_L002_R1_001.fastq.gz",
            "fastqFiles/10x/normal/paired/pbmc_1k_v3_S1_L002_R2_001.fastq.gz",
    ]

    @Override
    void setup() {
        SessionUtils.withNewSession {
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
                    programVersion  : "cellranger/3.0.1",
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

    SeqTrack createSeqTrack(String fastq1, String fastq2) {
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithTwoDataFiles(mwp, [
                seqType              : seqType,
                dataInstallationState: SeqTrack.DataProcessingState.FINISHED,
                sample               : sample,
        ], [:], [:])

        DataFile.findAllBySeqTrack(seqTrack).eachWithIndex { DataFile dataFile, int index ->
            dataFile.vbpFileName = dataFile.fileName = "fastq_${seqTrack.individual.pid}_${seqTrack.sampleType.name}_${seqTrack.laneId}_${index + 1}.fastq.gz"
            dataFile.save(flush: true)
        }

        linkFastqFiles(seqTrack, [
                new File(getInputRootDirectory(), fastq1),
                new File(getInputRootDirectory(), fastq2),
        ])
        return seqTrack
    }

    void checkResults() {
        SessionUtils.withNewSession {
            SingleCellBamFile singleCellBamFile = CollectionUtils.exactlyOneElement(SingleCellBamFile.all)

            assert singleCellBamFile.fileOperationStatus == AbstractMergedBamFile.FileOperationStatus.PROCESSED
            assert singleCellBamFile.fileSize
            assert singleCellBamFile.qualityAssessmentStatus == AbstractBamFile.QaProcessingStatus.FINISHED
            assert singleCellBamFile.overallQualityAssessment
            assert singleCellBamFile.qcTrafficLightStatus == AbstractMergedBamFile.QcTrafficLightStatus.QC_PASSED
        }
    }

    @Unroll
    void "test CellRanger with one lane, with expectedCells #expected and enforcedCells #enforced"() {
        given:
        SessionUtils.withNewSession {
            SeqTrack seqTrack = createSeqTrack(fastqFiles[0], fastqFiles[1])
            mwp.refresh()
            mwp.seqTracks = [seqTrack]
            mwp.expectedCells = expected
            mwp.enforcedCells = enforced
            mwp.save(flush: true)

            seqTrack.dataFilesWhereIndexFileIsFalse.each { DataFile dataFile ->
                DomainFactory.createMetaDataKeyAndEntry(dataFile, MetaDataColumn.SAMPLE_ID.name(), "asdfg")
            }
        }

        when:
        execute()

        then:
        checkResults()

        where:
        expected | enforced
        null     | null
        1000     | null
        null     | 1000
    }

    @Unroll
    void "test CellRanger with two lanes, with expectedCells #expected and enforcedCells #enforced"() {
        given:
        SessionUtils.withNewSession {
            SeqTrack seqTrack1 = createSeqTrack(fastqFiles[0], fastqFiles[1])
            SeqTrack seqTrack2 = createSeqTrack(fastqFiles[2], fastqFiles[3])
            CellRangerMergingWorkPackage crmwp = CellRangerMergingWorkPackage.get(mwp.id)
            crmwp.seqTracks = [seqTrack1, seqTrack2]
            mwp.expectedCells = expected
            mwp.enforcedCells = enforced
            crmwp.save(flush: true)

            crmwp.seqTracks.each { SeqTrack seqTrack ->
                seqTrack.dataFilesWhereIndexFileIsFalse.each { DataFile dataFile ->
                    DomainFactory.createMetaDataKeyAndEntry(dataFile, MetaDataColumn.SAMPLE_ID.name(), "asdfg")
                }
            }
        }

        when:
        execute()

        then:
        checkResults()

        where:
        expected | enforced
        null     | null
        1000     | null
        null     | 1000
    }

    @Override
    List<String> getWorkflowScripts() {
        return [
                "scripts/workflows/CellRangerWorkflow.groovy",
        ]
    }

    @Override
    Duration getTimeout() {
        return Duration.ofHours(6)
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
