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
package de.dkfz.tbi.otp.dataprocessing

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.roddyRna.RoddyRnaFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

class RnaRoddyBamFileSpec extends Specification implements RoddyRnaFactory, DataTest {
    @Override
    Class[] getDomainClassesToMock() {
        return [
                AbstractMergedBamFile,
                DataFile,
                FileType,
                Individual,
                LibraryPreparationKit,
                MergingCriteria,
                Pipeline,
                Project,
                Realm,
                ReferenceGenome,
                ReferenceGenomeProjectSeqType,
                RnaRoddyBamFile,
                RoddyWorkflowConfig,
                Run,
                FastqImportInstance,
                Sample,
                SampleType,
                SeqCenter,
                SeqPlatform,
                SeqPlatformGroup,
                SeqPlatformModelLabel,
                SeqTrack,
                SeqType,
                SoftwareTool,
                MergingWorkPackage,
        ]
    }

    void setup() {
        new TestConfigService()
    }

    void "test method getCorrespondingWorkChimericBamFile"() {
        given:
        RnaRoddyBamFile roddyBamFile = createBamFile()
        String testDir = "${roddyBamFile.individual.getViewByPidPath(roddyBamFile.seqType).absoluteDataManagementPath.path}/" +
                "${roddyBamFile.sampleType.dirName}/${roddyBamFile.seqType.libraryLayoutDirName}/merged-alignment"
        AbstractMergedBamFileService.metaClass.static.destinationDirectory = { AbstractMergedBamFile bamFile -> return testDir }

        expect:
        "${testDir}/${roddyBamFile.workDirectoryName}/${roddyBamFile.sampleType.dirName}_${roddyBamFile.individual.pid}_${RnaRoddyBamFile.CHIMERIC_BAM_SUFFIX}" == roddyBamFile.correspondingWorkChimericBamFile.path
    }
}
