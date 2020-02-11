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

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService

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

    private DataFile createSingleCellDataFile() {
        return createDataFile([
                seqTrack: createSeqTrack([
                        singleCellWellLabel: WELL,
                        seqType            : createSeqType([
                                singleCell: true,
                        ]),
                ]),
        ])
    }

    void "singleCellMappingFile, when all fine, then return path to mapping file"() {
        given:
        new TestConfigService()

        String dirOfFastqs = '/tmp/0_all/a/b/c'
        String fastqFile = "${dirOfFastqs}/fastq.fastq"

        DataFile dataFile = createSingleCellDataFile()

        SingleCellService service = new SingleCellService([
                lsdfFilesService: Mock(LsdfFilesService) {
                    1 * getWellAllFileViewByPidPath(dataFile) >> fastqFile
                    0 * _
                },
        ])

        Path expected = Paths.get(dirOfFastqs, SingleCellService.MAPPING_FILE)

        when:
        Path path = service.singleCellMappingFile(dataFile)

        then:
        expected == path
    }

    void "mappingEntry, when all fine, then return entry"() {
        given:
        new TestConfigService()

        DataFile dataFile = createSingleCellDataFile()

        SingleCellService service = new SingleCellService()

        String expected = "${dataFile.vbpFileName}\t${WELL}"

        when:
        String entry = service.mappingEntry(dataFile)

        then:
        expected == entry
    }
}
