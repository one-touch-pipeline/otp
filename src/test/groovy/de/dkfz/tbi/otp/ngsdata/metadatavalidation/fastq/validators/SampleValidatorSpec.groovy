/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.validators

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.parser.DefaultParsedSampleIdentifier
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import java.util.regex.Matcher
import java.util.regex.Pattern

import static de.dkfz.tbi.TestCase.assertContainSame
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.PROJECT
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.SAMPLE_NAME
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class SampleValidatorSpec extends Specification implements DataTest, DomainFactoryCore {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                Individual,
                ProcessingOption,
                Project,
                Realm,
                Sample,
                SampleIdentifier,
                SampleType,
        ]
    }

    static final Pattern PATTERN = Pattern.compile(/^P-([^ ]+)_I-([^ ]+)_S-([^ ]+)$/)
    static final String SAMPLE_Z = "P-X_I-Y_S-z"
    static final String SAMPLE_N = "P-B_I-M_S-N"
    static final String PROJECT_C = "C"
    static final String PROJECT_B = "B"
    static final String PROJECT_X = "X"

    static final String PARSED_SAMPLETYPE_PID = "The following Samples will be created:\n${SampleIdentifierService.BulkSampleCreationHeader.headers}\n"

    SampleValidator validator = withSampleIdentifierService(new SampleValidator())

    void 'validate, when identifier is not parseable but in DB, succeeds'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_NAME}\t${PROJECT}\nABC")
        DomainFactory.createSampleIdentifier(name: 'ABC')

        when:
        validator.validate(context)

        then:
        context.problems.isEmpty()
    }

    void 'validate, when sample names are neither parseable nor in DB, adds errors and info'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_NAME}\nABC\nAAA")
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, LogLevel.ERROR,
                        "Sample name 'ABC' is not registered in OTP and for the project no parser is defined.",
                        SampleValidator.ERROR_NEITHER_REGISTERED_NOR_PARSEABLE),
                new Problem(context.spreadsheet.dataRows[1].cells as Set, LogLevel.ERROR,
                        "Sample name 'AAA' is not registered in OTP and for the project no parser is defined.",
                        SampleValidator.ERROR_NEITHER_REGISTERED_NOR_PARSEABLE),
                new Problem(Collections.emptySet(), LogLevel.INFO,
                        "All sample names which are neither registered in OTP nor match a pattern known to OTP:\n${SampleIdentifierService.BulkSampleCreationHeader.headers}\n\t\t\tAAA\n\t\t\tABC",
                        "All sample names which are neither registered in OTP nor match a pattern known to OTP:\n${SampleIdentifierService.BulkSampleCreationHeader.summaryHeaders}\n\t\t\tAAA\t\t\n\t\t\tABC\t\t"),
        ]
        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when identifier is not in DB but parseable and project is not in DB, adds error'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_NAME}\t${PROJECT}\n${SAMPLE_Z}\t${PROJECT_X}")

        Collection<Problem> expectedProblems = [
                new Problem([context.spreadsheet.dataRows[0].cells[0], context.spreadsheet.dataRows[0].cells[1]] as Set, LogLevel.ERROR,
                        "The parsed project '${PROJECT_X}' of the sample name '${SAMPLE_Z}' could not be found in the database.",
                        SampleValidator.ERROR_PARSED_PROJECT_UNKNOWN),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when identifier is not in DB but parseable and project is not in DB but individual is, adds errors'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_NAME}\t${PROJECT}\n${SAMPLE_Z}\t${PROJECT_X}")
        Individual individual = DomainFactory.createIndividual(pid: 'Y')
        Collection<Problem> expectedProblems = [
                new Problem([context.spreadsheet.dataRows[0].cells[0], context.spreadsheet.dataRows[0].cells[1]] as Set, LogLevel.ERROR,
                        "The parsed pid 'Y' of the sample name '${SAMPLE_Z}' already exist, but belongs to the project '${individual.project.name}' and not the parsed project 'X'.",
                        SampleValidator.ERROR_PARSED_INDIVIDUAL_KNOWN_IN_OTHER_PROJECT),
                new Problem([context.spreadsheet.dataRows[0].cells[0], context.spreadsheet.dataRows[0].cells[1]] as Set, LogLevel.ERROR,
                        "The parsed project '${PROJECT_X}' of the sample name '${SAMPLE_Z}' could not be found in the database.",
                        SampleValidator.ERROR_PARSED_PROJECT_UNKNOWN),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    @Unroll
    void 'validate, when identifier is not in DB but parseable and individual belongs to different project, adds error (case: #name)'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_NAME}\t${PROJECT}\n${SAMPLE_Z}\t${PROJECT_X}")
        createProject([name: projectName, nameInMetadataFiles: projectAlias])
        Individual individual = DomainFactory.createIndividual(pid: 'Y')
        Collection<Problem> expectedProblems = [
                new Problem([context.spreadsheet.dataRows[0].cells[0], context.spreadsheet.dataRows[0].cells[1]] as Set, LogLevel.ERROR,
                        "The parsed pid 'Y' of the sample name '${SAMPLE_Z}' already exist, but belongs to the project '${individual.project.name}' and not the parsed project 'X'.",
                        SampleValidator.ERROR_PARSED_INDIVIDUAL_KNOWN_IN_OTHER_PROJECT),
        ]
        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)

        where:
        name              | projectName | projectAlias
        'matchAboutName'  | 'X'         | null
        'matchAboutAlias' | 'SOME'      | 'X'
    }

    @Unroll
    void 'validate, when identifier is not in DB but parseable and project is in DB, succeeds (case: #name)'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_NAME}\t${PROJECT}\n${SAMPLE_Z}\t${PROJECT_X}")
        createProject([name: projectName, nameInMetadataFiles: projectAlias])

        when:
        validator.validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == LogLevel.INFO
        problem.message == "${PARSED_SAMPLETYPE_PID}X\tY\tz\t${SAMPLE_Z}"

        where:
        name              | projectName | projectAlias
        'matchAboutName'  | 'X'         | null
        'matchAboutAlias' | 'SOME'      | 'X'
    }

    @Unroll
    void 'validate, when identifier is in DB and parseable and project is inconsistent, adds warning (case: #name)'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_NAME}\t${PROJECT}\n${SAMPLE_Z}")
        createSampleIdentifier2(context.spreadsheet.dataRows.get(0).cells.get(0).text, projectName, 'Y', 'z', projectAlias)

        when:
        validator.validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == LogLevel.WARNING
        problem.message == "Sample name '${SAMPLE_Z}' looks like it belongs to project 'X', but it is already registered in OTP with project '${projectName}'. If you ignore this warning, OTP will keep the assignment of the sample name to project '${projectName}'."
        problem.type == SampleValidator.ERROR_PARSED_AND_FOUND_PROJECT_DIFFER

        where:
        name              | projectName | projectAlias
        'matchAboutName'  | 'A'         | null
        'matchAboutAlias' | 'SOME'      | 'A'
    }

    @Unroll
    void 'validate, when identifier is in DB and parseable and individual is inconsistent, adds warning (case: #name)'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_NAME}\t${PROJECT}\n${SAMPLE_Z}\t${PROJECT_X}")
        createSampleIdentifier2(context.spreadsheet.dataRows.get(0).cells.get(0).text, projectName, 'B', 'z', projectAlias)

        when:
        validator.validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == LogLevel.WARNING
        problem.message == "Sample name '${SAMPLE_Z}' looks like it belongs to individual 'Y', but it is already registered in OTP with individual 'B'. If you ignore this warning, OTP will keep the assignment of the sample name to individual 'B'."
        problem.type == SampleValidator.ERROR_PARSED_AND_FOUND_INDIVIDUAL_DIFFER

        where:
        name              | projectName | projectAlias
        'matchAboutName'  | 'X'         | null
        'matchAboutAlias' | 'SOME'      | 'X'
    }

    @Unroll
    void 'validate, when identifier is in DB and parseable and sample type is inconsistent, adds warning (case: #name)'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_NAME}\t${PROJECT}\n${SAMPLE_Z}")
        createSampleIdentifier2(context.spreadsheet.dataRows.get(0).cells.get(0).text, projectName, 'Y', 'c', projectAlias)

        when:
        validator.validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == LogLevel.WARNING
        problem.message == "Sample name '${SAMPLE_Z}' looks like it belongs to sample type 'z', but it is already registered in OTP with sample type 'c'. If you ignore this warning, OTP will keep the assignment of the sample name to sample type 'c'."
        problem.type == SampleValidator.ERROR_PARSED_AND_FOUND_SAMPLE_TYPE_DIFFER

        where:
        name              | projectName | projectAlias
        'matchAboutName'  | 'X'         | null
        'matchAboutAlias' | 'SOME'      | 'X'
    }

    void 'validate, when identifiers belong to different projects, adds warning'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_NAME}\t${PROJECT}\n" +
                        "SampleA\n" +
                        "SampleB\n" +
                        "SampleC1\n" +
                        "SampleC2\n" +
                        "SampleC3\n" +
                        "${SAMPLE_N}\t${PROJECT_B}\n" +
                        "P-C_I-M_S-N\t${PROJECT_C}\n")
        createSampleIdentifier2('SampleB', 'B', 'W', 'x')
        SampleIdentifier c1 = createSampleIdentifier2('SampleC1', 'C', 'Y', 'z')
        DomainFactory.createSampleIdentifier(name: 'SampleC2', sample: c1.sample)
        DomainFactory.createSampleIdentifier(name: 'SampleC3', sample: c1.sample)
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[1..6]*.cells.flatten() as Set<Cell>, LogLevel.WARNING, """\
The sample names belong to different projects:
Project 'B':
        '${SAMPLE_N}'
        'SampleB'
Project 'C':
        'P-C_I-M_S-N'
        'SampleC1'
        'SampleC2'
        'SampleC3'\
"""),
                new Problem(context.spreadsheet.dataRows[0].cells as Set, LogLevel.ERROR,
                        "Sample name 'SampleA' is not registered in OTP and for the project no parser is defined.",
                        SampleValidator.ERROR_NEITHER_REGISTERED_NOR_PARSEABLE),
                new Problem(Collections.emptySet(), LogLevel.INFO,
                        "All sample names which are neither registered in OTP nor match a pattern known to OTP:\n" +
                                "${SampleIdentifierService.BulkSampleCreationHeader.headers}\n\t\t\tSampleA",
                        "All sample names which are neither registered in OTP nor match a pattern known to OTP:\n" +
                                "${SampleIdentifierService.BulkSampleCreationHeader.summaryHeaders}\n\t\t\tSampleA\t\t"),
                new Problem(Collections.emptySet(), LogLevel.INFO, "${PARSED_SAMPLETYPE_PID}B\tM\tN\tP-B_I-M_S-N\nC\tM\tN\tP-C_I-M_S-N"),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when all known identifiers belong to the same project, adds no warning about different projects'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_NAME}\t${PROJECT}\n" +
                        "SampleA\n" +
                        "SampleB\n" +
                        "${SAMPLE_N}\t${PROJECT_B}\n")
        createSampleIdentifier2('SampleB', 'B', 'W', 'x')
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, LogLevel.ERROR,
                        "Sample name 'SampleA' is not registered in OTP and for the project no parser is defined.",
                        SampleValidator.ERROR_NEITHER_REGISTERED_NOR_PARSEABLE),
                new Problem(Collections.emptySet(), LogLevel.INFO,
                        "All sample names which are neither registered in OTP nor match a pattern known to OTP:\n${SampleIdentifierService.BulkSampleCreationHeader.headers}\n\t\t\tSampleA",
                        "All sample names which are neither registered in OTP nor match a pattern known to OTP:\n${SampleIdentifierService.BulkSampleCreationHeader.summaryHeaders}\n\t\t\tSampleA\t\t"),
                new Problem(Collections.emptySet(), LogLevel.INFO, "${PARSED_SAMPLETYPE_PID}B\tM\tN\tP-B_I-M_S-N"),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when PROJECT column is missing and all identifiers are known and belong to the same project, adds info'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_NAME}\n" +
                        "SampleB\n")
        createSampleIdentifier2('SampleB', 'B', 'W', 'x')
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, LogLevel.INFO,
                        "All sample names belong to project 'B'.")
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when PROJECT column is present and all identifiers are known and belong to the same project, adds no problem'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_NAME}\t${PROJECT}\n" +
                        "SampleB\t${PROJECT_B}\n" +
                        "${SAMPLE_N}\t${PROJECT_B}\n")
        createSampleIdentifier2('SampleB', 'B', 'W', 'x')

        when:
        validator.validate(context)

        then:

        Problem problem = exactlyOneElement(context.problems)
        problem.level == LogLevel.INFO
        problem.message == "${PARSED_SAMPLETYPE_PID}B\tM\tN\tP-B_I-M_S-N"
    }

    private static SampleIdentifier createSampleIdentifier2(String sampleIdentifierName, String projectName, String pid, String sampleTypeName,
                                                            String projectAlias = null) {
        return DomainFactory.createSampleIdentifier(
                sample: DomainFactory.createSample(
                        individual: DomainFactory.createIndividual(
                                project: DomainFactory.createProject(
                                        name: projectName,
                                        nameInMetadataFiles: projectAlias,
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
                        return new DefaultParsedSampleIdentifier(matcher.group(1), matcher.group(2), matcher.group(3), sampleIdentifier, null)
                    }
                    return null
                }
        ] as SampleIdentifierService
        return validator
    }
}
