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
package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.AbstractIntegrationSpecWithoutRollbackAnnotation
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.utils.MailHelperService
import de.dkfz.tbi.otp.utils.SessionUtils
import de.dkfz.tbi.otp.utils.CreateFileHelper

import java.nio.file.Files
import java.nio.file.Path

class DataFileConsistencyCheckerIntegrationSpec extends AbstractIntegrationSpecWithoutRollbackAnnotation {

    Path rootDir
    Path initialDir
    Path copiedOrLinkedDir

    void setupData() {
        SessionUtils.withTransaction {
            DomainFactory.createAllAlignableSeqTypes()

            rootDir = Files.createDirectory(tempDir.resolve("root"))
            initialDir = Files.createDirectory(rootDir.resolve("initial"))
            copiedOrLinkedDir = Files.createDirectory(rootDir.resolve("copiedOrLinked"))
        }
    }

    //false positives, since rule can not recognize calling class
    @SuppressWarnings('ExplicitFlushForDeleteRule')
    void "test setFileExistsForAllDataFiles"() {
        given:
        setupData()
        DataFile dataFile1
        DataFile dataFile2
        DataFile dataFile3
        DataFile dataFile4
        DataFileConsistencyChecker dataFileConsistencyChecker = new DataFileConsistencyChecker()

        SessionUtils.withTransaction {
            Path initialFile1 = CreateFileHelper.createFile(initialDir.resolve("fileName1"))
            Path initialFile2 = CreateFileHelper.createFile(initialDir.resolve("fileName2"))
            Path initialFile3 = CreateFileHelper.createFile(initialDir.resolve("fileName3"))
            Path initialFile4 = CreateFileHelper.createFile(initialDir.resolve("fileName4"))
            Path copiedFile1 = copiedOrLinkedDir.resolve("fileName1")
            Path copiedFile2 = copiedOrLinkedDir.resolve("fileName2")
            Map<String, Object> commonProperties = [
                    fileType        : DomainFactory.createFileType(type: FileType.Type.SEQUENCE, subType: 'fastq'),
                    seqTrack        : DomainFactory.createSeqTrack(dataInstallationState: SeqTrack.DataProcessingState.FINISHED),
                    initialDirectory: initialDir,
            ]
            dataFile1 = DomainFactory.createDataFile(commonProperties + [fileName: initialFile1.toFile().name, fileLinked: false])
            dataFile2 = DomainFactory.createDataFile(commonProperties + [fileName: initialFile2.toFile().name, fileLinked: false])
            dataFile3 = DomainFactory.createDataFile(commonProperties + [fileName: initialFile3.toFile().name, fileLinked: true])
            dataFile4 = DomainFactory.createDataFile(commonProperties + [fileName: initialFile4.toFile().name, fileLinked: true])
            Files.copy(initialFile1, copiedFile1)
            Files.copy(initialFile2, copiedFile2)
            Files.createSymbolicLink(copiedOrLinkedDir.resolve("fileName3"), initialFile3)
            Files.createSymbolicLink(copiedOrLinkedDir.resolve("fileName4"), initialFile4)

            dataFileConsistencyChecker.lsdfFilesService = Mock(LsdfFilesService) {
                1 * getFileFinalPath(dataFile1) >> "${copiedOrLinkedDir}/fileName1"
                1 * getFileFinalPath(dataFile2) >> "${copiedOrLinkedDir}/fileName2"
                1 * getFileFinalPath(dataFile3) >> "${copiedOrLinkedDir}/fileName3"
                1 * getFileFinalPath(dataFile4) >> "${copiedOrLinkedDir}/fileName4"
            }
            dataFileConsistencyChecker.schedulerService = Mock(SchedulerService) {
                1 * isActive() >> true
            }

            Files.delete(copiedFile2)
            Files.delete(initialFile4)
        }

        when:
        SessionUtils.withTransaction {
            dataFileConsistencyChecker.setFileExistsForAllDataFiles()
        }

        then:
        SessionUtils.withTransaction {
            dataFile1.refresh()
            dataFile2.refresh()
            dataFile3.refresh()
            dataFile4.refresh()
            assert dataFile1.fileExists
            assert !dataFile2.fileExists
            assert dataFile3.fileExists
            assert !dataFile4.fileExists
            return true
        }
    }

    void "test setFileExistsForAllDataFiles with invalid datafile, sends email"() {
        given:
        setupData()

        DataFileConsistencyChecker dataFileConsistencyChecker = new DataFileConsistencyChecker()
        DataFile dataFile
        SessionUtils.withTransaction {
            FileType fileType = DomainFactory.createFileType(type: FileType.Type.SEQUENCE, subType: 'fastq', vbpPath: "/sequence/")
            SeqTrack seqTrack = DomainFactory.createSeqTrack(dataInstallationState: SeqTrack.DataProcessingState.FINISHED)
            dataFile = DomainFactory.createDataFile(fileType: fileType, seqTrack: seqTrack, fileLinked: false)
            dataFile.mateNumber = null
            dataFile.save(flush: true, validate: false)
        }

        dataFileConsistencyChecker.schedulerService = Mock(SchedulerService) {
            1 * isActive() >> true
        }
        dataFileConsistencyChecker.mailHelperService = Mock(MailHelperService) {
            1 * sendEmailToTicketSystem('Error: DataFileConsistencyChecker.setFileExistsForAllDataFiles() failed', _) >> { String emailSubject, String content ->
                assert content.contains("Error while saving datafile with id: ${dataFile.id}")
                assert content.contains("on field 'mateNumber': rejected value [null]")
            }
        }
        dataFileConsistencyChecker.lsdfFilesService = Mock(LsdfFilesService) {
            1 * getFileFinalPath(dataFile) >> "path"
        }
        dataFileConsistencyChecker.processingOptionService = new ProcessingOptionService()

        expect:
        dataFileConsistencyChecker.setFileExistsForAllDataFiles()
    }
}
