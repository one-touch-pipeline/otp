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
package de.dkfz.tbi.otp.workflow.alignment.rna

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.bamfiles.RoddyBamFileService
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.domainFactory.pipelines.roddyRna.RoddyRnaFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.FastqFile

class RnaAlignmentWorkflowSpec extends Specification implements RoddyRnaFactory, WorkflowSystemDomainFactory, DataTest {

    RnaAlignmentWorkflow rnaAlignmentWorkflow

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                MergingWorkPackage,
                RnaRoddyBamFile,
        ]
    }

    void setup() {
        rnaAlignmentWorkflow = new RnaAlignmentWorkflow()
    }

    void "getJobBeanNames, should return all RnaAlignmentJob bean names in correct order"() {
        expect:
        rnaAlignmentWorkflow.jobBeanNames == [
                "roddyAlignmentFragmentJob",
//                "rnaAlignmentCheckFragmentKeysJob",
                "roddyAlignmentConditionalFailJob",
                "attachUuidJob",
                "roddyAlignmentPrepareJob",
                "rnaAlignmentExecuteJob",
                "rnaAlignmentValidationJob",
                "rnaAlignmentParseJob",
                "roddyAlignmentCheckQcJob",
                "rnaAlignmentCleanUpJob",
                "setCorrectPermissionJob",
                "calculateSizeJob",
                "rnaAlignmentLinkJob",
                "roddyAlignmentFinishJob",
        ]
    }

    void "createCopyOfArtefact, should create a new artifact but copy the content of the old artifact"() {
        given:
        MergingWorkPackage mergingWorkPackage = createMergingWorkPackage()
        RnaRoddyBamFile roddyBamFile = createBamFile([
                workPackage     : mergingWorkPackage,
        ])

        when:
        RnaRoddyBamFile outputRoddyBamFile = rnaAlignmentWorkflow.createCopyOfArtefact(roddyBamFile) as RoddyBamFile

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
