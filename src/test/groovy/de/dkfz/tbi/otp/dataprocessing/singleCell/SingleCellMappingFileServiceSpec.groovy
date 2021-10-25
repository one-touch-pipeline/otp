/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.otp.dataprocessing.singleCell

import grails.testing.gorm.DataTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.DataFile

import java.nio.file.*

class SingleCellMappingFileServiceSpec extends Specification implements DataTest, DomainFactoryCore {

    private static final String ENTRY = "fileNew\twellNew"
    private static final String DATA = "file1\twell1\nfile2\twell2\n"

    @Override
    Class[] getDomainClassesToMock() {
        return [
                DataFile,
        ]
    }

    @Rule
    TemporaryFolder temporaryFolder

    class AddMappingFileEntryIfMissing implements DomainFactoryCore {

        SingleCellMappingFileService service
        List<DataFile> dataFiles = []
        Map<DataFile, Path> mappingFileOfDataFile = [:]

        AddMappingFileEntryIfMissing(List<DataFile> dataFiles) {
            this.dataFiles = dataFiles
            this.mappingFileOfDataFile = dataFiles.collectEntries { DataFile dataFile ->
                return [(dataFile): temporaryFolder.newFile().toPath()]
            }
        }
    }

    private AddMappingFileEntryIfMissing createDataAddMappingFileEntryIfMissing(List<DataFile> dataFiles) {
        AddMappingFileEntryIfMissing data = new AddMappingFileEntryIfMissing(dataFiles)

        data.service = new SingleCellMappingFileService([
                fileSystemService: Mock(FileSystemService) {
                    _ * getRemoteFileSystemOnDefaultRealm() >> FileSystems.default
                },
                fileService      : new FileService(),
                singleCellService: Mock(SingleCellService) {
                    _ * singleCellMappingFile(_) >> { DataFile df -> data.mappingFileOfDataFile[df] }
                    _ * getAllSingleCellMappingFiles() >> { data.mappingFileOfDataFile.values() as List<Path> }
                    _ * getAllDataFilesWithMappingFile() >> { data.dataFiles }
                    _ * mappingEntry(_) >> ENTRY
                },
        ])
        return data
    }

    //false positives, since rule can not recognize calling class
    @SuppressWarnings('ExplicitFlushForDeleteRule')
    void "addMappingFileEntryIfMissing, when file not exist, then create it with entry"() {
        given:
        List<DataFile> dataFiles = [createDataFile()]
        AddMappingFileEntryIfMissing data = createDataAddMappingFileEntryIfMissing(dataFiles)
        DataFile dataFile = data.dataFiles[0]
        Path mappingFile = data.mappingFileOfDataFile[(dataFile)]

        Files.delete(mappingFile)

        when:
        data.service.addMappingFileEntryIfMissing(dataFile)

        then:
        Files.exists(mappingFile)
        mappingFile.text == ENTRY + '\n'
    }

    void "addMappingFileEntryIfMissing, when file exist but has not the entry, then add entry"() {
        given:
        List<DataFile> dataFiles = [createDataFile()]
        AddMappingFileEntryIfMissing data = createDataAddMappingFileEntryIfMissing(dataFiles)
        DataFile dataFile = data.dataFiles[0]
        Path mappingFile = data.mappingFileOfDataFile[(dataFile)]

        mappingFile.text = DATA

        when:
        data.service.addMappingFileEntryIfMissing(dataFile)

        then:
        Files.exists(mappingFile)
        mappingFile.text == DATA + ENTRY + '\n'
    }

    void "addMappingFileEntryIfMissing, when file exist and has the entry, then do not add the entry again"() {
        given:
        List<DataFile> dataFiles = [createDataFile()]
        AddMappingFileEntryIfMissing data = createDataAddMappingFileEntryIfMissing(dataFiles)
        DataFile dataFile = data.dataFiles[0]
        Path mappingFile = data.mappingFileOfDataFile[(dataFile)]

        String existingData = "${DATA}\n${ENTRY}\n"
        mappingFile.text = existingData

        when:
        data.service.addMappingFileEntryIfMissing(dataFile)

        then:
        Files.exists(mappingFile)
        mappingFile.text == existingData
    }

    //false positives, since rule can not recognize calling class
    @SuppressWarnings('ExplicitFlushForDeleteRule')
    void "addMappingFileEntryIfMissingForAllViableDataFiles, creates file and adds the entry if not already contained"() {
        given:
        List<DataFile> dataFiles = [
                createDataFile(),
                createDataFile(),
                createDataFile(),
        ]
        AddMappingFileEntryIfMissing data = createDataAddMappingFileEntryIfMissing(dataFiles)

        String expectedContentWithPreexistingData = "${DATA}\n${ENTRY}\n"
        String expectedContentWithoutPreexistingData = "${ENTRY}\n"

        Path mappingFileA = data.mappingFileOfDataFile[dataFiles[0]]

        Path mappingFileB = data.mappingFileOfDataFile[dataFiles[1]]
        mappingFileB.text = expectedContentWithPreexistingData

        Path mappingFileC = data.mappingFileOfDataFile[dataFiles[2]]
        Files.delete(mappingFileC)

        when:
        data.service.addMappingFileEntryIfMissingForAllViableDataFiles()

        then:
        mappingFileA
        mappingFileA.text == expectedContentWithoutPreexistingData

        mappingFileB
        mappingFileB.text == expectedContentWithPreexistingData

        Files.exists(mappingFileC)
        mappingFileC.text == expectedContentWithoutPreexistingData
    }

    void "deleteAllMappingFiles, after the call, no mapping files are left"() {
        given:
        List<DataFile> dataFiles = [
                createDataFile(),
                createDataFile(),
        ]
        AddMappingFileEntryIfMissing data = createDataAddMappingFileEntryIfMissing(dataFiles)

        expect: "The files exist before the call"
        data.mappingFileOfDataFile.values().every { Path path ->
            Files.exists(path)
        }

        when:
        data.service.deleteAllMappingFiles()

        then: "The files have been removed"
        data.mappingFileOfDataFile.values().every { Path path ->
            !Files.exists(path)
        }
    }

    void "recreateAllMappingFiles, is executable in repetition"() {
        given:
        DataFile dataFile = createDataFile()
        AddMappingFileEntryIfMissing data = createDataAddMappingFileEntryIfMissing([dataFile])
        Path mappingFile = data.mappingFileOfDataFile[dataFile]

        Files.deleteIfExists(mappingFile)
        assert !Files.exists(mappingFile): "The tests requires that there is no file in the beginning"

        when: "File does not exist"
        data.service.recreateAllMappingFiles()

        then: "nothing to delete, create file with entry"
        Files.exists(mappingFile)
        mappingFile.text == "${ENTRY}\n"

        when: "File does exist, does not contain entry"
        Files.setPosixFilePermissions(mappingFile, FileService.OWNER_READ_WRITE_GROUP_READ_FILE_PERMISSION)
        mappingFile.text = DATA
        data.service.recreateAllMappingFiles()

        then: "delete file, create file"
        Files.exists(mappingFile)
        mappingFile.text == "${ENTRY}\n"

        when: "File does exist, does contain entry"
        String expected = mappingFile.text
        data.service.recreateAllMappingFiles()

        then: "delete file, create file"
        Files.exists(mappingFile)
        mappingFile.text == expected
    }
}
