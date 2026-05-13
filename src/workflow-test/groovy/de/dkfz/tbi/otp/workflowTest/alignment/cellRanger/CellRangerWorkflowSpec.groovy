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
package de.dkfz.tbi.otp.workflowTest.alignment.cellRanger

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.CellRangerFactory
import de.dkfz.tbi.otp.infrastructure.alignment.CellRangerLinkFileService
import de.dkfz.tbi.otp.job.processing.JobSubmissionOption
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.SessionUtils
import de.dkfz.tbi.otp.workflow.alignment.cellRanger.CellRangerWorkflow
import de.dkfz.tbi.otp.workflowExecution.OtpWorkflow
import de.dkfz.tbi.otp.workflowExecution.decider.AbstractWorkflowDecider
import de.dkfz.tbi.otp.workflowExecution.decider.CellRangerDecider
import de.dkfz.tbi.otp.workflowTest.alignment.AbstractAlignmentWorkflowSpec
import de.dkfz.tbi.otp.workflowTest.referenceGenome.ReferenceGenomeHg38CellRanger

import java.time.Duration

/**
 * class for all CellRanger workflow tests
 */
class CellRangerWorkflowSpec extends AbstractAlignmentWorkflowSpec implements ReferenceGenomeHg38CellRanger, CellRangerFactory {

    // @Slf4j does not work with Spock containing tests and produces problems in closures
    @SuppressWarnings('PropertyName')
    final static Logger log = LoggerFactory.getLogger(CellRangerWorkflowSpec)

    CellRangerDecider cellRangerDecider

    CellRangerLinkFileService cellRangerLinkFileService

    protected ReferenceGenomeIndex referenceGenomeIndex

    Duration runningTimeout = Duration.ofHours(24)

    Class<? extends OtpWorkflow> workflowComponentClass = CellRangerWorkflow

    @Override
    protected AbstractWorkflowDecider getDecider() {
        return cellRangerDecider
    }

    @Override
    String getWorkflowName() {
        return CellRangerWorkflow.WORKFLOW
    }

    @Override
    void setup() {
        log.debug("Start setup ${this.class.simpleName}")
        SessionUtils.withTransaction {
            setUpFilesVariables()

            setUpDomainVariables()

            linkReferenceGenomeDirectoryToReference(referenceGenome)
        }
        log.debug("Finish setup ${this.class.simpleName}")
    }

    void "test CellRanger with default settings"() {
        given:
        SessionUtils.withTransaction {
            createSeqTrack("readGroup1")
            decide(1, 1)
        }

        when:
        execute()

        then:
        verify_AlignLanesOnly_AllFine()
    }

    void "test align lanes only, two lanes, all fine"() {
        given:
        SeqTrack firstSeqTrack
        SeqTrack secondSeqTrack

        SessionUtils.withTransaction {
            firstSeqTrack = createSeqTrack("readGroup1")
            secondSeqTrack = createSeqTrack("readGroup2")
            decide(2, 1)
        }

        when:
        execute()

        then:
        verify_alignLanesOnly_TwoLanes(firstSeqTrack, secondSeqTrack)
    }

    @Override
    protected void assertBaseFileSystemState(AbstractBamFile bamFile) {
        SingleCellBamFile scBamFile = bamFile as SingleCellBamFile
        cellRangerLinkFileService.getLinkedResultFiles(scBamFile).each {
            fileAssertHelper.assertPathIsReadable(it)
        }
        verifyInputIsNotDeleted()
    }

    @Override
    protected void checkQC(AbstractBamFile bamFile) { }

    @Override
    protected void checkBamFileConfig(AbstractBamFile bamFile) { }

    protected void setUpFilesVariables() {
        testFastqFiles = [
                readGroup1: [
                        referenceDataDirectory.resolve('fastqFiles/10x/small/test_10x_sc3_v3_5k_a549_gex_l1_fastq1.fastq.gz'),
                        referenceDataDirectory.resolve('fastqFiles/10x/small/test_10x_sc3_v3_5k_a549_gex_l1_fastq2.fastq.gz'),
                ].asImmutable(),
                readGroup2: [
                        referenceDataDirectory.resolve('fastqFiles/10x/small/test_10x_sc3_v3_5k_a549_gex_l2_fastq1.fastq.gz'),
                        referenceDataDirectory.resolve('fastqFiles/10x/small/test_10x_sc3_v3_5k_a549_gex_l2_fastq2.fastq.gz'),
                ].asImmutable(),
        ].asImmutable()
    }

    @Override
    protected void setUpDomainVariables() {
        super.setUpDomainVariables()

        ToolName toolName = createToolName(name: 'CELL_RANGER', type: ToolName.Type.SINGLE_CELL, path: "cellranger")
        referenceGenomeIndex = createReferenceGenomeIndex(
                toolName        : toolName,
                path            : "1.2.0",
                referenceGenome : referenceGenome,
                indexToolVersion: "1.2.0",
        )
        log.info("Create ReferenceGenomeIndex ${referenceGenomeIndex}")
    }

    @Override
    protected Map<JobSubmissionOption, String> getJobSubmissionOptions() {
        return [
                (JobSubmissionOption.WALLTIME): Duration.ofHours(5).toString(),
                (JobSubmissionOption.MEMORY)  : "60",
                (JobSubmissionOption.CORES)   : "16",
        ]
    }

    @Override
    protected boolean isFastQcRequired() {
        return false
    }

    @Override
    protected SeqType findSeqType() {
        return SeqTypeService.'10xSingleCellRnaSeqType'
    }
}
