package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import grails.test.mixin.*
import spock.lang.*


@Mock([
        ProcessingOption,
        ReferenceGenome,
        ReferenceGenomeIndex,
        ToolName,
])
@TestFor(ReferenceGenomeIndexService)
class ReferenceGenomeIndexServiceSpec extends Specification {
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
