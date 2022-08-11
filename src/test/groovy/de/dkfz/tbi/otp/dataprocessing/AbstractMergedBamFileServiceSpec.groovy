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
import spock.lang.TempDir
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

import java.nio.file.Path
import java.nio.file.Paths

class AbstractMergedBamFileServiceSpec extends Specification implements DataTest, IsRoddy {

    @Override
    Class[] getDomainClassesToMock() {
        [
                FastqImportInstance,
                FileType,
                Individual,
                LibraryPreparationKit,
                MergingWorkPackage,
                Pipeline,
                ProcessingPriority,
                Project,
                Realm,
                ReferenceGenomeProjectSeqType,
                RoddyBamFile,
                RoddyWorkflowConfig,
                Sample,
                SampleType,
        ]
    }

    @TempDir
    Path tempDir

    void "test getBaseDirectory"() {
        given:
        RoddyBamFile bamFile = createBamFile()

        AbstractMergedBamFileService service = new AbstractMergedBamFileService()
        service.individualService = Mock(IndividualService) {
            getViewByPidPath(_, _) >> Paths.get("/vbp-path")
        }

        expect:
        service.getBaseDirectory(bamFile) ==
                Paths.get("/vbp-path/${bamFile.sample.sampleType.dirName}/${bamFile.seqType.libraryLayoutDirName}/merged-alignment")
    }

    void "test getBaseDirectory, with antibody target"() {
        given:
        RoddyBamFile bamFile = createBamFile()
        bamFile.seqType.hasAntibodyTarget = true
        bamFile.mergingWorkPackage.antibodyTarget = createAntibodyTarget(name: "antibody-target-name")

        AbstractMergedBamFileService service = new AbstractMergedBamFileService()
        service.individualService = Mock(IndividualService) {
            getViewByPidPath(_, _) >> Paths.get("/vbp-path")
        }

        expect:
        service.getBaseDirectory(bamFile) ==
                Paths.get("/vbp-path/${bamFile.sample.sampleType.dirName}-antibody-target-name/${bamFile.seqType.libraryLayoutDirName}/merged-alignment")
    }

    void "getExistingBamFilePath, when all fine, return the file"() {
        given:
        File file = CreateFileHelper.createFile(tempDir.resolve("test.txt")).toFile()
        AbstractMergedBamFileService service = new AbstractMergedBamFileService()
        AbstractMergedBamFile bamFile = Mock(AbstractMergedBamFile) {
            1 * pathForFurtherProcessing >> file
            1 * md5sum >> HelperUtils.randomMd5sum
            2 * fileSize >> file.size()
            0 * _
        }

        when:
        File existingFile = service.getExistingBamFilePath(bamFile)

        then:
        file == existingFile
    }

    @Unroll
    void "getExistingBamFilePath, when fail for #failCase, throw an exception"() {
        given:
        File file = CreateFileHelper.createFile(tempDir.resolve("test.txt")).toFile()
        AbstractMergedBamFileService service = new AbstractMergedBamFileService()
        AbstractMergedBamFile bamFile = Mock(AbstractMergedBamFile) {
            _ * pathForFurtherProcessing >> {
                if (failCase == 'exceptionInFurtherProcessingPath') {
                    throw new AssertionError('pathForFurtherProcessing fail')
                } else if (failCase == 'fileNotExist') {
                    TestCase.uniqueNonExistentPath
                } else {
                    file
                }
            }
            _ * md5sum >> {
                failCase == 'invalidMd5sum' ? 'invalid' : HelperUtils.randomMd5sum
            }
            _ * fileSize >> {
                failCase == 'fileSizeZero' ? 0 :
                        failCase == 'fileSizeWrong' ? 1234 : file.length()
            }
            0 * _
        }

        when:
        service.getExistingBamFilePath(bamFile)

        then:
        AssertionError e = thrown()
        e.message.contains(exception)

        where:
        failCase                           || exception
        'exceptionInFurtherProcessingPath' || 'pathForFurtherProcessing fail'
        'fileNotExist'                     || 'on local filesystem is not accessible or does not exist. Expression: de.dkfz.tbi.otp.utils.ThreadUtils.waitFor'
        'invalidMd5sum'                    || 'assert bamFile.md5sum'
        'fileSizeZero'                     || 'assert bamFile.fileSize'
        'fileSizeWrong'                    || 'assert file.length() == bamFile.fileSize'
    }
}
