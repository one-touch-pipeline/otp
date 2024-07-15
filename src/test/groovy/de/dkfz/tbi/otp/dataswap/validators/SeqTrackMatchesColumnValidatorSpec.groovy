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
package de.dkfz.tbi.otp.dataswap.validators

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.dataCorrection.DataSwapService
import de.dkfz.tbi.otp.dataCorrection.validators.SeqTrackMatchesColumnsValidator
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.AntibodyTarget
import de.dkfz.tbi.otp.ngsdata.Individual
import de.dkfz.tbi.otp.ngsdata.Sample
import de.dkfz.tbi.otp.ngsdata.SampleType
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.MessageSourceService
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority
import de.dkfz.tbi.otp.utils.spreadsheet.Delimiter
import de.dkfz.tbi.otp.utils.spreadsheet.Spreadsheet
import de.dkfz.tbi.otp.utils.spreadsheet.validation.ValidationContext

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
        String content = [seqTrack.id, '', '', '', '', seqTrack.project.name, '', seqTrack.individual.pid, '',
                          seqTrack.sampleType.name, '', seqTrack.seqType.displayName, '', '', '',
                          seqTrack.sampleIdentifier, '', seqTrack.antibodyTarget.name].join(delimiter.delimiter as String)
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
        SeqTrack seqTrack1 = createSeqTrack()
        SeqTrack seqTrack2 = createSeqTrack([antibodyTarget: createAntibodyTarget()])
        SeqTrack seqTrack3 = createSeqTrack([antibodyTarget: createAntibodyTarget()])
        String header = service.dataSwapHeaders.join(delimiter.delimiter as String)
        String content1 = [seqTrack1.id, '', '', '', '', seqTrack1.project.name, '', seqTrack1.individual.pid, '',
                           seqTrack1.sampleType.name, 'New Sample Type', seqTrack1.seqType.displayName, '', '', '',
                           seqTrack1.sampleIdentifier, '', 'Wrong Antibody Target'].join(delimiter.delimiter as String)
        String content2 = [seqTrack2.id, '', '', '', '', 'Wrong Project', '', seqTrack2.individual.pid, '', seqTrack2.sampleType.name,
                           '', 'Wrong Seq Type', '', '', '', seqTrack2.sampleIdentifier,
                           '', seqTrack2.antibodyTarget.name].join(delimiter.delimiter as String)
        String content3 = [seqTrack3.id, '', '', '', '', seqTrack3.project.name, '', 'Wrong Individual Pid', 'New Individual',
                           'Wrong Sample Type', '', seqTrack3.seqType.displayName, '', '', '',
                           'Wrong Sample Identifier', '', seqTrack3.antibodyTarget.name].join(delimiter.delimiter as String)
        String content4 = [wrongSeqTrackId, '', '', '', '', '', '', 'Wrong Individual Pid', 'New Individual',
                           'Wrong Sample Type', '', '', '', '', '', 'Wrong Sample Identifier', '', ''].join(delimiter.delimiter as String)
        Spreadsheet spreadsheet = new Spreadsheet([header, content1, content2, content3, content4].join('\n'), delimiter)
        ValidationContext context = new ValidationContext(spreadsheet)

        when:
        validator.validate(context)

        then:
        context.problems.size() == 7
        context.problems*.message.any { message -> message.contains('Wrong Antibody Target') && seqTrack1.id }
        context.problems*.message.any { message -> message.contains('Wrong Seq Type') && seqTrack2.id }
        context.problems*.message.any { message -> message.contains('Wrong Project') && seqTrack2.id }
        context.problems*.message.any { message -> message.contains('Wrong Sample Type') && seqTrack3.id }
        context.problems*.message.any { message -> message.contains('Wrong Sample Identifier') && seqTrack3.id }
        context.problems*.message.any { message -> message.contains('Wrong Individual Pid') && seqTrack3.id }
        context.problems*.message.any { message -> message.contains(wrongSeqTrackId) }
    }
}
