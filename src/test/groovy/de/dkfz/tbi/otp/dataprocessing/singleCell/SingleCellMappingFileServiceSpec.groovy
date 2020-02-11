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
        [
                DataFile,
        ]
    }

    @SuppressWarnings('JUnitPublicProperty')
    @Rule
    TemporaryFolder temporaryFolder

    class AddMappingFileEntryIfMissing implements DomainFactoryCore {

        SingleCellMappingFileService service

        Path mappingFile = temporaryFolder.newFile().toPath()

        DataFile dataFile = createDataFile()
    }

    private AddMappingFileEntryIfMissing createDataAddMappingFileEntryIfMissing() {
        AddMappingFileEntryIfMissing data = new AddMappingFileEntryIfMissing()

        data.service = new SingleCellMappingFileService([
                fileSystemService: Mock(FileSystemService) {
                    1 * getRemoteFileSystemOnDefaultRealm() >> FileSystems.default
                },
                fileService      : new FileService(),
                singleCellService: Mock(SingleCellService) {
                    1 * singleCellMappingFile(_) >> data.mappingFile
                    1 * mappingEntry(_) >> ENTRY
                },
        ])
        return data
    }

    void "addMappingFileEntryIfMissing, when file not exist, then create it with entry"() {
        given:
        AddMappingFileEntryIfMissing data = createDataAddMappingFileEntryIfMissing()
        Files.delete(data.mappingFile)

        when:
        data.service.addMappingFileEntryIfMissing(data.dataFile)

        then:
        Files.exists(data.mappingFile)
        data.mappingFile.text == ENTRY + '\n'
    }

    void "addMappingFileEntryIfMissing, when file exist but has not the entry, then add entry"() {
        given:
        AddMappingFileEntryIfMissing data = createDataAddMappingFileEntryIfMissing()
        data.mappingFile.text = DATA

        when:
        data.service.addMappingFileEntryIfMissing(data.dataFile)

        then:
        Files.exists(data.mappingFile)
        data.mappingFile.text == DATA + ENTRY + '\n'
    }

    void "addMappingFileEntryIfMissing, when file exist and has the entry, then do not add the entry again"() {
        given:
        String existingData = "${DATA}\n${ENTRY}\n"
        AddMappingFileEntryIfMissing data = createDataAddMappingFileEntryIfMissing()
        data.mappingFile.text = existingData

        when:
        data.service.addMappingFileEntryIfMissing(data.dataFile)

        then:
        Files.exists(data.mappingFile)
        data.mappingFile.text == existingData
    }
}
