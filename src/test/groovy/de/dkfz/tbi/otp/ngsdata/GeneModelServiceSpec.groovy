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
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService

class GeneModelServiceSpec extends Specification implements DataTest, ServiceUnitTest<GeneModelService> {

    @Override
    Class[] getDomainClassesToMock() {
        [
                GeneModel,
                ProcessingOption,
                ReferenceGenome,
        ]
    }

    GeneModel geneModel

    def setup() {
        service.referenceGenomeService = new ReferenceGenomeService()
        service.referenceGenomeService.processingOptionService = new ProcessingOptionService()
        ReferenceGenome referenceGenome = DomainFactory.createReferenceGenome(path: "1KGRef")
        geneModel = DomainFactory.createGeneModel(path: "geneModel", referenceGenome: referenceGenome)
        DomainFactory.createProcessingOptionBasePathReferenceGenome("/referenceGenomes")
    }


    void "test getFile"() {
        expect:
        service.getFile(geneModel) == new File("/referenceGenomes/1KGRef/gencode/geneModel/fileName.gtf")
    }

    void "test getFile fails with null"() {
        when:
        service.getFile(null)

        then:
        thrown(AssertionError)
    }

    void "test getExcludeFile"() {
        expect:
        service.getExcludeFile(geneModel) == new File("/referenceGenomes/1KGRef/gencode/geneModel/excludeFileName.gtf")
    }

    void "test getExcludeFile fails with null"() {
        when:
        service.getExcludeFile(null)

        then:
        thrown(AssertionError)
    }

    void "test getDexSeqFile"() {
        expect:
        service.getDexSeqFile(geneModel) == new File("/referenceGenomes/1KGRef/gencode/geneModel/dexSeqFileName.gtf")
    }

    void "test getDexSeqFile fails with null"() {
        when:
        service.getDexSeqFile(null)

        then:
        thrown(AssertionError)
    }

    void "test getGcFile"() {
        expect:
        service.getGcFile(geneModel) == new File("/referenceGenomes/1KGRef/gencode/geneModel/gcFileName.gtf")
    }

    void "test getGcFile fails with null"() {
        when:
        service.getGcFile(null)

        then:
        thrown(AssertionError)
    }
}
