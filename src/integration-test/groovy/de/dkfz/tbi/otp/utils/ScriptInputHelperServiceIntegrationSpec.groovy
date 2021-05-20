/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.otp.utils

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.SeqTrack

@Rollback
@Integration
class ScriptInputHelperServiceIntegrationSpec extends Specification implements DomainFactoryCore {

    void "seqTracksByLaneDefinition, when input is valid, then return corresponding seqTracks"() {
        given:
        ScriptInputHelperService service = new ScriptInputHelperService()
        List<SeqTrack> seqTracks = createValidSeqTracks()

        String input = seqTracks.collect { SeqTrack seqTrack ->
            seqTracksByLaneDefinitionForSeqTrack(seqTrack)
        }.join('\n')

        expect:
        service.seqTracksByLaneDefinition(input) == seqTracks
    }

    @Unroll
    void "seqTracksByLaneDefinition, when input is invalid (#invalidProperty = #invalidValue), then throw exception"() {
        given:
        ScriptInputHelperService service = new ScriptInputHelperService()

        List<SeqTrack> seqTracks = createValidSeqTracks()
        SeqTrack invalidSeqTrack = createSeqTrack([
                seqType            : createSeqType([
                        singleCell: true,
                ]),
                singleCellWellLabel: withWellLabel ? 'label' : null,
        ])

        String input = seqTracks.collect { SeqTrack seqTrack ->
            seqTracksByLaneDefinitionForSeqTrack(seqTrack)
        }.join('\n') + '\n' + seqTracksByLaneDefinitionForSeqTrack(invalidSeqTrack, [(invalidProperty): invalidValue])

        when:
        service.seqTracksByLaneDefinition(input)

        then:
        AssertionError e = thrown()
        e.message.contains(errorText)

        where:
        withWellLabel | invalidProperty       | invalidValue                 || errorText
        false         | 'project'             | 'unknownProject'             || 'Could not find any project'
        false         | 'run'                 | 'unknownRunName'             || 'Could not find any run'
        false         | 'laneId'              | 'unknownLane'                || 'Could not find any seqTracks'
        true          | 'project'             | 'unknownProject'             || 'Could not find any project'
        true          | 'run'                 | 'unknownRunName'             || 'Could not find any run'
        true          | 'laneId'              | 'unknownLane'                || 'Could not find any seqTracks'
        true          | 'singleCellWellLabel' | 'unknownSingleCellWellLabel' || 'Could not find any seqTracks'
    }

    private List<SeqTrack> createValidSeqTracks() {
        return [
                createSeqTrack(),
                createSeqTrack([
                        seqType: createSeqType([
                                singleCell: true,
                        ]),
                ]),
                createSeqTrack([
                        seqType            : createSeqType([
                                singleCell: true,
                        ]),
                        singleCellWellLabel: 'label',
                ]),
        ]
    }

    private String seqTracksByLaneDefinitionForSeqTrack(SeqTrack seqTrack, Map invalidValue = [:]) {
        return ([
                project            : seqTrack.project.name,
                run                : seqTrack.run.name,
                laneId             : seqTrack.laneId,
                singleCellWellLabel: seqTrack.singleCellWellLabel ?: '',
        ] + invalidValue).values().join(',')
    }
}
