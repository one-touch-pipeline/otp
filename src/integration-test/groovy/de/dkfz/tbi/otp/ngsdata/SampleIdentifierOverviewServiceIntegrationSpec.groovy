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
package de.dkfz.tbi.otp.ngsdata

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore

@Rollback
@Integration
class SampleIdentifierOverviewServiceIntegrationSpec extends Specification implements DomainFactoryCore {

    SampleIdentifierOverviewService sampleIdentifierOverviewService

    void "rawSequenceFilesOfProjectBySampleAndSeqType, basic testcase"() {
        given:
        Sample sample1 = createSample()
        Sample sample2 = createSample()
        SeqType seqType1 = createSeqType()
        SeqType seqType2 = createSeqType()

        List<SeqTrack> seqTracks = [
                createSeqTrack(sample: sample1, seqType: seqType1),
                createSeqTrack(sample: sample1, seqType: seqType2),
                createSeqTrack(sample: sample1, seqType: seqType2),
                createSeqTrack(sample: sample2, seqType: seqType1),
                createSeqTrack(sample: sample2, seqType: seqType2),
        ]

        List<RawSequenceFile> rawSequenceFiles = seqTracks.collect { SeqTrack seqTrack ->
            createFastqFile(seqTrack: seqTrack)
        }

        sampleIdentifierOverviewService = new SampleIdentifierOverviewService(
                rawSequenceFileService: Mock(RawSequenceFileService) {
                    _ * getAllRawSequenceFilesOfProject(_) >> rawSequenceFiles
                }
        )

        Map<List, List<RawSequenceFile>> expected = [
            [sample1, seqType1]: [rawSequenceFiles[0]],
            [sample1, seqType2]: rawSequenceFiles[1, 2],
            [sample2, seqType1]: [rawSequenceFiles[3]],
            [sample2, seqType2]: [rawSequenceFiles[4]],
        ]

        when:
        Map<List, List<RawSequenceFile>> result = sampleIdentifierOverviewService.rawSequenceFilesOfProjectBySampleAndSeqType(createProject())

        then:
        TestCase.assertContainSame(expected, result)
    }

    void "extractSampleIdentifiers, all combinations of DataFiles"() {
        given:
        List<SeqTrack> seqTracks = (0..5).collect {
            createSeqTrack()
        }

        List<RawSequenceFile> rawSequenceFiles = [
                // normal not withdrawn file
                createFastqFile(seqTrack: seqTracks[0], fileWithdrawn: false, withdrawnComment: null),
                // same comments, get combined
                createFastqFile(seqTrack: seqTracks[1], fileWithdrawn: true, withdrawnComment: "comment"),
                createFastqFile(seqTrack: seqTracks[1], fileWithdrawn: true, withdrawnComment: "comment"),
                // differing comments, get joined
                createFastqFile(seqTrack: seqTracks[2], fileWithdrawn: true, withdrawnComment: "commentA"),
                createFastqFile(seqTrack: seqTracks[2], fileWithdrawn: true, withdrawnComment: "commentB"),
                // comments, with html escaped character
                createFastqFile(seqTrack: seqTracks[3], fileWithdrawn: true, withdrawnComment: "comment \" comment"),
                // not withdrawn file, but comment given
                createFastqFile(seqTrack: seqTracks[4], fileWithdrawn: false, withdrawnComment: "comment"),
                // partially withdrawn
                createFastqFile(seqTrack: seqTracks[5], fileWithdrawn: false, withdrawnComment: null),
                createFastqFile(seqTrack: seqTracks[5], fileWithdrawn: true, withdrawnComment: "comment"),
        ]

        sampleIdentifierOverviewService = new SampleIdentifierOverviewService()

        List<Map> expected = [
                [text: seqTracks[0].sampleIdentifier, withdrawn: false, comments: ""],
                [text: seqTracks[1].sampleIdentifier, withdrawn: true,  comments: "comment"],
                [text: seqTracks[2].sampleIdentifier, withdrawn: true,  comments: "commentA; commentB"],
                [text: seqTracks[3].sampleIdentifier, withdrawn: true,  comments: "comment &quot; comment"],
                [text: seqTracks[4].sampleIdentifier, withdrawn: false, comments: "comment"],
                [text: seqTracks[5].sampleIdentifier, withdrawn: true,  comments: "comment"],
        ]

        when:
        List<Map> result = sampleIdentifierOverviewService.extractSampleIdentifiers(rawSequenceFiles)

        then:
        TestCase.assertContainSame(expected, result)
    }

    void "handleSampleIdentifierEntry, test map building"() {
        given:
        RawSequenceFile rawSequenceFile = createFastqFile(
                seqTrack: createSeqTrack(

                ),
        )
        Sample sample = rawSequenceFile.seqTrack.sample
        SeqType seqType = rawSequenceFile.seqTrack.seqType

        sampleIdentifierOverviewService = new SampleIdentifierOverviewService()

        Map sampleIdentifier = [
                text     : rawSequenceFile.seqTrack.sampleIdentifier,
                withdrawn: false,
                comments : "",
        ]

        Map expected = [
                individual      : [
                        id  : sample.individual.id,
                        name: sample.individual.pid,
                ],
                sampleType      : [
                        id  : sample.sampleType.id,
                        name: sample.sampleType.name,
                ],
                seqType         : [
                        id         : seqType.id,
                        displayText: seqType.displayNameWithLibraryLayout,
                ],
                sampleIdentifier: [sampleIdentifier],
        ]

        when:
        Map result = sampleIdentifierOverviewService.handleSampleIdentifierEntry(rawSequenceFile.seqTrack.sample, rawSequenceFile.seqTrack.seqType, [rawSequenceFile])

        then:
        TestCase.assertContainSame(expected, result)
    }
}
