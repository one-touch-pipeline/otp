package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import grails.test.mixin.Mock
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import java.util.regex.Matcher
import java.util.regex.Pattern

import static de.dkfz.tbi.TestCase.assertContainSame
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.PROJECT
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.SAMPLE_ID
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

@Mock([
        Individual,
        ProcessingOption,
        Project,
        ProjectCategory,
        Realm,
        Sample,
        SampleIdentifier,
        SampleType,
])
class SampleValidatorSpec extends Specification {

    static final Pattern PATTERN = Pattern.compile(/^P-([^ ]+)_I-([^ ]+)_S-([^ ]+)$/)
    static final String SAMPLE_Z = "P-X_I-Y_S-Z"
    static final String SAMPLE_N = "P-B_I-M_S-N"

    SampleValidator validator = withSampleIdentifierService(new SampleValidator())

    void 'validate, when identifier is not parseable but in DB, succeeds'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_ID}\t${PROJECT}\nABC")
        DomainFactory.createSampleIdentifier(name: 'ABC')

        when:
        validator.validate(context)

        then:
        context.problems.isEmpty()
    }

    void 'validate, when identifiers are neither parseable nor in DB, adds errors and info'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_ID}\nABC\nAAA")
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.ERROR,
                        "Sample identifier 'ABC' is neither registered in OTP nor matches a pattern known to OTP.", "At least one sample identifier is neither registered in OTP nor matches a pattern known to OTP."),
                new Problem(context.spreadsheet.dataRows[1].cells as Set, Level.ERROR,
                        "Sample identifier 'AAA' is neither registered in OTP nor matches a pattern known to OTP.", "At least one sample identifier is neither registered in OTP nor matches a pattern known to OTP."),
                new Problem(Collections.emptySet(), Level.INFO,
                        "All sample identifiers which are neither registered in OTP nor match a pattern known to OTP:\nAAA\nABC"),
        ]
        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when identifier is not in DB but parseable and project is not in DB, adds error'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_ID}\t${PROJECT}\n${SAMPLE_Z}")

        when:
        validator.validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        problem.message == "Sample identifier '${SAMPLE_Z}' is not registered in OTP. It looks like it belongs to project 'X', but no project with that name is registered in OTP."
    }

    void 'validate, when identifier is not in DB but parseable and project is not in DB but individual is, adds errors'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_ID}\t${PROJECT}\n${SAMPLE_Z}")
        Individual individual = DomainFactory.createIndividual(pid: 'Y')
        Collection<Problem> expectedProblems = [
                new Problem([context.spreadsheet.dataRows[0].cells[0], context.spreadsheet.dataRows[0].cells[1]] as Set, Level.ERROR,
                        "Sample identifier '${SAMPLE_Z}' is not registered in OTP. It looks like it belongs to project 'X', but no project with that name is registered in OTP.", "At least one sample identifier is not registered in OTP. It looks like it belongs to a project not registered in OTP."),
                new Problem([context.spreadsheet.dataRows[0].cells[0], context.spreadsheet.dataRows[0].cells[1]] as Set, Level.ERROR,
                        "Sample identifier '${SAMPLE_Z}' is not registered in OTP. It looks like it belongs to project 'X' and individual 'Y', but individual 'Y' is already registered in OTP with project '${individual.project.name}'.", "At least one sample identifier is not registered in OTP. It looks like it belongs to a specific project and individual, but this individual is already registered in OTP with another project."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when identifier is not in DB but parseable and individual belongs to different project, adds error'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_ID}\t${PROJECT}\n${SAMPLE_Z}")
        DomainFactory.createProject(name: 'X')
        Individual individual = DomainFactory.createIndividual(pid: 'Y')

        when:
        validator.validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        problem.message == "Sample identifier '${SAMPLE_Z}' is not registered in OTP. It looks like it belongs to project 'X' and individual 'Y', but individual 'Y' is already registered in OTP with project '${individual.project.name}'.".toString()
    }

    void 'validate, when identifier is not in DB but parseable and project is in DB, succeeds'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_ID}\t${PROJECT}\n${SAMPLE_Z}")
        DomainFactory.createProject(name: 'X')

        when:
        validator.validate(context)

        then:
        context.problems.isEmpty()
    }

    void 'validate, when identifier is in DB and parseable and project is inconsistent, adds warning'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_ID}\t${PROJECT}\n${SAMPLE_Z}")
        createSampleIdentifier(context.spreadsheet.dataRows.get(0).cells.get(0).text, 'A', 'Y', 'Z')

        when:
        validator.validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.WARNING
        problem.message == "Sample identifier '${SAMPLE_Z}' looks like it belongs to project 'X', but it is already registered in OTP with project 'A'. If you ignore this warning, OTP will keep the assignment of the sample identifier to project 'A'."
    }

    void 'validate, when identifier is in DB and parseable and individual is inconsistent, adds warning'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_ID}\t${PROJECT}\n${SAMPLE_Z}")
        createSampleIdentifier(context.spreadsheet.dataRows.get(0).cells.get(0).text, 'X', 'B', 'Z')

        when:
        validator.validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.WARNING
        problem.message == "Sample identifier '${SAMPLE_Z}' looks like it belongs to individual 'Y', but it is already registered in OTP with individual 'B'. If you ignore this warning, OTP will keep the assignment of the sample identifier to individual 'B'."
    }

    void 'validate, when identifier is in DB and parseable and sample type is inconsistent, adds warning'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_ID}\t${PROJECT}\n${SAMPLE_Z}")
        createSampleIdentifier(context.spreadsheet.dataRows.get(0).cells.get(0).text, 'X', 'Y', 'C')

        when:
        validator.validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.WARNING
        problem.message == "Sample identifier '${SAMPLE_Z}' looks like it belongs to sample type 'Z', but it is already registered in OTP with sample type 'C' If you ignore this warning, OTP will keep the assignment of the sample identifier to sample type 'C'."
    }

    void 'validate, when identifiers belong to different projects, adds warning'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_ID}\n" +
                        "SampleA\n" +
                        "SampleB\n" +
                        "SampleC\n" +
                        "${SAMPLE_N}\n" +
                        "P-C_I-M_S-N\n")
        createSampleIdentifier('SampleB', 'B', 'W', 'X')
        createSampleIdentifier('SampleC', 'C', 'Y', 'Z')
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[1].cells +
                        context.spreadsheet.dataRows[2].cells +
                        context.spreadsheet.dataRows[3].cells +
                        context.spreadsheet.dataRows[4].cells as Set, Level.WARNING,
                        "The sample identifiers belong to different projects:\nProject 'B': '${SAMPLE_N}', 'SampleB'\nProject 'C': 'P-C_I-M_S-N', 'SampleC'"),
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.ERROR,
                        "Sample identifier 'SampleA' is neither registered in OTP nor matches a pattern known to OTP.", "At least one sample identifier is neither registered in OTP nor matches a pattern known to OTP."),
                new Problem(Collections.emptySet(), Level.INFO,
                        "All sample identifiers which are neither registered in OTP nor match a pattern known to OTP:\nSampleA"),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when all known identifiers belong to the same project, adds no warning about different projects'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_ID}\n" +
                        "SampleA\n" +
                        "SampleB\n" +
                        "${SAMPLE_N}\n")
        createSampleIdentifier('SampleB', 'B', 'W', 'X')
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.ERROR,
                        "Sample identifier 'SampleA' is neither registered in OTP nor matches a pattern known to OTP.", "At least one sample identifier is neither registered in OTP nor matches a pattern known to OTP."),
                new Problem(Collections.emptySet(), Level.INFO,
                        "All sample identifiers which are neither registered in OTP nor match a pattern known to OTP:\nSampleA"),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when PROJECT column is missing and all identifiers are known and belong to the same project, adds info'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_ID}\n" +
                        "SampleB\n" +
                        "${SAMPLE_N}\n")
        createSampleIdentifier('SampleB', 'B', 'W', 'X')
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells + context.spreadsheet.dataRows[1].cells as Set, Level.INFO,
                        "All sample identifiers belong to project 'B'."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when PROJECT column is present and all identifiers are known and belong to the same project, adds no problem'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_ID}\t${PROJECT}\n" +
                        "SampleB\n" +
                        "${SAMPLE_N}\n")
        createSampleIdentifier('SampleB', 'B', 'W', 'X')

        when:
        validator.validate(context)

        then:
        context.problems.isEmpty()
    }

    void 'validate, when sample identifier contains not allowed characters, adds warning'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_ID}\t${PROJECT}\nP:X I:Y S:Z")
        createSampleIdentifier(context.spreadsheet.dataRows.get(0).cells.get(0).text, 'X', 'Y', 'Z')

        when:
        validator.validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.WARNING
        problem.message == "Sample identifier 'P:X I:Y S:Z' contains not allowed characters."
        problem.type == "Sample identifiers are only allowed with the characters [A-Za-z0-9_-]"
    }

    private
    static SampleIdentifier createSampleIdentifier(String sampleIdentifierName, String projectName, String pid, String sampleTypeName) {
        return DomainFactory.createSampleIdentifier(
                sample: DomainFactory.createSample(
                        individual: DomainFactory.createIndividual(
                                project: DomainFactory.createProject(
                                        name: projectName,
                                ),
                                pid: pid,
                        ),
                        sampleType: DomainFactory.createSampleType(
                                name: sampleTypeName,
                        ),
                ),
                name: sampleIdentifierName,
        )
    }

    private static SampleValidator withSampleIdentifierService(SampleValidator validator) {
        validator.sampleIdentifierService = [
                parseSampleIdentifier: { String sampleIdentifier, Project project ->
                    Matcher matcher = PATTERN.matcher(sampleIdentifier)
                    if (matcher.matches()) {
                        return new DefaultParsedSampleIdentifier(matcher.group(1), matcher.group(2), matcher.group(3), sampleIdentifier)
                    } else {
                        return null
                    }
                }
        ] as SampleIdentifierService
        return validator
    }
}
