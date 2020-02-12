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
package de.dkfz.tbi.otp.job.jobs.dataInstallation

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellMappingFileService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.job.processing.Artefact
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.LinkFileUtils

class CreateSingleCellAllLinkJobSpec extends Specification implements DataTest, DomainFactoryCore {

    @Override
    Class[] getDomainClassesToMock() {
        [
                DataFile,
                ProcessingStep,
                Artefact,
        ]
    }

    private CreateSingleCellAllLinkJob createSingleCellAllLinkJob

    private ProcessingStep step

    void setup() {
        new TestConfigService()
        step = DomainFactory.createProcessingStep()
        createSingleCellAllLinkJob = new CreateSingleCellAllLinkJob()
        createSingleCellAllLinkJob.processingStep = step

        createSingleCellAllLinkJob.lsdfFilesService = new LsdfFilesService()
    }

    void "test execute when everything is fine"() {
        given:
        SeqTrack seqTrack = createSeqTrack([
                seqType            : createSeqType([
                        singleCell: true,
                ]),
                singleCellWellLabel: 'WELL',
        ])

        DataFile dataFile = createDataFile([
                seqTrack      : seqTrack,
                fileExists    : true,
                dateFileSystem: new Date(),
                fileSize      : DomainFactory.counter++,
        ])

        File source = new File(createSingleCellAllLinkJob.lsdfFilesService.getFileFinalPath(dataFile))
        File target = new File(createSingleCellAllLinkJob.lsdfFilesService.getWellAllFileViewByPidPath(dataFile))

        DomainFactory.createProcessParameter([
                process  : step.process,
                value    : seqTrack.id,
                className: SeqTrack.name,
        ])

        createSingleCellAllLinkJob.linkFileUtils = Mock(LinkFileUtils) {
            1 * createAndValidateLinks([
                    (source): target
            ], _, _)
        }
        createSingleCellAllLinkJob.singleCellMappingFileService = Mock(SingleCellMappingFileService) {
            1 * addMappingFileEntryIfMissing(dataFile)
        }

        when:
        createSingleCellAllLinkJob.execute()

        then:
        noExceptionThrown()
    }
}
