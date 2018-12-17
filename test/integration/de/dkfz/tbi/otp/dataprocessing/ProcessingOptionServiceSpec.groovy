package de.dkfz.tbi.otp.dataprocessing

import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.ngsdata.DomainFactory

class ProcessingOptionServiceSpec extends Specification {

    ProcessingOptionService processingOptionService

    @Unroll
    def "test findOptionAsBoolean"() {
        given:
        DomainFactory.createProcessingOptionLazy(name: ProcessingOption.OptionName.GUI_SHOW_PARTNERS, value: input)

        expect:
        output == processingOptionService.findOptionAsBoolean(ProcessingOption.OptionName.GUI_SHOW_PARTNERS)

        where:
        input   || output
        "true"  || true
        "false" || false
    }
}
