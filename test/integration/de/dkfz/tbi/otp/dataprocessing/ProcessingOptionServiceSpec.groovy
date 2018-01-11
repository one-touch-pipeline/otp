package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import spock.lang.*

class ProcessingOptionServiceSpec extends Specification {
    def "test findOptionAsBoolean"() {
        given:
        DomainFactory.createProcessingOptionLazy(name: ProcessingOption.OptionName.GUI_SHOW_PARTNERS, value: input)

        expect:
        output == ProcessingOptionService.findOptionAsBoolean(ProcessingOption.OptionName.GUI_SHOW_PARTNERS, null, null)

        where:
        input   || output
        "true"  || true
        "false" || false
        "TRUE"  || false
        "tRuE"  || false
        "1"     || false
        "0"     || false
        ""      || false
    }
}
