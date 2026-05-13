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
package de.dkfz.tbi.otp.workflow.alignment.cellRanger

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerConfig
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.CellRangerFactory
import de.dkfz.tbi.otp.infrastructure.alignment.CellRangerWorkFileService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.workflow.alignment.AlignmentFragmentJob
import de.dkfz.tbi.otp.workflow.jobs.*

class CellRangerWorkflowSpec extends Specification implements CellRangerFactory, DataTest {

    CellRangerWorkflow cellRangerWorkflow

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                Pipeline,
                CellRangerMergingWorkPackage,
                SingleCellBamFile,
                FastqImportInstance,
                ReferenceGenomeProjectSeqType,
                CellRangerConfig,
        ]
    }

    void setup() {
        cellRangerWorkflow = new CellRangerWorkflow()
    }

    void "getJobList, should return all CellRangerJob bean names in correct order"() {
        expect:
        cellRangerWorkflow.jobList == [
                AlignmentFragmentJob,
                CellRangerCheckFragmentKeysJob,
                CellRangerConditionalFailJob,
                AttachUuidJob,
                CellRangerPrepareJob,
                CellRangerExecuteJob,
                CellRangerValidationJob,
                CellRangerParseJob,
                CellRangerCleanUpJob,
                CellRangerAclCleanUpJob,
                SetCorrectPermissionJob,
                CalculateSizeJob,
                CellRangerLinkJob,
                CellRangerFinishJob,
        ]
    }

    void "createCopyOfArtefact, should create a new artifact but copy the content of the old artifact"() {
        given:
        MergingWorkPackage mergingWorkPackage = createMergingWorkPackage()
        SingleCellBamFile singleCellBamFile = createBamFile(workPackage: mergingWorkPackage)
        String directory = "/test"
        cellRangerWorkflow.cellRangerWorkFileService = Mock(CellRangerWorkFileService) {
            1 * buildWorkDirectoryName(_, _) >> directory
        }

        when:
        SingleCellBamFile outputSingleCellBamFile = cellRangerWorkflow.createCopyOfArtefact(singleCellBamFile) as SingleCellBamFile

        then:
        outputSingleCellBamFile != singleCellBamFile
        outputSingleCellBamFile.mergingWorkPackage == singleCellBamFile.mergingWorkPackage
        outputSingleCellBamFile.identifier == 1
        outputSingleCellBamFile.workDirectoryName == directory
        outputSingleCellBamFile.seqTracks == singleCellBamFile.seqTracks
        outputSingleCellBamFile.numberOfMergedLanes == singleCellBamFile.numberOfMergedLanes

        and:
        singleCellBamFile.withdrawn
    }
}
