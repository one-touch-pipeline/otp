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

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.parser.DefaultParsedSampleIdentifier
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import java.util.regex.Matcher
import java.util.regex.Pattern

import static de.dkfz.tbi.TestCase.assertContainSame
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.PROJECT
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.SAMPLE_NAME
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class SampleValidatorSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
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

    static final String PARSED_SAMPLETYPE_PID = "The following Samples will be created:\n${SampleIdentifierService.BulkSampleCreationHeader.getHeaders()}\n"

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
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.ERROR,
                        "Sample name 'ABC' is neither registered in OTP nor matches a pattern known to OTP.", "At least one sample name is neither registered in OTP nor matches a pattern known to OTP."),
                new Problem(context.spreadsheet.dataRows[1].cells as Set, Level.ERROR,
                        "Sample name 'AAA' is neither registered in OTP nor matches a pattern known to OTP.", "At least one sample name is neither registered in OTP nor matches a pattern known to OTP."),
                new Problem(Collections.emptySet(), Level.INFO,
                        "All sample names which are neither registered in OTP nor match a pattern known to OTP:\n${SampleIdentifierService.BulkSampleCreationHeader.getHeaders()}\n\t\t\tAAA\n\t\t\tABC"),
        ]
        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when identifier is not in DB but parseable and project is not in DB, adds error'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_NAME}\t${PROJECT}\n${SAMPLE_Z}")

        when:
        validator.validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        problem.message == "Sample name '${SAMPLE_Z}' is not registered in OTP. It looks like it belongs to project 'X', but no project with that name is registered in OTP."
    }

    void 'validate, when identifier is not in DB but parseable and project is not in DB but individual is, adds errors'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_NAME}\t${PROJECT}\n${SAMPLE_Z}")
        Individual individual = DomainFactory.createIndividual(pid: 'Y')
        Collection<Problem> expectedProblems = [
                new Problem([context.spreadsheet.dataRows[0].cells[0], context.spreadsheet.dataRows[0].cells[1]] as Set, Level.ERROR,
                        "Sample name '${SAMPLE_Z}' is not registered in OTP. It looks like it belongs to project 'X', but no project with that name is registered in OTP.", "At least one sample name is not registered in OTP. It looks like it belongs to a project not registered in OTP."),
                new Problem([context.spreadsheet.dataRows[0].cells[0], context.spreadsheet.dataRows[0].cells[1]] as Set, Level.ERROR,
                        "Sample name '${SAMPLE_Z}' is not registered in OTP. It looks like it belongs to project 'X' and individual 'Y', but individual 'Y' is already registered in OTP with project '${individual.project.name}'.", "At least one sample name is not registered in OTP. It looks like it belongs to a specific project and individual, but this individual is already registered in OTP with another project."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when identifier is not in DB but parseable and individual belongs to different project, adds error'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_NAME}\t${PROJECT}\n${SAMPLE_Z}")
        DomainFactory.createProject(name: 'X')
        Individual individual = DomainFactory.createIndividual(pid: 'Y')

        when:
        validator.validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        problem.message == "Sample name '${SAMPLE_Z}' is not registered in OTP. It looks like it belongs to project 'X' and individual 'Y', but individual 'Y' is already registered in OTP with project '${individual.project.name}'.".toString()
    }

    void 'validate, when identifier is not in DB but parseable and project is in DB, succeeds'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_NAME}\t${PROJECT}\n${SAMPLE_Z}")
        DomainFactory.createProject(name: 'X')

        when:
        validator.validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.INFO
        problem.message == "${PARSED_SAMPLETYPE_PID}X\tY\tz\tP-X_I-Y_S-z"
    }

    void 'validate, when identifier is in DB and parseable and project is inconsistent, adds warning'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_NAME}\t${PROJECT}\n${SAMPLE_Z}")
        createSampleIdentifier(context.spreadsheet.dataRows.get(0).cells.get(0).text, 'A', 'Y', 'z')

        when:
        validator.validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.WARNING
        problem.message == "Sample name '${SAMPLE_Z}' looks like it belongs to project 'X', but it is already registered in OTP with project 'A'. If you ignore this warning, OTP will keep the assignment of the sample name to project 'A'."
    }

    void 'validate, when identifier is in DB and parseable and individual is inconsistent, adds warning'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_NAME}\t${PROJECT}\n${SAMPLE_Z}")
        createSampleIdentifier(context.spreadsheet.dataRows.get(0).cells.get(0).text, 'X', 'B', 'z')

        when:
        validator.validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.WARNING
        problem.message == "Sample name '${SAMPLE_Z}' looks like it belongs to individual 'Y', but it is already registered in OTP with individual 'B'. If you ignore this warning, OTP will keep the assignment of the sample name to individual 'B'."
    }

    void 'validate, when identifier is in DB and parseable and sample type is inconsistent, adds warning'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_NAME}\t${PROJECT}\n${SAMPLE_Z}")
        createSampleIdentifier(context.spreadsheet.dataRows.get(0).cells.get(0).text, 'X', 'Y', 'c')

        when:
        validator.validate(context)

        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.WARNING
        problem.message == "Sample name '${SAMPLE_Z}' looks like it belongs to sample type 'z', but it is already registered in OTP with sample type 'c' If you ignore this warning, OTP will keep the assignment of the sample name to sample type 'c'."
    }

    void 'validate, when identifiers belong to different projects, adds warning'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_NAME}\n" +
                        "SampleA\n" +
                        "SampleB\n" +
                        "SampleC1\n" +
                        "SampleC2\n" +
                        "SampleC3\n" +
                        "${SAMPLE_N}\n" +
                        "P-C_I-M_S-N\n")
        createSampleIdentifier('SampleB', 'B', 'W', 'x')
        SampleIdentifier c1 = createSampleIdentifier('SampleC1', 'C', 'Y', 'z')
        DomainFactory.createSampleIdentifier(name: 'SampleC2', sample: c1.sample)
        DomainFactory.createSampleIdentifier(name: 'SampleC3', sample: c1.sample)
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[1..6]*.cells.flatten() as Set, Level.WARNING, """\
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
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.ERROR,
                        "Sample name 'SampleA' is neither registered in OTP nor matches a pattern known to OTP.",
                        "At least one sample name is neither registered in OTP nor matches a pattern known to OTP."),
                new Problem(Collections.emptySet(), Level.INFO,
                        "All sample names which are neither registered in OTP nor match a pattern known to OTP:\n" +
                                "${SampleIdentifierService.BulkSampleCreationHeader.getHeaders()}\n\t\t\tSampleA"),
                new Problem(Collections.emptySet(), Level.INFO, "${PARSED_SAMPLETYPE_PID}B\tM\tN\tP-B_I-M_S-N\nC\tM\tN\tP-C_I-M_S-N"),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when all known identifiers belong to the same project, adds no warning about different projects'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_NAME}\n" +
                        "SampleA\n" +
                        "SampleB\n" +
                        "${SAMPLE_N}\n")
        createSampleIdentifier('SampleB', 'B', 'W', 'x')
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.ERROR,
                        "Sample name 'SampleA' is neither registered in OTP nor matches a pattern known to OTP.", "At least one sample name is neither registered in OTP nor matches a pattern known to OTP."),
                new Problem(Collections.emptySet(), Level.INFO,
                        "All sample names which are neither registered in OTP nor match a pattern known to OTP:\n${SampleIdentifierService.BulkSampleCreationHeader.getHeaders()}\n\t\t\tSampleA"),
                new Problem(Collections.emptySet(), Level.INFO, "${PARSED_SAMPLETYPE_PID}B\tM\tN\tP-B_I-M_S-N"),
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
                        "SampleB\n" +
                        "${SAMPLE_N}\n")
        createSampleIdentifier('SampleB', 'B', 'W', 'x')
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells + context.spreadsheet.dataRows[1].cells as Set, Level.INFO,
                        "All sample names belong to project 'B'."),
                new Problem(Collections.emptySet(), Level.INFO, "${PARSED_SAMPLETYPE_PID}B\tM\tN\tP-B_I-M_S-N"),
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
                        "SampleB\n" +
                        "${SAMPLE_N}\n")
        createSampleIdentifier('SampleB', 'B', 'W', 'x')

        when:
        validator.validate(context)

        then:

        Problem problem = exactlyOneElement(context.problems)
        problem.level == Level.INFO
        problem.message == "${PARSED_SAMPLETYPE_PID}B\tM\tN\tP-B_I-M_S-N"
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
                parseSampleIdentifier: { String sampleIdentifier, Project project ->
                    Matcher matcher = PATTERN.matcher(sampleIdentifier)
                    if (matcher.matches()) {
                        return new DefaultParsedSampleIdentifier(matcher.group(1), matcher.group(2), matcher.group(3), sampleIdentifier, null)
                    } else {
                        return null
                    }
                }
        ] as SampleIdentifierService
        return validator
    }
}
