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
package de.dkfz.tbi.otp.workflow.alignment.panCancer

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.bamfiles.RoddyBamFileService
import de.dkfz.tbi.otp.domainFactory.pipelines.RoddyPanCancerFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.workflow.alignment.*
import de.dkfz.tbi.otp.workflow.jobs.AttachUuidJob
import de.dkfz.tbi.otp.workflow.jobs.CalculateSizeJob
import de.dkfz.tbi.otp.workflow.jobs.SetCorrectPermissionJob

class PanCancerWorkflowSpec extends Specification implements RoddyPanCancerFactory, DataTest {

    PanCancerWorkflow panCancerWorkflow

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                Pipeline,
                LibraryPreparationKit,
                MergingWorkPackage,
                RoddyBamFile,
                FileType,
                FastqImportInstance,
                ReferenceGenomeProjectSeqType,
        ]
    }

    void setup() {
        panCancerWorkflow = new PanCancerWorkflow()
    }

    void "getJobList, should return all PanCancerJob bean names in correct order"() {
        expect:
        panCancerWorkflow.jobList == [
                RoddyAlignmentFragmentJob,
//                RoddyAlignmentCheckFragmentKeysJob,
                RoddyAlignmentConditionalFailJob,
                AttachUuidJob,
                RoddyAlignmentPrepareJob,
                PanCancerExecuteJob,
                PanCancerValidationJob,
                PanCancerParseJob,
                RoddyAlignmentCheckQcJob,
                PanCancerCleanUpJob,
                SetCorrectPermissionJob,
                CalculateSizeJob,
                PanCancerLinkJob,
                RoddyAlignmentFinishJob,
        ]
    }

    void "createCopyOfArtefact, should create a new artifact but copy the content of the old artifact"() {
        given:
        MergingWorkPackage mergingWorkPackage = createMergingWorkPackage()
        RoddyBamFile roddyBamFile = createBamFile(workPackage: mergingWorkPackage)

        when:
        RoddyBamFile outputRoddyBamFile = panCancerWorkflow.createCopyOfArtefact(roddyBamFile) as RoddyBamFile

        then:
        outputRoddyBamFile != roddyBamFile
        outputRoddyBamFile.mergingWorkPackage == roddyBamFile.mergingWorkPackage
        outputRoddyBamFile.identifier == 1
        outputRoddyBamFile.workDirectoryName == "${RoddyBamFileService.WORK_DIR_PREFIX}_1"
        outputRoddyBamFile.seqTracks == roddyBamFile.seqTracks
        outputRoddyBamFile.config == roddyBamFile.config
        outputRoddyBamFile.numberOfMergedLanes == roddyBamFile.numberOfMergedLanes
    }
}
