package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import grails.test.mixin.*
import spock.lang.*

import static de.dkfz.tbi.TestCase.*
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Mock([
        Individual,
        ProcessingOption,
        Project,
        Sample,
        SampleIdentifier,
        SampleType,
])
class SampleProjectValidatorSpec extends Specification {

    void 'validate adds expected warnings'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext((
                "${PROJECT} ${SAMPLE_ID}\n" +
                        "A not_parseable\n" +  // neither in DB nor can be parsed
                        "B project_B\n" +      // SampleIdentifier in DB, name matches
                        "C project_C\n" +      // SampleIdentifier in DB, nameInMetadataFiles matches
                        "D project_D\n" +      // SampleIdentifier in DB, neither name nor nameInMetadataFiles matches
                        "E project_E\n" +      // SampleIdentifier not in DB, name matches
                        "F project_V\n" +      // SampleIdentifier not in DB, nameInMetadataFiles matches
                        "G project_W\n"        // SampleIdentifier not in DB, neither name nor nameInMetadataFiles matches
        ).replace(' ', '\t'))
        createSampleIdentifier('project_B', 'B', 'X')
        createSampleIdentifier('project_C', 'Y', 'C')
        createSampleIdentifier('project_D', 'Z', 'Z')
        DomainFactory.createProject(name: 'V', nameInMetadataFiles: 'F')
        SampleProjectValidator validator = new SampleProjectValidator()
        validator.sampleIdentifierService = [
                parseSampleIdentifier: { String sampleIdentifier ->
                    final String prefix = 'project_'
                    if (sampleIdentifier.startsWith(prefix)) {
                        return new DefaultParsedSampleIdentifier(sampleIdentifier.substring(prefix.length()), "don't care", "don't care", sampleIdentifier)
                    } else {
                        return null
                    }
                }
        ] as SampleIdentifierService
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[3].cells as Set<Cell>, Level.WARNING,
                        "Sample identifier 'project_D' is already registered in OTP with project 'Z', not with project 'D'. If you ignore this warning, OTP will keep the assignment of the sample identifier to project 'Z' and ignore the value 'D' in the '${PROJECT}' column."),
                new Problem(context.spreadsheet.dataRows[6].cells as Set<Cell>, Level.WARNING,
                        "Sample identifier 'project_W' looks like it belongs to project 'W', not to project 'G'. If you ignore this warning, OTP will assign the sample to project 'W' and ignore the value 'G' in the '${PROJECT}' column."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    private static SampleIdentifier createSampleIdentifier(String sampleIdentifierName, String projectName, String projectNameInMetadataFiles) {
        return DomainFactory.createSampleIdentifier(
                sample: DomainFactory.createSample(
                        individual: DomainFactory.createIndividual(
                                project: DomainFactory.createProject(
                                        name: projectName,
                                        nameInMetadataFiles: projectNameInMetadataFiles,
                                ),
                        ),
                ),
                name: sampleIdentifierName,
        )
    }
}
