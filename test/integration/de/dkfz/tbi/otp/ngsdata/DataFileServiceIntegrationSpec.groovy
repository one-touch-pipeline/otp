package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.testing.*
import de.dkfz.tbi.otp.utils.MailHelperService
import grails.plugin.springsecurity.*
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.*
import grails.test.spock.*

import java.nio.file.*

class DataFileServiceIntegrationSpec extends IntegrationSpec implements UserAndRoles {

    @Rule
    TemporaryFolder temporaryFolder
    File testFolder

    def setup() {
        createUserAndRoles()
        DomainFactory.createAllAlignableSeqTypes()

        testFolder = temporaryFolder.newFolder("root")
        temporaryFolder.newFolder("root", "initial")
        temporaryFolder.newFolder("root", "copiedOrLinked")
    }

    void "test setFileExistsForAllDataFiles"() {
        given:
        DataFileService dataFileService = new DataFileService()
        File initialFile1 = temporaryFolder.newFile("root/initial/fileName1")
        File initialFile2 = temporaryFolder.newFile("root/initial/fileName2")
        File initialFile3 = temporaryFolder.newFile("root/initial/fileName3")
        File initialFile4 = temporaryFolder.newFile("root/initial/fileName4")
        File copiedFile1 = new File("${testFolder.absolutePath}/copiedOrLinked/fileName1")
        File copiedFile2 = new File("${testFolder.absolutePath}/copiedOrLinked/fileName2")
        FileType fileType = DomainFactory.createFileType(type: FileType.Type.SEQUENCE, subType: 'fastq')
        SeqTrack seqTrack = DomainFactory.createSeqTrack(dataInstallationState: SeqTrack.DataProcessingState.FINISHED)
        DataFile dataFile1 = DomainFactory.createDataFile(fileName: initialFile1.name, initialDirectory: "${testFolder.absolutePath}/initial", fileType: fileType, seqTrack: seqTrack, fileLinked: false)
        DataFile dataFile2 = DomainFactory.createDataFile(fileName: initialFile2.name, initialDirectory: "${testFolder.absolutePath}/initial", fileType: fileType, seqTrack: seqTrack, fileLinked: false)
        DataFile dataFile3 = DomainFactory.createDataFile(fileName: initialFile3.name, initialDirectory: "${testFolder.absolutePath}/initial", fileType: fileType, seqTrack: seqTrack, fileLinked: true)
        DataFile dataFile4 = DomainFactory.createDataFile(fileName: initialFile4.name, initialDirectory: "${testFolder.absolutePath}/initial", fileType: fileType, seqTrack: seqTrack, fileLinked: true)
        Files.copy(initialFile1.toPath(), copiedFile1.toPath())
        Files.copy(initialFile2.toPath(), copiedFile2.toPath())
        Files.createSymbolicLink(new File("${testFolder.absolutePath}/copiedOrLinked/fileName3").toPath(), initialFile3.toPath())
        Files.createSymbolicLink(new File("${testFolder.absolutePath}/copiedOrLinked/fileName4").toPath(), initialFile4.toPath())

        dataFileService.lsdfFilesService = Mock(LsdfFilesService) {
            1 * getFileFinalPath(dataFile1) >> "${testFolder.absolutePath}/copiedOrLinked/fileName1"
            1 * getFileFinalPath(dataFile2) >> "${testFolder.absolutePath}/copiedOrLinked/fileName2"
            1 * getFileFinalPath(dataFile3) >> "${testFolder.absolutePath}/copiedOrLinked/fileName3"
            1 * getFileFinalPath(dataFile4) >> "${testFolder.absolutePath}/copiedOrLinked/fileName4"
        }

        copiedFile2.delete()
        initialFile4.delete()

        when:
        dataFileService.setFileExistsForAllDataFiles()

        then:
        dataFile1.fileExists
        !dataFile2.fileExists
        dataFile3.fileExists
        !dataFile4.fileExists
    }

    void "test getFastqDataFiles"() {
        when:
        DataFileService dataFileService = new DataFileService()
        DomainFactory.createDataFile(fileType: DomainFactory.createFileType(type: type, subType: subType), seqTrack: DomainFactory.createSeqTrack(dataInstallationState: state), fileWithdrawn: fileWithdrawn)

        then:
        dataFileService.getFastqDataFiles().size() == size

        where:
        type                   | subType    | state                                    | fileWithdrawn || size
        FileType.Type.SEQUENCE | 'fastq'    | SeqTrack.DataProcessingState.FINISHED    | true          || 0
        FileType.Type.SEQUENCE | 'fastq'    | SeqTrack.DataProcessingState.NOT_STARTED | false         || 0
        FileType.Type.SEQUENCE | 'notFastq' | SeqTrack.DataProcessingState.FINISHED    | false         || 0
        FileType.Type.MERGED   | 'fastq'    | SeqTrack.DataProcessingState.FINISHED    | false         || 0
        FileType.Type.SEQUENCE | 'fastq'    | SeqTrack.DataProcessingState.FINISHED    | false         || 1
    }


    void "test setFileExistsForAllDataFiles with invalid datafile, sends email"() {
        given:
        DataFileService dataFileService = new DataFileService()
        String errorRecipient = "test@test.com"
        DomainFactory.createProcessingOptionForErrorRecipient(errorRecipient)
        FileType fileType = DomainFactory.createFileType(type: FileType.Type.SEQUENCE, subType: 'fastq', vbpPath: "/sequence/")
        SeqTrack seqTrack = DomainFactory.createSeqTrack(dataInstallationState: SeqTrack.DataProcessingState.FINISHED)
        DataFile dataFile = DomainFactory.createDataFile(fileType: fileType, seqTrack: seqTrack, fileLinked: false)
        dataFile.mateNumber = null
        dataFile.save(validate: false)

        dataFileService.mailHelperService = Mock(MailHelperService) {
            1 * sendEmail('Error: DataFileService.setFileExistsForAllDataFiles() failed', _, errorRecipient) >> {String emailSubject, String content, String recipients ->
                assert content.contains("Error while saving datafile with id: ${dataFile.id}")
                assert content.contains("on field 'mateNumber': rejected value [null]")
            }
        }

        dataFileService.lsdfFilesService = Mock(LsdfFilesService) {
            1 * getFileFinalPath(dataFile) >> "path"
        }

        expect:
        dataFileService.setFileExistsForAllDataFiles()
    }
}
