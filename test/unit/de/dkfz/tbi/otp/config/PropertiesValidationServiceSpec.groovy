package de.dkfz.tbi.otp.config

import grails.test.mixin.Mock
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqType

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.*

@Mock([
        ProcessingOption,
        SeqType,
])
class PropertiesValidationServiceSpec extends Specification {

    static final String INVALID = 'i\nvalid'
    static final String SEQ_TYPE_RODDY_NAME = 'seqTypeRoddyName'

    PropertiesValidationService propertiesValidationService


    def setup() {
        propertiesValidationService = new PropertiesValidationService()
        propertiesValidationService.processingOptionService = new ProcessingOptionService()
        DomainFactory.createSeqType(roddyName: SEQ_TYPE_RODDY_NAME)
    }


    @Unroll
    void "test validateProcessingOptionName"() {
        given:
        ProcessingOption processingOption = new ProcessingOption(
                name: name,
                value: value,
                type: type,
        )
        processingOption.save(validate: false, flush: true)

        expect:
        propertiesValidationService.validateProcessingOptionName(name, type)?.type == problem

        where:
        name                                      | value   | type                || problem
        PIPELINE_RODDY_SNV_DEFAULT_PLUGIN_NAME    | '{}'    | null                || null
        PIPELINE_RODDY_SNV_DEFAULT_PLUGIN_NAME    | INVALID | null                || OptionProblem.ProblemType.VALUE_INVALID
        PIPELINE_RODDY_SNV_DEFAULT_PLUGIN_NAME    | '{}'    | SEQ_TYPE_RODDY_NAME || OptionProblem.ProblemType.TYPE_INVALID
        PIPELINE_RODDY_SNV_DEFAULT_PLUGIN_VERSION | '1.2.3' | SEQ_TYPE_RODDY_NAME || null
        PIPELINE_RODDY_SNV_DEFAULT_PLUGIN_VERSION | INVALID | SEQ_TYPE_RODDY_NAME || OptionProblem.ProblemType.VALUE_INVALID
        PIPELINE_RODDY_SNV_DEFAULT_PLUGIN_VERSION | '1.2.3' | INVALID             || OptionProblem.ProblemType.TYPE_INVALID
        PIPELINE_RODDY_SNV_DEFAULT_PLUGIN_VERSION | '1.2.3' | null                || OptionProblem.ProblemType.TYPE_INVALID
        and: 'test deprecated processing option'
        PIPELINE_RODDY_ALIGNMENT_BWA_PATHS        | INVALID | null                || null
    }

    void "test validateProcessingOptionName, when option is missing"() {
        expect:
        propertiesValidationService.validateProcessingOptionName(PIPELINE_RODDY_SNV_DEFAULT_PLUGIN_VERSION, SEQ_TYPE_RODDY_NAME).type == OptionProblem.ProblemType.MISSING
    }
}
