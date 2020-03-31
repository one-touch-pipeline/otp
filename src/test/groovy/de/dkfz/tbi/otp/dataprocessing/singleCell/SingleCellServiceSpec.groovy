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

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.utils.CollectionUtils

import java.nio.file.Path
import java.nio.file.Paths

class SingleCellServiceSpec extends Specification implements DataTest, DomainFactoryCore {

    private static final String WELL = 'WELL'

    @Override
    Class[] getDomainClassesToMock() {
        return [
                DataFile,
        ]
    }

    @SuppressWarnings('PublicInstanceField')
    @Rule
    public TemporaryFolder temporaryFolder

    private DataFile createDataFileHelper(String wellLabel = WELL, boolean singleCell = true, Map properties = [:]) {
        return createDataFile([
                seqTrack: createSeqTrack([
                        singleCellWellLabel: wellLabel,
                        seqType            : createSeqType(singleCell: singleCell),
                ]),
        ] + properties)
    }

    private List<DataFile> createSingleAndBulkDataFiles() {
        createDataFileHelper(WELL, false)
        createDataFileHelper(null, false)
        createDataFileHelper(null, true)
        return [
                createDataFileHelper(WELL, true),
                createDataFileHelper(WELL, true),
        ]
    }

    void "buildMappingFileName, from DataFile"() {
        given:
        DataFile dataFile = createDataFileHelper()
        SingleCellService service = new SingleCellService()

        when:
        String entry = service.buildMappingFileName(dataFile)

        then:
        "${dataFile.individual.pid}_${dataFile.sampleType.name}_${SingleCellService.MAPPING_FILE_SUFFIX}" == entry
    }

    void "buildMappingFileName, from Individual and SampleType"() {
        given:
        DataFile dataFile = createDataFileHelper()
        SingleCellService service = new SingleCellService()

        when:
        String entry = service.buildMappingFileName(dataFile.individual, dataFile.sampleType)

        then:
        "${dataFile.individual.pid}_${dataFile.sampleType.name}_${SingleCellService.MAPPING_FILE_SUFFIX}" == entry
    }

    void "singleCellMappingFile, when all fine, then return path to mapping file"() {
        given:
        new TestConfigService()

        String allWellDir = '/vbpPath/0_all'
        DataFile dataFile = createDataFileHelper()

        SingleCellService service = new SingleCellService([
                lsdfFilesService: Mock(LsdfFilesService) {
                    1 * createSingleCellAllWellDirectoryPath(dataFile) >> allWellDir
                    0 * _
                },
        ])

        Path expected = Paths.get(allWellDir, service.buildMappingFileName(dataFile))

        when:
        Path path = service.singleCellMappingFile(dataFile)

        then:
        expected == path
    }

    void "getAllSingleCellMappingFiles, when all fine, then return path to mapping files of all DataFiles that should have one"() {
        given:
        new TestConfigService()

        List<DataFile> scDataFiles = createSingleAndBulkDataFiles()
        scDataFiles << createDataFileHelper(WELL, true, [seqTrack: scDataFiles[0].seqTrack])

        Closure<String> vbpSubdirectory = { DataFile df ->
            return "/${df.individual.pid}/${df.sampleType.name}/0_all"
        }

        SingleCellService service = new SingleCellService([
                lsdfFilesService: Mock(LsdfFilesService) {
                    3 * createSingleCellAllWellDirectoryPath(_) >> { DataFile df -> vbpSubdirectory(df) }
                    0 * _
                },
        ])

        List<Path> expectedPaths = scDataFiles.collect { DataFile dataFile ->
            return Paths.get(vbpSubdirectory(dataFile), service.buildMappingFileName(dataFile))
        }.unique()

        when:
        List<Path> resultPaths = service.allSingleCellMappingFiles

        then:
        CollectionUtils.containSame(expectedPaths, resultPaths)
    }

    void "mappingEntry, when all fine, then return entry"() {
        given:
        new TestConfigService()

        DataFile dataFile = createDataFileHelper()
        String dataFilePath = "pathToFastq/fastq.fastq"

        SingleCellService service = new SingleCellService(
                lsdfFilesService: Mock(LsdfFilesService) {
                    1 * getFileViewByPidPath(_) >> dataFilePath
                }
        )

        String expected = "${dataFilePath}\t${WELL}"

        when:
        String entry = service.mappingEntry(dataFile)

        then:
        expected == entry
    }

    void "getAllDataFilesWithMappingFile, only returns DataFiles of SeqTracks with a single cell SeqType and a well label"() {
        given:
        SingleCellService service = new SingleCellService()
        List<DataFile> expectedDataFiles = createSingleAndBulkDataFiles()

        expect:
        TestCase.assertContainSame(expectedDataFiles, service.allDataFilesWithMappingFile)
    }
}
