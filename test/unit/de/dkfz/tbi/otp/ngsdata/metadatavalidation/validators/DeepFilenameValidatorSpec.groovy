package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import grails.test.mixin.*
import spock.lang.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.*


@Mock([
        Individual,
        ProcessingOption,
        Project,
        Sample,
        SampleIdentifier,
        SampleType,
])
public class DeepFilenameValidatorSpec extends Specification {

    DeepFilenameValidator validator = withSampleIdentifierService(new DeepFilenameValidator())

    void "validate with data containing mismatching file name and sample ID using SampleIdentifierParser"() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${FASTQ_FILE}\t${SAMPLE_ID}\n" +
                "asdf.fastq\t02_MCF10A_CoAd_Ct1_H3K36me3_I_1"
        )

        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.WARNING,
                        "In the DEEP project, name of FASTQ file should start with sample ID."),
        ]

        when:
        validator.validate(context)

        then:
        containSame(context.problems, expectedProblems)
    }


    void "validate with data containing mismatching file name and sample ID using existing SampleIdentifier"() {
        given:
        String sampleName = "a_deep_dample"
        Sample sample = DomainFactory.createSample(individual: DomainFactory.createIndividual(project: DomainFactory.createProject(name: "DEEP")))
        DomainFactory.createSampleIdentifier(name: sampleName, sample: sample).save(flush: true)

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${FASTQ_FILE}\t${SAMPLE_ID}\n" +
                "asdf.fastq\t${sampleName}"
        )

        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.WARNING,
                        "In the DEEP project, name of FASTQ file should start with sample ID."),
        ]

        when:
        validator.validate(context)

        then:
        containSame(context.problems, expectedProblems)
    }


    void "validate with correct data"(String document) {
        given:
        String sampleName = "a_deep_dample"
        Sample sample = DomainFactory.createSample(individual: DomainFactory.createIndividual(project: DomainFactory.createProject(name: "DEEP")))
        DomainFactory.createSampleIdentifier(name: sampleName, sample: sample).save(flush: true)

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(document)

        when:
        validator.validate(context)

        then:
        context.problems.empty

        where:
        document                                                                                              | _
        "${FASTQ_FILE}\t${SAMPLE_ID}\n"                                                                       | _
        "${FASTQ_FILE}\t${SAMPLE_ID}\nasdf\tasdf"                                                             | _
        "${FASTQ_FILE}\t${SAMPLE_ID}\n02_MCF10A_CoAd_Ct1_H3K36me3_I_1.fastq\t02_MCF10A_CoAd_Ct1_H3K36me3_I_1" | _
        "${FASTQ_FILE}\t${SAMPLE_ID}\n{sampleName}.fastq\t{sampleName}"                                       | _
    }


    private static DeepFilenameValidator withSampleIdentifierService(DeepFilenameValidator validator) {
        SampleIdentifierParser parser = new DeepSampleIdentifierParser()
        validator.sampleIdentifierService = [
                parseSampleIdentifier: { String sampleIdentifier ->
                    return parser.tryParse(sampleIdentifier)
                }
        ] as SampleIdentifierService
        return validator
    }
}
