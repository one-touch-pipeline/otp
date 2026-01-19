/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.dataswap.validators

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.dataCorrection.DataSwapService
import de.dkfz.tbi.otp.dataCorrection.validators.SeqTrackMatchesColumnsValidator
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.MessageSourceService
import de.dkfz.tbi.otp.utils.spreadsheet.Delimiter
import de.dkfz.tbi.otp.utils.spreadsheet.Spreadsheet
import de.dkfz.tbi.otp.utils.spreadsheet.validation.ValidationContext
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

class SeqTrackMatchesColumnValidatorSpec extends Specification implements DataTest, DomainFactoryCore {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                AntibodyTarget,
                ProcessingPriority,
                Project,
                Individual,
                SampleType,
                Sample,
        ]
    }

    DataSwapService service
    SeqTrackMatchesColumnsValidator validator

    void setup() {
        service = new DataSwapService()
        MessageSourceService messageSourceService = Mock(MessageSourceService) {
            _ * getMessage(_) >> { templateName, args -> templateName }
        }
        service.messageSourceService = messageSourceService
        validator = new SeqTrackMatchesColumnsValidator()
        validator.dataSwapService = service
        validator.messageSourceService = messageSourceService
    }

    void "validate, should run through when content is correct"() {
        given:
        Delimiter delimiter = Delimiter.COMMA
        SeqTrack seqTrack = createSeqTrack([antibodyTarget: createAntibodyTarget()])
        String header = service.dataSwapHeaders.join(delimiter.delimiter as String)
        // columns: id, ilse, run, lane, wellLabel, projectOld, projectNew, individualOld, individualNew,
        // sampleTypeOld, sampleTypeNew, seqTypeOld, seqTypeNew, libraryLayout, singleCellOld, singleCellNew,
        // sampleNameOld, sampleNameNew, antibodyTargetOld, antibodyTargetNew
        String content = [seqTrack.id, '', '', '', '', seqTrack.project.name, '', seqTrack.individual.pid, '',
                          seqTrack.sampleType.name, '', seqTrack.seqType.displayName, '', '', seqTrack.seqType.singleCell.toString(), '',
                          seqTrack.sampleIdentifier, '', seqTrack.antibodyTarget.name, ''].join(delimiter.delimiter as String)
        Spreadsheet spreadsheet = new Spreadsheet([header, content].join('\n'), delimiter)
        ValidationContext context = new ValidationContext(spreadsheet)

        when:
        validator.validate(context)

        then:
        context.problems.size() == 0
    }

    void "validate, should produce problems when seqTrack and old content doesnt fit"() {
        given:
        String wrongSeqTrackId = '123468'
        Delimiter delimiter = Delimiter.COMMA
        SeqTrack seqTrack1 = createSeqTrack([seqType: createSeqType([singleCell: false])])
        SeqTrack seqTrack2 = createSeqTrack([antibodyTarget: createAntibodyTarget(), seqType: createSeqType([singleCell: true])])
        SeqTrack seqTrack3 = createSeqTrack([antibodyTarget: createAntibodyTarget()])
        SeqTrack seqTrack4 = createSeqTrack([antibodyTarget: createAntibodyTarget()])
        String header = service.dataSwapHeaders.join(delimiter.delimiter as String)
        // columns: id, ilse, run, lane, wellLabel, projectOld, projectNew, individualOld, individualNew,
        // sampleTypeOld, sampleTypeNew, seqTypeOld, seqTypeNew, libraryLayout, singleCellOld, singleCellNew,
        // sampleNameOld, sampleNameNew, antibodyTargetOld, antibodyTargetNew
        // Empty singleCell values (seqTrack has singleCell=false by default), plus wrong antibodyTarget value.
        String content1 = [seqTrack1.id, '', '', '', '', seqTrack1.project.name, '', seqTrack1.individual.pid, '',
                           seqTrack1.sampleType.name, 'New Sample Type', seqTrack1.seqType.displayName, '', '', '', '',
                           seqTrack1.sampleIdentifier, '', 'Wrong Antibody Target', ''].join(delimiter.delimiter as String)
        // singleCell old is false which should be true, should trigger mismatch. Plus other mismatches: wrong project and wrong seqType values
        String content2 = [seqTrack2.id, '', '', '', '', 'Wrong Project', '', seqTrack2.individual.pid, '',
                           seqTrack2.sampleType.name, '', 'Wrong Seq Type', '', '', 'false', '',
                           seqTrack2.sampleIdentifier, '', seqTrack2.antibodyTarget.name, ''].join(delimiter.delimiter as String)
        // additional mismatches: wrong sampleType, singleCell and sampleIdentifier
        String content3 = [seqTrack3.id, '', '', '', '', seqTrack3.project.name, '', 'Wrong Individual Pid', 'New Individual',
                           'Wrong Sample Type', '', seqTrack3.seqType.displayName, '', '', 'Wrong Single Cell', '',
                           'Wrong Sample Identifier', '', seqTrack3.antibodyTarget.name, ''].join(delimiter.delimiter as String)
        // Testing column shift: singleCellNew is missing, causing subsequent columns to shift left
        String content4 = [seqTrack4.id, '', '', '', '', seqTrack4.project.name, '', seqTrack4.individual.pid, '',
                           seqTrack4.sampleIdentifier, '', seqTrack4.seqType.displayName, '', '', '', // missing single cell column
                           seqTrack4.sampleIdentifier, 'Sample name as single cell', seqTrack4.antibodyTarget.name, 'New antibody target'].join(delimiter.delimiter as String)
        // wrong seqTrack id row
        String content5 = [wrongSeqTrackId, '', '', '', '', '', '', 'Wrong Individual Pid', 'New Individual',
                           'Wrong Sample Type', '', '', '', '', '', '',
                           'Wrong Sample Identifier', '', '', ''].join(delimiter.delimiter as String)

        Spreadsheet spreadsheet = new Spreadsheet([header, content1, content2, content3, content4, content5].join('\n'), delimiter)
        ValidationContext context = new ValidationContext(spreadsheet)

        when:
        validator.validate(context)

        then:
        context.problems.size() == 14
        context.problems*.message.any { message -> seqTrack1.id && message.contains('Wrong Antibody Target') }
        context.problems*.message.any { message -> seqTrack1.id && message.contains('singleCell \"\"') && message.contains('singleCell \"false\"') }
        context.problems*.message.any { message -> seqTrack2.id && message.contains('Wrong Seq Type') }
        context.problems*.message.any { message -> seqTrack2.id && message.contains('Wrong Project') }
        context.problems*.message.any { message -> seqTrack2.id && message.contains('singleCell \"false\"') && message.contains('singleCell \"true\"') }
        context.problems*.message.any { message -> seqTrack3.id && message.contains('Wrong Sample Type')  }
        context.problems*.message.any { message -> seqTrack3.id && message.contains('Wrong Sample Identifier') }
        context.problems*.message.any { message -> seqTrack3.id && message.contains('Wrong Individual Pid') }
        context.problems*.message.any { message -> seqTrack3.id && message.contains('singleCell \"Wrong Single Cell\"') && message.contains('singleCell \"false\"') }
        context.problems*.message.any { message -> seqTrack4.id && message.contains('Sample name as single cell') }
        content4.split(delimiter.delimiter as String).size() == service.dataSwapHeaders.size() - 1
        context.problems*.message.any { message -> message.contains(wrongSeqTrackId) }
    }
}
