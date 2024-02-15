/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp.infrastructure

import grails.testing.gorm.DataTest
import spock.lang.*

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.ProjectService

import java.nio.file.Paths

class RawSequenceDataAllWellFileServiceSpec extends Specification implements DataTest, DomainFactoryCore {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                FastqImportInstance,
                SeqTrack,
        ]
    }

    RawSequenceDataAllWellFileService service

    String seqDir = "/seq-dir"

    void setup() {
        service = new RawSequenceDataAllWellFileService()
        service.individualService = new IndividualService()
        service.rawSequenceDataViewFileService = new RawSequenceDataViewFileService([individualService: new IndividualService(
                [projectService: Mock(ProjectService) {
                    _ * getSequencingDirectory(_) >> Paths.get(seqDir)
                }]
        )])
    }

    @Unroll
    void "getFilePath, when antibody is '#antiBody' and single cell well label is defined, then path part is '#sampleTypePart'"() {
        given:
        String singleCellWellLabel = 'well'
        SeqType seqType = createSeqType([
                hasAntibodyTarget: antiBody as boolean,
                singleCell       : true,
        ])
        AntibodyTarget antibodyTarget = antiBody ? createAntibodyTarget([name: antiBody]) : null
        RawSequenceFile rawSequenceFile = createFastqFile([
                seqTrack: createSeqTrack([
                        seqType            : seqType,
                        sample             : createSample([
                                sampleType: createSampleType([
                                        name: sampleType,
                                ]),
                        ]),
                        antibodyTarget     : antibodyTarget,
                        singleCellWellLabel: singleCellWellLabel,
                ]),
        ])

        String expected = [
                seqDir,
                seqType.dirName,
                "view-by-pid",
                rawSequenceFile.individual.pid,
                sampleTypePart,
                seqType.libraryLayoutDirName,
                "run${rawSequenceFile.run.name}",
                rawSequenceFile.fileType.vbpPath,
                rawSequenceFile.vbpFileName,
        ].join('/')

        when:
        String path = service.getFilePath(rawSequenceFile)

        then:
        expected == path

        where:
        sampleType | antiBody    || sampleTypePart
        'CONTROL'  | null        || 'control/0_all'
        'CONTROL'  | 'anti-body' || 'control-anti-body/0_all'
    }

    @Unroll
    void "getFilePath, should throw assertion error when single cell is #singleCell and well label is #wellLabel"() {
        given:
        SeqType seqType = createSeqType([
                singleCell: singleCell,
        ])
        RawSequenceFile rawSequenceFile = createFastqFile([
                seqTrack: createSeqTrack([
                        seqType            : seqType,
                        sample             : createSample([
                                sampleType: createSampleType(),
                        ]),
                        singleCellWellLabel: wellLabel,
                ]),
        ])

        when:
        service.getFilePath(rawSequenceFile)

        then:
        thrown(AssertionError)

        where:
        singleCell | wellLabel || sampleTypePart
        true       | null      || 'control'
        false      | 'well'    || 'control-anti-body'
    }
}
