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

package de.dkfz.tbi.otp.alignment

import groovy.json.JsonOutput

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.CellRangerFactory
import de.dkfz.tbi.otp.job.processing.JobSubmissionOption
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils

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
        Realm.withNewSession {
            Project project = createProject(realm: realm)
            Individual individual = DomainFactory.createIndividual(project: project)
            sample = createSample(individual: individual)

            seqType = createSeqType()

            ToolName toolName = createToolName(path: "cellranger")
            ReferenceGenome referenceGenome = createReferenceGenome(path: "hg_GRCh38")
            ReferenceGenomeIndex referenceGenomeIndex = createReferenceGenomeIndex(
                    toolName: toolName,
                    path: "1.2.0",
                    referenceGenome: referenceGenome,
                    indexToolVersion: "1.2.0",
            )

            ConfigPerProjectAndSeqType conf = createConfig(
                    seqType: seqType,
                    project: project,
                    programVersion: "cellranger/3.0.1",
                    referenceGenomeIndex: referenceGenomeIndex,
            )

            mwp = createMergingWorkPackage(
                    needsProcessing: true,
                    sample: sample,
                    config: conf,
                    expectedCells: 1000, //according to 10x
                    referenceGenome: referenceGenome,
            )

            setUpRefGenomeDir(mwp, new File(referenceGenomeDirectory, 'hg_GRCh38'))

            DomainFactory.createMergingCriteriaLazy(project: project, seqType: seqType)

            findOrCreatePipeline()
        }
    }

    SeqTrack createSeqTrack(String fastq1, String fastq2) {
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithTwoDataFiles(mwp, [
                seqType: seqType,
                dataInstallationState: SeqTrack.DataProcessingState.FINISHED,
                sample: sample,
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
        Realm.withNewSession {
            SingleCellBamFile singleCellBamFile = CollectionUtils.exactlyOneElement(SingleCellBamFile.all)

            assert singleCellBamFile.fileOperationStatus == AbstractMergedBamFile.FileOperationStatus.PROCESSED
            assert singleCellBamFile.fileSize
            assert singleCellBamFile.qualityAssessmentStatus == AbstractBamFile.QaProcessingStatus.FINISHED
            assert singleCellBamFile.overallQualityAssessment
            assert singleCellBamFile.qcTrafficLightStatus == AbstractMergedBamFile.QcTrafficLightStatus.QC_PASSED
        }
    }

    void "test CellRanger with one lane"() {
        given:
        Realm.withNewSession {
            SeqTrack seqTrack = createSeqTrack(fastqFiles[0], fastqFiles[1])
            mwp.refresh()
            mwp.seqTracks = [seqTrack]
            mwp.save(flush: true)
        }

        when:
        execute()

        then:
        checkResults()
    }

    void "test CellRanger with two lanes"() {
        given:
        Realm.withNewSession {
            SeqTrack seqTrack1 = createSeqTrack(fastqFiles[0], fastqFiles[1])
            SeqTrack seqTrack2 = createSeqTrack(fastqFiles[2], fastqFiles[3])
            CellRangerMergingWorkPackage crmwp = CellRangerMergingWorkPackage.get(mwp.id)
            crmwp.seqTracks = [seqTrack1, seqTrack2]
            crmwp.save(flush: true)
        }

        when:
        execute()

        then:
        checkResults()
    }

    @Override
    List<String> getWorkflowScripts() {
        return [
                "scripts/workflows/CellRangerWorkflow.groovy",
        ]
    }

    @Override
    Duration getTimeout() {
        return Duration.ofHours(3)
    }

    @Override
    String getJobSubmissionOptions() {
        JsonOutput.toJson([
                (JobSubmissionOption.WALLTIME): Duration.ofHours(3).toString(),
                (JobSubmissionOption.MEMORY)  : "60g",
                (JobSubmissionOption.CORES)  : "16",
        ])
    }
}
