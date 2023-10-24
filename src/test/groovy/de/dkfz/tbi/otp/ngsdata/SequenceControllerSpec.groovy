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
package de.dkfz.tbi.otp.ngsdata

import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

import static javax.servlet.http.HttpServletResponse.SC_OK

class SequenceControllerSpec extends Specification implements ControllerUnitTest<SequenceController>, DataTest, DomainFactoryCore {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                RawSequenceFile,
                ProcessingPriority,
                Project,
                SeqTrack,
                Sequence,
        ]
    }

    def "test exportAll to ensure that both header and content have the correct number of columns"() {
        given:
        Sequence sequence = DomainFactory.createSequence()
        createSeqTrack(id: sequence.seqTrackId)
        controller.seqTrackService = Mock(SeqTrackService) {
            1 * listSequences(*_) >> [sequence]
        }
        int expectedNumberOfColumns = (SequenceColumn.values() - SequenceColumn.FASTQC).size()

        when:
        controller.exportAll()

        then:
        controller.response.status == SC_OK
        (controller.response.text as String).split("\n").every {
            it.split(",", -1).size() == expectedNumberOfColumns
        }
    }
}
