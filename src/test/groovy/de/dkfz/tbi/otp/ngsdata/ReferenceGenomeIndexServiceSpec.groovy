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

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeIndexService
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService

class ReferenceGenomeIndexServiceSpec extends Specification implements DataTest, ServiceUnitTest<ReferenceGenomeIndexService> {

    @Override
    Class[] getDomainClassesToMock() {
        [
                ProcessingOption,
                ReferenceGenome,
                ReferenceGenomeIndex,
                ToolName,
        ]
    }

    ReferenceGenomeIndex referenceGenomeIndex

    def setup() {
        service.referenceGenomeService = new ReferenceGenomeService()
        service.referenceGenomeService.processingOptionService = new ProcessingOptionService()
        ReferenceGenome referenceGenome = DomainFactory.createReferenceGenome(path: "1KGRef")
        ToolName toolName = DomainFactory.createToolName(path: "toolName")
        referenceGenomeIndex = DomainFactory.createReferenceGenomeIndex(path: "path", toolName: toolName, referenceGenome: referenceGenome)
        DomainFactory.createProcessingOptionBasePathReferenceGenome("/referenceGenomes")
    }

    void "test getFile"() {
        expect:
        service.getFile(referenceGenomeIndex) == new File("/referenceGenomes/1KGRef/indexes/toolName/path")
    }

    void "test getFile fails with null"() {
        when:
        service.getFile(null)

        then:
        thrown(AssertionError)
    }
}
