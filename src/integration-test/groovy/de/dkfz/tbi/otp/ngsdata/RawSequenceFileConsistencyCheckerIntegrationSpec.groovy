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
package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.AbstractIntegrationSpecWithoutRollbackAnnotation
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataWorkFileService
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.administration.MailHelperService
import de.dkfz.tbi.otp.utils.SessionUtils
import de.dkfz.tbi.otp.utils.CreateFileHelper

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class RawSequenceFileConsistencyCheckerIntegrationSpec extends AbstractIntegrationSpecWithoutRollbackAnnotation {

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

    // false positives, since rule can not recognize calling class
    @SuppressWarnings('ExplicitFlushForDeleteRule')
    void "test setFileExistsForAllRawSequenceFiles"() {
        given:
        setupData()
        RawSequenceFile rawSequenceFile1
        RawSequenceFile rawSequenceFile2
        RawSequenceFile rawSequenceFile3
        RawSequenceFile rawSequenceFile4
        RawSequenceFileConsistencyChecker consistencyChecker = new RawSequenceFileConsistencyChecker()

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
            rawSequenceFile1 = DomainFactory.createFastqFile(commonProperties + [fileName: initialFile1.toFile().name, fileLinked: false])
            rawSequenceFile2 = DomainFactory.createFastqFile(commonProperties + [fileName: initialFile2.toFile().name, fileLinked: false])
            rawSequenceFile3 = DomainFactory.createFastqFile(commonProperties + [fileName: initialFile3.toFile().name, fileLinked: true])
            rawSequenceFile4 = DomainFactory.createFastqFile(commonProperties + [fileName: initialFile4.toFile().name, fileLinked: true])
            Files.copy(initialFile1, copiedFile1)
            Files.copy(initialFile2, copiedFile2)
            Files.createSymbolicLink(copiedOrLinkedDir.resolve("fileName3"), initialFile3)
            Files.createSymbolicLink(copiedOrLinkedDir.resolve("fileName4"), initialFile4)

            consistencyChecker.rawSequenceDataWorkFileService = Mock(RawSequenceDataWorkFileService) {
                1 * getFilePath(rawSequenceFile1) >> Paths.get("${copiedOrLinkedDir}/fileName1")
                1 * getFilePath(rawSequenceFile2) >> Paths.get("${copiedOrLinkedDir}/fileName2")
                1 * getFilePath(rawSequenceFile3) >> Paths.get("${copiedOrLinkedDir}/fileName3")
                1 * getFilePath(rawSequenceFile4) >> Paths.get("${copiedOrLinkedDir}/fileName4")
            }
            consistencyChecker.schedulerService = Mock(SchedulerService) {
                1 * isActive() >> true
            }

            Files.delete(copiedFile2)
            Files.delete(initialFile4)
        }

        when:
        SessionUtils.withTransaction {
            consistencyChecker.setFileExistsForAllRawSequenceFiles()
        }

        then:
        SessionUtils.withTransaction {
            rawSequenceFile1.refresh()
            rawSequenceFile2.refresh()
            rawSequenceFile3.refresh()
            rawSequenceFile4.refresh()
            assert rawSequenceFile1.fileExists
            assert !rawSequenceFile2.fileExists
            assert rawSequenceFile3.fileExists
            assert !rawSequenceFile4.fileExists
            return true
        }
    }

    void "test setFileExistsForAllRawSequenceFiles with invalid datafile, sends email"() {
        given:
        setupData()

        RawSequenceFileConsistencyChecker consistencyChecker = new RawSequenceFileConsistencyChecker()
        RawSequenceFile rawSequenceFile
        SessionUtils.withTransaction {
            FileType fileType = DomainFactory.createFileType(type: FileType.Type.SEQUENCE, subType: 'fastq', vbpPath: "/sequence/")
            SeqTrack seqTrack = DomainFactory.createSeqTrack(dataInstallationState: SeqTrack.DataProcessingState.FINISHED)
            rawSequenceFile = DomainFactory.createFastqFile(fileType: fileType, seqTrack: seqTrack, fileLinked: false)
            rawSequenceFile.mateNumber = null
            rawSequenceFile.save(flush: true, validate: false)
        }

        consistencyChecker.schedulerService = Mock(SchedulerService) {
            1 * isActive() >> true
        }
        consistencyChecker.mailHelperService = Mock(MailHelperService) {
            1 * saveErrorMailInNewTransaction('Error: RawSequenceFileConsistencyChecker.setFileExistsForAllDataFiles() failed', _) >> { String emailSubject, String content ->
                assert content.contains("Error while saving datafile with id: ${rawSequenceFile.id}")
                assert content.contains("on field 'mateNumber': rejected value [null]")
            }
        }
        consistencyChecker.rawSequenceDataWorkFileService = Mock(RawSequenceDataWorkFileService) {
            1 * getFilePath(rawSequenceFile) >> Paths.get("path")
        }
        consistencyChecker.processingOptionService = new ProcessingOptionService()

        expect:
        consistencyChecker.setFileExistsForAllRawSequenceFiles()
    }
}
