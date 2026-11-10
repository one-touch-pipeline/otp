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
package de.dkfz.tbi.otp.ngsdata

import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import org.springframework.http.HttpStatus
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore

class SeqTrackControllerSpec extends Specification implements ControllerUnitTest<SeqTrackController>, DataTest, DomainFactoryCore {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                Individual,
                SampleType,
                SeqType,
        ]
    }

    void "seqTrackSet, when all selection parameters exist, then return the model for the selection"() {
        given:
        Individual individual = createIndividual()
        SampleType sampleType = createSampleType()
        SeqType seqType = createSeqType()

        controller.seqTrackService = Mock(SeqTrackService) {
            1 * getSeqTrackSet(individual, sampleType, seqType) >> []
        }

        controller.request.method = 'GET'
        controller.params.individual = individual.id
        controller.params.sampleType = sampleType.id
        controller.params.seqType = seqType.id

        when:
        Map model = controller.seqTrackSet()

        then:
        controller.response.status == HttpStatus.OK.value()
        model.individual == individual
        model.sampleType == sampleType
        model.seqType == seqType
        model.seqTrackSet.numberOfLanes == 0
    }

    @Unroll
    void "seqTrackSet, when #parameter is #description, then respond with 404 and do not query SeqTracks"() {
        given:
        Map<String, Object> params = [
                individual: createIndividual().id,
                sampleType: createSampleType().id,
                seqType   : createSeqType().id,
        ]
        params[parameter] = value

        controller.seqTrackService = Mock(SeqTrackService) {
            0 * getSeqTrackSet(_, _, _)
        }

        controller.request.method = 'GET'
        controller.params.putAll(params.findAll { it.value != null })

        when:
        controller.seqTrackSet()

        then:
        controller.response.status == HttpStatus.NOT_FOUND.value()

        where:
        parameter    | value              | description
        'individual' | null               | 'missing'
        'individual' | Long.MAX_VALUE     | 'unknown'
        'individual' | 'abc'              | 'not a number'
        'sampleType' | null               | 'missing'
        'sampleType' | Long.MAX_VALUE     | 'unknown'
        'seqType'    | null               | 'missing'
        'seqType'    | Long.MAX_VALUE     | 'unknown'
    }
}
