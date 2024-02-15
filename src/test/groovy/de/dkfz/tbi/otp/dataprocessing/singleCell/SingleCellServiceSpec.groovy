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
package de.dkfz.tbi.otp.dataprocessing.singleCell

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataAllWellFileService
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataViewFileService
import de.dkfz.tbi.otp.ngsdata.FastqFile
import de.dkfz.tbi.otp.ngsdata.RawSequenceFile

import java.nio.file.Path
import java.nio.file.Paths

class SingleCellServiceSpec extends Specification implements DataTest, DomainFactoryCore {

    private static final String WELL = 'WELL'

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                RawSequenceFile,
        ]
    }

    private RawSequenceFile createRawSequenceFileHelper(String wellLabel = WELL, boolean singleCell = true, Map properties = [:]) {
        return createFastqFile([
                seqTrack: createSeqTrack([
                        singleCellWellLabel: wellLabel,
                        seqType            : createSeqType(singleCell: singleCell),
                ]),
        ] + properties)
    }

    void "buildMappingFileName, from RawSequenceFile"() {
        given:
        RawSequenceFile rawSequenceFile = createRawSequenceFileHelper()
        SingleCellService service = new SingleCellService()

        when:
        String entry = service.buildMappingFileName(rawSequenceFile)

        then:
        "${rawSequenceFile.individual.pid}_${rawSequenceFile.sampleType.name}_${SingleCellService.MAPPING_FILE_SUFFIX}" == entry
    }

    void "buildMappingFileName, from Individual and SampleType"() {
        given:
        RawSequenceFile rawSequenceFile = createRawSequenceFileHelper()
        SingleCellService service = new SingleCellService()

        when:
        String entry = service.buildMappingFileName(rawSequenceFile.individual, rawSequenceFile.sampleType)

        then:
        "${rawSequenceFile.individual.pid}_${rawSequenceFile.sampleType.name}_${SingleCellService.MAPPING_FILE_SUFFIX}" == entry
    }

    void "singleCellMappingFile, when all fine, then return path to mapping file"() {
        given:
        new TestConfigService()

        Path allWellDir = Paths.get('/vbpPath/0_all')
        RawSequenceFile rawSequenceFile = createRawSequenceFileHelper()

        SingleCellService service = new SingleCellService([
                rawSequenceDataAllWellFileService: Mock(RawSequenceDataAllWellFileService) {
                    1 * getAllWellDirectoryPath(rawSequenceFile) >> allWellDir
                }
        ])

        Path expected = allWellDir.resolve(service.buildMappingFileName(rawSequenceFile))

        when:
        Path path = service.singleCellMappingFile(rawSequenceFile)

        then:
        expected == path
    }

    void "mappingEntry, when all fine, then return entry"() {
        given:
        new TestConfigService()

        RawSequenceFile rawSequenceFile = createRawSequenceFileHelper()
        String rawSequenceFilePath = "pathToFastq/fastq.fastq"

        SingleCellService service = new SingleCellService(
                rawSequenceDataViewFileService: Mock(RawSequenceDataViewFileService) {
                    1 * getFilePath(_) >> Paths.get(rawSequenceFilePath)
                }
        )

        String expected = "${rawSequenceFilePath}\t${WELL}"

        when:
        String entry = service.mappingEntry(rawSequenceFile)

        then:
        expected == entry
    }
}
