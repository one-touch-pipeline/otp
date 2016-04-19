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

    static String parsableDeepSampleName = "02_MCF10A_CoAd_Ct1_H3K36me3_I_1"
    static String sampleName = "a_deep_sample"


    void "validate with data containing mismatching file name and sample ID using SampleIdentifierParser"() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${FASTQ_FILE}\t${SAMPLE_ID}\n" +
                "asdf.fastq\t${parsableDeepSampleName}"
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


    void "validate with correct data"(String document, String projectName) {
        given:
        Sample sample = DomainFactory.createSample(individual: DomainFactory.createIndividual(project: DomainFactory.createProject(name: projectName)))
        DomainFactory.createSampleIdentifier(name: sampleName, sample: sample).save(flush: true)

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(document)

        when:
        validator.validate(context)

        then:
        context.problems.empty

        where:
        document                                                                                  | projectName
        "${FASTQ_FILE}\t${SAMPLE_ID}\n${parsableDeepSampleName}.fastq\t${parsableDeepSampleName}" | "dont_care"
        "${FASTQ_FILE}\t${SAMPLE_ID}\n${sampleName}.fastq\t${sampleName}"                         | "DEEP"
        "${FASTQ_FILE}\t${SAMPLE_ID}\n"                                                           | "dont_care"
        "${FASTQ_FILE}\t${SAMPLE_ID}\nasdf\tasdf"                                                 | "dont_care"
        "${FASTQ_FILE}\t${SAMPLE_ID}\n${sampleName}.fastq\t${sampleName}"                         | "not_DEEP"
        "${FASTQ_FILE}\t${SAMPLE_ID}\nasdf.fastq\t${sampleName}"                                  | "not_DEEP"

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
