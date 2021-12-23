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
import grails.gorm.transactions.Rollback
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.SeqTrackWithComment

@Rollback
@Integration
class ScriptInputHelperServiceIntegrationSpec extends Specification implements DomainFactoryCore {

    void "seqTracksByLaneDefinition, when input is valid, then return corresponding seqTracks"() {
        given:
        ScriptInputHelperService service = new ScriptInputHelperService()
        List<SeqTrackWithComment> seqTrackWithComments = createValidSeqTracks()

        String input = seqTrackWithComments.collect { SeqTrackWithComment trackWithComment ->
            seqTracksByLaneDefinitionForSeqTrack(trackWithComment)
        }.join('\n')

        expect:
        service.seqTracksByLaneDefinition(input) == seqTrackWithComments
    }

    @Unroll
    void "seqTracksByLaneDefinition, when input is invalid (#invalidProperty = #invalidValue), then throw exception"() {
        given:
        ScriptInputHelperService service = new ScriptInputHelperService()

        List<SeqTrackWithComment> seqTrackWithComments = createValidSeqTracks()
        SeqTrackWithComment invalidSeqTrack = new SeqTrackWithComment(createSeqTrack([
                seqType            : createSeqType([
                        singleCell: true,
                ]),
                singleCellWellLabel: withWellLabel ? 'label' : null,
        ]), "comment")

        String input = seqTrackWithComments.collect { SeqTrackWithComment seqTrackWithComment ->
            seqTracksByLaneDefinitionForSeqTrack(seqTrackWithComment)
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
        false         | 'comment'             | ''                           || 'A multi input for lane is defined by 5 columns' //empty columns at the end are skipped, so column count is incorrect
        false         | 'comment'             | "''"                         || 'Comment may not be empty'
        true          | 'project'             | 'unknownProject'             || 'Could not find any project'
        true          | 'run'                 | 'unknownRunName'             || 'Could not find any run'
        true          | 'laneId'              | 'unknownLane'                || 'Could not find any seqTracks'
        true          | 'singleCellWellLabel' | 'unknownSingleCellWellLabel' || 'Could not find any seqTracks'
        true          | 'comment'             | ''                           || 'A multi input for lane is defined by 5 columns' //empty columns at the end are skipped, so column count is incorrect
        true          | 'comment'             | "''"                         || 'Comment may not be empty'
    }

    private List<SeqTrackWithComment> createValidSeqTracks() {
        return [
                new SeqTrackWithComment(createSeqTrack(), "a comment"),
                new SeqTrackWithComment(createSeqTrack([
                        seqType: createSeqType([
                                singleCell: true,
                        ]),
                ]), "a\nmultiline\ncomment"),
                new SeqTrackWithComment(createSeqTrack([
                        seqType            : createSeqType([
                                singleCell: true,
                        ]),
                        singleCellWellLabel: 'label',
                ]), "a 'quotes' 'containing' comment"),
        ]
    }

    private String seqTracksByLaneDefinitionForSeqTrack(SeqTrackWithComment seqTrackWithComment, Map invalidValue = [:]) {
        return ([
                project            : seqTrackWithComment.seqTrack.project.name,
                run                : seqTrackWithComment.seqTrack.run.name,
                laneId             : seqTrackWithComment.seqTrack.laneId,
                singleCellWellLabel: seqTrackWithComment.seqTrack.singleCellWellLabel ?: '',
                comment            : "'${seqTrackWithComment.comment}'",
        ] + invalidValue).values().join(',')
    }
}
