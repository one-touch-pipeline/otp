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

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.*

class ScriptInputHelperServiceSpec extends Specification implements ServiceUnitTest<ScriptInputHelperService>, DataTest, DomainFactoryCore {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                SeqTrack,
        ]
    }

    @Unroll
    void "parseHelper, when input is '#input', then output is '#output'"() {
        expect:
        service.parseHelper(input) == output

        where:
        input                   || output
        ""                      || []
        "\n\n"                  || []
        "#test"                 || []
        "abc"                   || ["abc"]
        "abc\ndef"              || ["abc", "def"]
        "\nabc\n#test\ndef\n\n" || ["abc", "def"]
    }

    @Unroll
    void "parseAndSplitHelper, when input is '#input', then output is '#output'"() {
        expect:
        service.parseAndSplitHelper(input) == output

        where:
        input                           || output
        ""                              || []
        "\n\n"                          || []
        "#test"                         || []
        "abc"                           || [["abc"]]
        "abc def"                       || [["abc def"]]
        "abc,def"                       || [["abc", "def"]]
        "abc\tdef"                      || [["abc", "def"]]
        "abc;def"                       || [["abc", "def"]]
        "abc,def\nghi,jkl"              || [["abc", "def"], ["ghi", "jkl"]]
        "abc,def\nghi;jkl"              || [["abc", "def"], ["ghi", "jkl"]]
        "abc;def\nghi\tjkl"             || [["abc", "def"], ["ghi", "jkl"]]
        "\nabc,def\n#test\nghi;jkl\n\n" || [["abc", "def"], ["ghi", "jkl"]]
    }

    void "seqTrackById, when input is seqTrack ids with comment, then return corresponding seqTracks"() {
        given:
        List<SeqTrack> seqTracks = (1..3).collect {
            createSeqTrack()
        }
        String input = seqTracks*.id.join(",\'comment\'\n") + ",\'comment\'"

        expect:
        service.seqTrackById(input) == seqTracks
    }

    void "seqTrackById, when input contain unknown seqTrack ids, then throw exception"() {
        given:
        List<SeqTrack> seqTracks = (1..3).collect {
            createSeqTrack()
        }
        String input = seqTracks*.id.join('\n') + "\n-2"

        when:
        service.seqTrackById(input)

        then:
        thrown(AssertionError)
    }

    void "seqTracksBySampleDefinition, when input is valid, then return corresponding seqTracks"() {
        given:
        service.seqTypeService = new SeqTypeService()
        List<SeqTrack> seqTracks = (1..3).collect {
            createSeqTrack()
        }

        String input = seqTracks.collect { SeqTrack seqTrack ->
            sampleDefinitionForSeqTrack(seqTrack)
        }.join(',\'comment\'\n') + ",\'comment\'"

        expect:
        service.seqTracksBySampleDefinition(input) == seqTracks
    }

    @Unroll
    void "seqTracksBySampleDefinition, when input is invalid (#invalidProperty = #invalidValue), then throw exception"() {
        given:
        createSampleType(name: 'alternativeSampleType')
        service.seqTypeService = new SeqTypeService()
        List<SeqTrack> seqTracks = (1..3).collect {
            createSeqTrack()
        }
        SeqTrack invalidSeqTrack = createSeqTrack()

        String input = seqTracks.collect { SeqTrack seqTrack ->
            sampleDefinitionForSeqTrack(seqTrack)
        }.join(',\'comment\'\n') + ',\'comment\'\n' + sampleDefinitionForSeqTrack(invalidSeqTrack, [(invalidProperty): invalidValue]) + ",\'comment\'"

        when:
        service.seqTracksBySampleDefinition(input)

        then:
        AssertionError e = thrown()
        e.message.contains(errorText)

        where:
        invalidProperty | invalidValue                 || errorText
        'pid'           | 'unknownPid'                 || 'Could not find any individual'
        'sampleType'    | 'unknownSampleType'          || 'Could not find any sampleType'
        'sampleType'    | 'alternativeSampleType'      || 'Could not find any sample'
        'libraryLayout' | 'unknownSequencingReadType'  || 'is no valid sequencingReadType'
        'singleCell'    | 'unknownSingleCell'          || 'singleCell have to be either \'true\' or \'false\''
        'seqType'       | 'unknownSeqTypeName'         || 'Could not find seqType'
        'libraryLayout' | SequencingReadType.MATE_PAIR || 'Could not find seqType'
        'singleCell'    | 'true'                       || 'Could not find seqType'
    }

    void "getCommentsFromMultiLineDefinition, when receiving multi line string, then return only comments in quote marks"() {
        given:
        String stringListWithComment = """
                                        pid1,tumor,WGS,PAIRED,false,sampleName1
                                        pid3,control,WES,PAIRED,false,
                                        pid3,control,WES,PAIRED,false,'long withdrawn Comment
                                        with multiple Lines'
                                        pid5,control,RNA,SINGLE,true,sampleName2,'This
                                        is a multiline
                                        commentary'""".stripIndent()

        expect:
        service.getCommentsFromMultiLineDefinition(stringListWithComment) ==
                ["", "", 'long withdrawn Comment\nwith multiple Lines', "This\nis a multiline\ncommentary"]
    }

    void "removeCommentsFromStringList, when receiving string list, return string list without commentaries in quote marks"() {
        given:
        List<String> stringListWithComments = ["String1", 'String2', "'Commentary\nover multiple Lines with some special characters =)§\"(§'", "last String?"]

        expect:
        service.removeCommentsFromStringList(stringListWithComments) == ["String1", 'String2', "last String?"]
    }

    void "isCommentInMultiLineDefinition, when receiving multi line string with comment in every line, then return true"() {
        given:
        String multiLineString = """Input1, string2, 'Commentary'
                                    string3, string?!§4, 'commentary
                                    over multiple
                                    lines', string5""".stripIndent()

        expect:
        service.isCommentInMultiLineDefinition(multiLineString)
    }

    void "isCommentInMultiLineDefinition, when receiving multi line string with comment not in every line, then return false"() {
        given:
        String multiLineString = """Input1, string3
                                    Input1, string5, 'commentary'""".stripIndent()

        expect:
        !service.isCommentInMultiLineDefinition(multiLineString)
    }

    void "checkIfExactlyOneMultiLineStringContainsContent, when receiving List of multi line strings, should return true if exactly one is not empty"() {
        expect:
        service.checkIfExactlyOneMultiLineStringContainsContent(multiLineStringList) == containsExactlyOneMultiLineStringContainsContent

        where:
        multiLineStringList << [["""Input1, string2, 'Commentary'
                 string3, string?!§4, 'commentary
                 over multiple
                 lines', string5""",
                 """#Input1, string2, 'Commentary'
                 #string3, string?!§4""",
                 ""],
                ["""Input1, string2, 'Commentary'
                 string3, string?!§4, 'commentary
                 over multiple
                 lines', string5""",
                 """#Input1, string2, 'Commentary'
                 #string3, string?!§4"""],
                ["""Input1, string2, 'Commentary'
                 string3, string?!§4, 'commentary
                 over multiple
                 lines', string5""",
                 """Input1, string2, 'Commentary'
                 string3, string?!§4""",
                 ""],
                ["""Input1, string2, 'Commentary'
                 string3, string?!§4, 'commentary
                 over multiple
                 lines', string5""",
                 """Input1, string2, 'Commentary'
                 string3, string?!§4""",
                 "",
                 """#Input1, string2, 'Commentary'
                 #string3, string?!§4""",
                 ""],
        ]
        containsExactlyOneMultiLineStringContainsContent << [true, true , false, false]
    }

    private String sampleDefinitionForSeqTrack(SeqTrack seqTrack, Map invalidVaue = [:]) {
        return ([
                pid          : seqTrack.individual.pid,
                sampleType   : seqTrack.sampleType.name,
                seqType      : seqTrack.seqType.name,
                libraryLayout: seqTrack.seqType.libraryLayout,
                singleCell   : seqTrack.seqType.singleCell,
        ] + invalidVaue).values().join(',')
    }
}
