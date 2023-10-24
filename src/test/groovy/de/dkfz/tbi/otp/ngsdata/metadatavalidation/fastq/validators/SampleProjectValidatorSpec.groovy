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
import de.dkfz.tbi.otp.parser.SampleIdentifierParserBeanName
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.TestCase.assertContainSame
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.PROJECT
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.SAMPLE_NAME

class SampleProjectValidatorSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                Individual,
                ProcessingOption,
                Project,
                Sample,
                SampleIdentifier,
                SampleType,
        ]
    }

    void 'validate adds expected warnings'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext((
                "${PROJECT} ${SAMPLE_NAME}\n" +
                        "A not_parseable\n" +  // neither in DB nor can be parsed
                        "B project_B\n" +      // SampleIdentifier in DB, name matches
                        "C project_C\n" +      // SampleIdentifier in DB, nameInMetadataFiles matches
                        "D project_D\n" +      // SampleIdentifier in DB, neither name nor nameInMetadataFiles matches
                        "E project_E\n" +      // SampleIdentifier not in DB, name matches
                        "F project_V\n" +      // SampleIdentifier not in DB, nameInMetadataFiles matches
                        "G noProject_W\n"        // SampleIdentifier not in DB, neither name nor nameInMetadataFiles matches
        ).replace(' ', '\t'))
        createSampleIdentifier('project_B', 'B', 'X')
        createSampleIdentifier('project_C', 'Y', 'C')
        createSampleIdentifier('project_D', 'Z', 'Z')
        DomainFactory.createProject(name: 'D',)
        DomainFactory.createProject(name: 'V', nameInMetadataFiles: 'F', sampleIdentifierParserBeanName: SampleIdentifierParserBeanName.HIPO)
        DomainFactory.createProject(name: 'E', nameInMetadataFiles: 'E', sampleIdentifierParserBeanName: SampleIdentifierParserBeanName.HIPO)
        DomainFactory.createProject(name: 'G', nameInMetadataFiles: 'W', sampleIdentifierParserBeanName: SampleIdentifierParserBeanName.HIPO)
        SampleProjectValidator validator = new SampleProjectValidator()
        validator.sampleIdentifierService = [
                parseSampleIdentifier: { String sampleIdentifier, Project project ->
                    final String prefix = 'project_'
                    if (sampleIdentifier.startsWith(prefix)) {
                        return new DefaultParsedSampleIdentifier(sampleIdentifier.substring(prefix.length()), "don't care", "don't care", sampleIdentifier, null)
                    }
                    return null
                }
        ] as SampleIdentifierService
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[3].cells as Set<Cell>, LogLevel.WARNING,
                        "Sample name 'project_D' is already registered in OTP with project 'Z', not with project 'D'. If you ignore this warning, OTP will keep the assignment of the sample name to project 'Z' and ignore the value 'D' in the '${PROJECT}' column.", "At least one sample name is already registered in OTP but with another project."),
                new Problem(context.spreadsheet.dataRows[6].cells as Set<Cell>, LogLevel.WARNING,
                        "Sample name 'noProject_W' can not be parsed with the sampleIdentifierParser '${SampleIdentifierParserBeanName.HIPO}' given by project 'G'.", "At least one sample name looks like it does not belong to the project in the '${PROJECT}' column."),
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
