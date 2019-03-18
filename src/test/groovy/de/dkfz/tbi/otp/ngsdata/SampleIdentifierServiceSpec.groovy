/*
 * Copyright 2011-2019 The OTP authors
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

import grails.test.mixin.Mock
import org.springframework.context.ApplicationContext
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.util.spreadsheet.Spreadsheet

@Mock([
        Realm,
        Project,
        SampleIdentifier,
        Individual,
        SampleType,
        Sample,
        ProcessingOption,
])
class SampleIdentifierServiceSpec extends Specification implements DomainFactoryCore {

    SampleIdentifierService sampleIdentifierService

    private String HEADER = SampleIdentifierService.BulkSampleCreationHeader.getHeaders(Spreadsheet.Delimiter.COMMA)

    void setup() {
        sampleIdentifierService = Spy(SampleIdentifierService) {
            findOrSaveSampleIdentifier(_) >> { }
        }
        sampleIdentifierService.applicationContext = Mock(ApplicationContext) {
            getBean(_) >> {
                return new BulkSampleCreationValidator()
            }
        }
    }

    @Unroll
    void "test createBulkSamples with correct values"() {
        given:
        List<String> output
        Project project = createProject()
        String context = "${HEADER}\n${useName ? project.name : ''},test,test,test"

        when:
        output = sampleIdentifierService.createBulkSamples(context, Spreadsheet.Delimiter.COMMA, project)

        then:
        output == []

        where:
        useName | _
        true    | _
        false   | _
    }

    void "test createBulkSamples when headers are missing"() {
        given:
        List<String> output
        Project project = createProject()

        when:
        output = sampleIdentifierService.createBulkSamples('invalidHeader\ntest,test,test', Spreadsheet.Delimiter.COMMA, project)

        then:
        CollectionUtils.containSame(output, [
                "Required column '${SampleIdentifierService.BulkSampleCreationHeader.PID.name()}' is missing.",
                "Required column '${SampleIdentifierService.BulkSampleCreationHeader.SAMPLE_TYPE.name()}' is missing.",
                "Required column '${SampleIdentifierService.BulkSampleCreationHeader.SAMPLE_IDENTIFIER.name()}' is missing.",
        ])
    }

    void "test createBulkSamples when given wrong projectName"() {
        given:
        List<String> output
        Project project = createProject()

        when:
        output = sampleIdentifierService.createBulkSamples("${HEADER}\ninvalidProject,test,test,test", Spreadsheet.Delimiter.COMMA, project)

        then:
        CollectionUtils.containSame(output, ["Could not find Project 'invalidProject'"])
    }
}
