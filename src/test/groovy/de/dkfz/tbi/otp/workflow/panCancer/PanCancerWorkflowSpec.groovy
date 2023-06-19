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
package de.dkfz.tbi.otp.workflow.panCancer

import grails.testing.gorm.DataTest
import spock.lang.Specification
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.pipelines.RoddyPancanFactory
import de.dkfz.tbi.otp.ngsdata.*

class PanCancerWorkflowSpec extends Specification implements RoddyPancanFactory, DataTest {

    PanCancerWorkflow panCancerWorkflow

    @Override
    Class[] getDomainClassesToMock() {
        return [
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

    void "getJobBeanNames, should return all PanCancerJob bean names in correct order"() {
        expect:
        panCancerWorkflow.jobBeanNames == [
                "panCancerFragmentJob",
                "panCancerConditionalFailJob",
                "panCancerPrepareJob",
                "panCancerExecuteJob",
                "panCancerValidationJob",
                "panCancerParseJob",
                "panCancerCheckQcJob",
                "panCancerCleanUpJob",
                "setCorrectPermissionJob",
                "panCancerLinkJob",
                "panCancerFinishJob",
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
        outputRoddyBamFile.workDirectoryName == "${RoddyBamFile.WORK_DIR_PREFIX}_1"
        outputRoddyBamFile.baseBamFile == roddyBamFile.baseBamFile
        outputRoddyBamFile.seqTracks == roddyBamFile.seqTracks
        outputRoddyBamFile.config == roddyBamFile.config
        outputRoddyBamFile.numberOfMergedLanes == roddyBamFile.numberOfMergedLanes
    }
}
