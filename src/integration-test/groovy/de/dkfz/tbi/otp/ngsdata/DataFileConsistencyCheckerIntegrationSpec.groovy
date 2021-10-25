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

import org.junit.Rule
import org.junit.rules.TemporaryFolder

import de.dkfz.tbi.otp.AbstractIntegrationSpecWithoutRollbackAnnotation
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.utils.MailHelperService
import de.dkfz.tbi.otp.utils.SessionUtils

import java.nio.file.Files

class DataFileConsistencyCheckerIntegrationSpec extends AbstractIntegrationSpecWithoutRollbackAnnotation {

    @Rule
    TemporaryFolder temporaryFolder
    File testFolder

    void setupData() {
        SessionUtils.withNewSession {
            DomainFactory.createAllAlignableSeqTypes()

            testFolder = temporaryFolder.newFolder("root")
            temporaryFolder.newFolder("root", "initial")
            temporaryFolder.newFolder("root", "copiedOrLinked")
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

        SessionUtils.withNewSession {
            File initialFile1 = temporaryFolder.newFile("root/initial/fileName1")
            File initialFile2 = temporaryFolder.newFile("root/initial/fileName2")
            File initialFile3 = temporaryFolder.newFile("root/initial/fileName3")
            File initialFile4 = temporaryFolder.newFile("root/initial/fileName4")
            File copiedFile1 = new File("${testFolder.absolutePath}/copiedOrLinked/fileName1")
            File copiedFile2 = new File("${testFolder.absolutePath}/copiedOrLinked/fileName2")
            Map<String, Object> commonProperties = [
                    fileType        : DomainFactory.createFileType(type: FileType.Type.SEQUENCE, subType: 'fastq'),
                    seqTrack        : DomainFactory.createSeqTrack(dataInstallationState: SeqTrack.DataProcessingState.FINISHED),
                    initialDirectory: "${testFolder.absolutePath}/initial",
            ]
            dataFile1 = DomainFactory.createDataFile(commonProperties + [fileName: initialFile1.name, fileLinked: false])
            dataFile2 = DomainFactory.createDataFile(commonProperties + [fileName: initialFile2.name, fileLinked: false])
            dataFile3 = DomainFactory.createDataFile(commonProperties + [fileName: initialFile3.name, fileLinked: true])
            dataFile4 = DomainFactory.createDataFile(commonProperties + [fileName: initialFile4.name, fileLinked: true])
            Files.copy(initialFile1.toPath(), copiedFile1.toPath())
            Files.copy(initialFile2.toPath(), copiedFile2.toPath())
            Files.createSymbolicLink(new File("${testFolder.absolutePath}/copiedOrLinked/fileName3").toPath(), initialFile3.toPath())
            Files.createSymbolicLink(new File("${testFolder.absolutePath}/copiedOrLinked/fileName4").toPath(), initialFile4.toPath())

            dataFileConsistencyChecker.lsdfFilesService = Mock(LsdfFilesService) {
                1 * getFileFinalPath(dataFile1) >> "${testFolder.absolutePath}/copiedOrLinked/fileName1"
                1 * getFileFinalPath(dataFile2) >> "${testFolder.absolutePath}/copiedOrLinked/fileName2"
                1 * getFileFinalPath(dataFile3) >> "${testFolder.absolutePath}/copiedOrLinked/fileName3"
                1 * getFileFinalPath(dataFile4) >> "${testFolder.absolutePath}/copiedOrLinked/fileName4"
            }
            dataFileConsistencyChecker.schedulerService = Mock(SchedulerService) {
                1 * isActive() >> true
            }

            copiedFile2.delete()
            initialFile4.delete()
        }

        when:
        SessionUtils.withNewSession {
            dataFileConsistencyChecker.setFileExistsForAllDataFiles()
            dataFile1.refresh()
            dataFile2.refresh()
            dataFile3.refresh()
            dataFile4.refresh()
        }

        then:
        SessionUtils.withNewSession {
            return dataFile1.fileExists &&
                    !dataFile2.fileExists &&
                    dataFile3.fileExists &&
                    !dataFile4.fileExists
        }
    }

    void "test setFileExistsForAllDataFiles with invalid datafile, sends email"() {
        given:
        setupData()

        DataFileConsistencyChecker dataFileConsistencyChecker = new DataFileConsistencyChecker()
        DataFile dataFile
        String errorRecipient = "test@test.com"
        SessionUtils.withNewSession {
            DomainFactory.createProcessingOptionForErrorRecipient(errorRecipient)
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
            1 * sendEmail('Error: DataFileConsistencyChecker.setFileExistsForAllDataFiles() failed', _, errorRecipient) >> { String emailSubject, String content, String recipients ->
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
