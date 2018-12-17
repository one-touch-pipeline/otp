package de.dkfz.tbi.otp.ngsdata

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService


@Mock([
        GeneModel,
        ProcessingOption,
        ReferenceGenome,
])
@TestFor(GeneModelService)
class GeneModelServiceSpec extends Specification {
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
