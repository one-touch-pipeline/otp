/*
 * Copyright 2011-2022 The OTP authors
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
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

import java.nio.file.Paths

class ProcessedMergedBamFileServiceSpec extends Specification implements ServiceUnitTest<ProcessedMergedBamFileService>, DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                AlignmentPass,
                FastqImportInstance,
                Individual,
                LibraryPreparationKit,
                MergingPass,
                MergingSet,
                MergingSetAssignment,
                MergingWorkPackage,
                Pipeline,
                ProcessedBamFile,
                ProcessedMergedBamFile,
                ProcessingPriority,
                Project,
                Realm,
                Sample,
                SampleType,
                SeqType,
        ]
    }

    ProcessedMergedBamFile bamFile
    String testDir

    void setup() {
        bamFile = DomainFactory.createProcessedMergedBamFile()
        testDir = "/base-dir"
        service.abstractMergedBamFileService = Mock(AbstractMergedBamFileService) {
            getBaseDirectory(_) >> Paths.get("/base-dir")
        }
    }

    void "test getFinalInsertSizeFile"() {
        when:
        service.getFinalInsertSizeFile(bamFile)

        then:
        thrown(UnsupportedOperationException)
    }

    void "test getPathForFurtherProcessing, returns null since qcTrafficLightStatus is #status"() {
        given:
        bamFile.qcTrafficLightStatus = status
        bamFile.comment = DomainFactory.createComment()

        expect:
        !service.getPathForFurtherProcessing(bamFile)

        where:
        status << [AbstractMergedBamFile.QcTrafficLightStatus.BLOCKED, AbstractMergedBamFile.QcTrafficLightStatus.REJECTED]
    }

    void "test getPathForFurtherProcessing, should return final directory"() {
        given:
        bamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.PROCESSED
        bamFile.md5sum = HelperUtils.randomMd5sum
        bamFile.fileSize = 1
        bamFile.save(flush: true)
        bamFile.mergingWorkPackage.bamFileInProjectFolder = bamFile
        bamFile.mergingWorkPackage.save(flush: true)

        expect:
        service.getPathForFurtherProcessing(bamFile).toString() == "/base-dir/${bamFile.bamFileName}"
    }

    void "test getPathForFurtherProcessing, when not set in mergingWorkPackage, should throw exception"() {
        when:
        service.getPathForFurtherProcessing(bamFile)

        then:
        thrown(IllegalStateException)
    }
}
