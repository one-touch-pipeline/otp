package de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.validators

import grails.test.mixin.Mock
import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.BamMetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

@Mock([
        Individual,
        Project,
        Realm,
        Sample,
        SampleType
])
class SampleTypeIndividualValidatorSpec extends Specification {

    void 'validate, when column(s) is/are missing, adds error(s)'() {

        given:
        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext("""\
${header}
value1\tvalue2
""")

        when:
        new SampleTypeIndividualValidator().validate(context)

        then:
        containSame(context.problems*.message, messages)

        where:
        header                                                || messages
        "${BamMetadataColumn.SAMPLE_TYPE.name()}\tindividual" || ["Mandatory column 'INDIVIDUAL' is missing."]
        "sampleType\t${BamMetadataColumn.INDIVIDUAL.name()}"  || ["Mandatory column 'SAMPLE_TYPE' is missing."]
        "individual\tsampleType"                              || ["Mandatory column 'INDIVIDUAL' is missing.", "Mandatory column 'SAMPLE_TYPE' is missing."]
    }

    void 'validate, when combinations are in database, adds no problem'() {

        given:
        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext("""\
${BamMetadataColumn.SAMPLE_TYPE.name()}\t${BamMetadataColumn.INDIVIDUAL.name()}
SampleType1\tIndividual1\t
""")
        Individual individual = DomainFactory.createIndividual(pid: 'Individual1')
        SampleType sampleType = DomainFactory.createSampleType(name: 'SampleType1')
        DomainFactory.createSample(sampleType: sampleType, individual: individual)

        when:
        new SampleTypeIndividualValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when combinations are not in database, adds expected problem'() {
        given:
        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext("""\
${BamMetadataColumn.SAMPLE_TYPE.name()}\t${BamMetadataColumn.INDIVIDUAL.name()}
SampleType1\tIndividual1\t
""")
        Individual individual = DomainFactory.createIndividual(pid: 'anotherIndividual')
        SampleType sampleType = DomainFactory.createSampleType(name: 'anotherSampleType')
        DomainFactory.createSample(sampleType: sampleType, individual: individual)

        when:
        new SampleTypeIndividualValidator().validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        containSame(problem.affectedCells*.cellAddress, ['A2', 'B2'])
        problem.message.contains("The sample as combination of the individual 'Individual1' and the sample type 'SampleType1' is not registered in OTP.")
    }
}