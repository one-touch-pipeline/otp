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

import java.nio.file.*

class RawSequenceDataViewFileServiceSpec extends Specification implements DataTest, DomainFactoryCore {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                FastqImportInstance,
                SeqTrack,
        ]
    }

    RawSequenceDataViewFileService service

    String seqDir = "/seq-dir"

    void setup() {
        service = new RawSequenceDataViewFileService()
        service.individualService = new IndividualService()
        service.individualService.projectService = Mock(ProjectService) {
            getSequencingDirectory(_) >> Paths.get(seqDir)
        }
    }

    private Map<String, ?> setUpViewByPidTests(String antiBody, String well, String sampleType, String sampleTypeDirPart) {
        SeqType seqType = createSeqType([
                hasAntibodyTarget: antiBody as boolean,
                singleCell       : well as boolean,
        ])
        AntibodyTarget antibodyTarget = antiBody ? createAntibodyTarget([
                name: antiBody,
        ]) : null
        RawSequenceFile rawSequenceFile = createFastqFile([
                seqTrack: createSeqTrack([
                        seqType            : seqType,
                        sample             : createSample([
                                sampleType: createSampleType([
                                        name: sampleType,
                                ]),
                        ]),
                        antibodyTarget     : antibodyTarget,
                        singleCellWellLabel: well,
                ]),
        ])

        String expected = [
                seqDir,
                seqType.dirName,
                "view-by-pid",
                rawSequenceFile.individual.pid,
                sampleTypeDirPart,
                seqType.libraryLayoutDirName,
                "run${rawSequenceFile.run.name}",
                rawSequenceFile.fileType.vbpPath,
                rawSequenceFile.vbpFileName,
        ].join('/')

        return [
                rawSequenceFile: rawSequenceFile,
                expected: expected,
        ]
    }

    @Unroll
    void "getFilePath, when antibody is '#antiBody' and single cell well is '#well', then path part is '#sampleTypePart'"() {
        given:
        Map<String, ?> data = setUpViewByPidTests(antiBody, well, sampleType, sampleTypePart)

        when:
        String path = service.getFilePath(data.rawSequenceFile)

        then:
        data.expected == path

        where:
        sampleType | antiBody    | well   || sampleTypePart
        'control'  | null        | null   || 'control'
        'Control'  | null        | null   || 'control'
        'CONTROL'  | null        | null   || 'control'
        'CONTROL'  | 'anti-body' | null   || 'control-anti-body'
        'CONTROL'  | null        | 'well' || 'control/well'
        'CONTROL'  | 'anti-body' | 'well' || 'control-anti-body/well'
    }

    void "getFilePath, when datafile is an unaligned single cell bam file, then return expected path"() {
        given:
        SeqTrack seqTrack = createSeqTrack()

        RawSequenceFile rawSequenceFile = createFastqFile([
                seqTrack    : seqTrack,
        ])

        String expected = [
                seqDir,
                seqTrack.seqType.dirName,
                "view-by-pid",
                seqTrack.individual.pid,
                seqTrack.sampleType.dirName,
                seqTrack.seqType.libraryLayoutDirName,
                "run${seqTrack.run.name}",
                rawSequenceFile.fileType.vbpPath,
                rawSequenceFile.vbpFileName,
        ].join('/')

        when:
        String path = service.getFilePath(rawSequenceFile)

        then:
        expected == path
    }
}
