package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import grails.test.mixin.*
import spock.lang.*

import java.util.regex.*

import static de.dkfz.tbi.TestCase.*
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.*

@Mock([
        Individual,
        ProcessingOption,
        Project,
        ProjectCategory,
        Sample,
        SampleIdentifier,
        SampleType,
])
class SampleValidatorSpec extends Specification {

    static final Pattern PATTERN = Pattern.compile(/^P:([^ ]+) I:([^ ]+) S:([^ ]+)$/)

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
                        "Sample identifier 'ABC' is neither registered in OTP nor matches a pattern known to OTP."),
                new Problem(context.spreadsheet.dataRows[1].cells as Set, Level.ERROR,
                        "Sample identifier 'AAA' is neither registered in OTP nor matches a pattern known to OTP."),
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
                "${SAMPLE_ID}\t${PROJECT}\nP:X I:Y S:Z")

        when:
        validator.validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        problem.message == "Sample identifier 'P:X I:Y S:Z' is not registered in OTP. It looks like it belongs to project 'X', but no project with that name is registered in OTP."
    }

    void 'validate, when identifier is not in DB but parseable and project is not in DB but individual is, adds errors'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_ID}\t${PROJECT}\nP:X I:Y S:Z")
        Individual individual = DomainFactory.createIndividual(pid: 'Y')
        Collection<Problem> expectedProblems = [
                new Problem([context.spreadsheet.dataRows[0].cells[0]] as Set, Level.ERROR,
                        "Sample identifier 'P:X I:Y S:Z' is not registered in OTP. It looks like it belongs to project 'X', but no project with that name is registered in OTP."),
                new Problem([context.spreadsheet.dataRows[0].cells[0]] as Set, Level.ERROR,
                        "Sample identifier 'P:X I:Y S:Z' is not registered in OTP. It looks like it belongs to project 'X' and individual 'Y', but individual 'Y' is already registered in OTP with project '${individual.project.name}'."),
        ]

        when:
        validator.validate(context)

        then:
        containSame(context.problems, expectedProblems)
    }

    void 'validate, when identifier is not in DB but parseable and individual belongs to different project, adds error'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_ID}\t${PROJECT}\nP:X I:Y S:Z")
        DomainFactory.createProject(name: 'X')
        Individual individual = DomainFactory.createIndividual(pid: 'Y')

        when:
        validator.validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        problem.message == "Sample identifier 'P:X I:Y S:Z' is not registered in OTP. It looks like it belongs to project 'X' and individual 'Y', but individual 'Y' is already registered in OTP with project '${individual.project.name}'.".toString()
    }

    void 'validate, when identifier is not in DB but parseable and project is in DB, succeeds'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_ID}\t${PROJECT}\nP:X I:Y S:Z")
        DomainFactory.createProject(name: 'X')

        when:
        validator.validate(context)

        then:
        context.problems.isEmpty()
    }

    void 'validate, when identifier is in DB and parseable and project is inconsistent, adds warning'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_ID}\t${PROJECT}\nP:X I:Y S:Z")
        createSampleIdentifier(context.spreadsheet.dataRows.get(0).cells.get(0).text, 'A', 'Y', 'Z')

        when:
        validator.validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.WARNING
        problem.message == "Sample identifier 'P:X I:Y S:Z' looks like it belongs to project 'X', but it is already registered in OTP with project 'A'. If you ignore this warning, OTP will keep the assignment of the sample identifier to project 'A'."
    }

    void 'validate, when identifier is in DB and parseable and individual is inconsistent, adds warning'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_ID}\t${PROJECT}\nP:X I:Y S:Z")
        createSampleIdentifier(context.spreadsheet.dataRows.get(0).cells.get(0).text, 'X', 'B', 'Z')

        when:
        validator.validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.WARNING
        problem.message == "Sample identifier 'P:X I:Y S:Z' looks like it belongs to individual 'Y', but it is already registered in OTP with individual 'B'. If you ignore this warning, OTP will keep the assignment of the sample identifier to individual 'B'."
    }

    void 'validate, when identifier is in DB and parseable and sample type is inconsistent, adds warning'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_ID}\t${PROJECT}\nP:X I:Y S:Z")
        createSampleIdentifier(context.spreadsheet.dataRows.get(0).cells.get(0).text, 'X', 'Y', 'C')

        when:
        validator.validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.WARNING
        problem.message == "Sample identifier 'P:X I:Y S:Z' looks like it belongs to sample type 'Z', but it is already registered in OTP with sample type 'C' If you ignore this warning, OTP will keep the assignment of the sample identifier to sample type 'C'."
    }

    void 'validate, when identifiers belong to different projects, adds warning'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_ID}\n" +
                        "SampleA\n" +
                        "SampleB\n" +
                        "SampleC\n" +
                        "P:B I:M S:N\n" +
                        "P:C I:M S:N\n")
        createSampleIdentifier('SampleB', 'B', 'W', 'X')
        createSampleIdentifier('SampleC', 'C', 'Y', 'Z')
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[1].cells +
                        context.spreadsheet.dataRows[2].cells +
                        context.spreadsheet.dataRows[3].cells +
                        context.spreadsheet.dataRows[4].cells as Set, Level.WARNING,
                        "The sample identifiers belong to different projects:\nProject 'B': 'P:B I:M S:N', 'SampleB'\nProject 'C': 'P:C I:M S:N', 'SampleC'"),
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.ERROR,
                        "Sample identifier 'SampleA' is neither registered in OTP nor matches a pattern known to OTP."),
                new Problem(Collections.emptySet(), Level.INFO,
                        "All sample identifiers which are neither registered in OTP nor match a pattern known to OTP:\nSampleA"),
        ]

        when:
        validator.validate(context)

        then:
        containSame(context.problems, expectedProblems)
    }

    void 'validate, when all known identifiers belong to the same project, adds no warning about different projects'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_ID}\n" +
                        "SampleA\n" +
                        "SampleB\n" +
                        "P:B I:M S:N\n")
        createSampleIdentifier('SampleB', 'B', 'W', 'X')
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.ERROR,
                        "Sample identifier 'SampleA' is neither registered in OTP nor matches a pattern known to OTP."),
                new Problem(Collections.emptySet(), Level.INFO,
                        "All sample identifiers which are neither registered in OTP nor match a pattern known to OTP:\nSampleA"),
        ]

        when:
        validator.validate(context)

        then:
        containSame(context.problems, expectedProblems)
    }

    void 'validate, when PROJECT column is missing and all identifiers are known and belong to the same project, adds info'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_ID}\n" +
                        "SampleB\n" +
                        "P:B I:M S:N\n")
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
                        "P:B I:M S:N\n")
        createSampleIdentifier('SampleB', 'B', 'W', 'X')

        when:
        validator.validate(context)

        then:
        context.problems.isEmpty()
    }

    private static SampleIdentifier createSampleIdentifier(String sampleIdentifierName, String projectName, String pid, String sampleTypeName) {
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
                parseSampleIdentifier: { String sampleIdentifier ->
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
