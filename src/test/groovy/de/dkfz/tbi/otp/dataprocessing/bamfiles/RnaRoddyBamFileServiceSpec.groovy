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
package de.dkfz.tbi.otp.dataprocessing.bamfiles

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.roddyRna.RoddyRnaFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

import java.nio.file.Paths

class RnaRoddyBamFileServiceSpec extends Specification implements ServiceUnitTest<RnaRoddyBamFileService>, DataTest, RoddyRnaFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                AbstractBamFile,
                RawSequenceFile,
                FastqFile,
                FileType,
                Individual,
                LibraryPreparationKit,
                MergingCriteria,
                Pipeline,
                Project,
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

    RnaRoddyBamFile bamFile
    String testDir

    void setup() {
        bamFile = createBamFile()
        testDir = "/base-dir"
        service.abstractBamFileService = Mock(AbstractBamFileService) {
            getBaseDirectory(_) >> Paths.get("/base-dir")
        }
    }

    void "test getWorkMergedQADirectory"() {
        expect:
        service.getWorkMergedQADirectory(bamFile).toString() == "${testDir}/${bamFile.workDirectoryName}/qualitycontrol"
    }

    void "test getFinalMergedQADirectory"() {
        expect:
        service.getFinalMergedQADirectory(bamFile).toString() == "${testDir}/qualitycontrol"
    }

    void "test getCorrespondingWorkChimericBamFile"() {
        expect:
        service.getCorrespondingWorkChimericBamFile(bamFile).toString() ==
                "${testDir}/${bamFile.workDirectoryName}/${bamFile.sampleType.dirName}_${bamFile.individual.pid}_chimeric_merged.mdup.bam"
    }

    void "test getWorkArribaFusionPlotPdf"() {
        expect:
        service.getWorkArribaFusionPlotPdf(bamFile).toString() ==
                "${testDir}/${bamFile.workDirectoryName}/fusions_arriba/${bamFile.sampleType.dirName}_${bamFile.individual.pid}.fusions.pdf"
    }
}
