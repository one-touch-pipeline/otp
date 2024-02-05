/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.parser.DefaultParsedSampleIdentifier
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.Problem

@Rollback
@Integration
class MultipleImportValidatorIntegrationSpec extends Specification implements DomainFactoryCore {

    void setupRawSequenceFileHelper(Project project, SampleType sampleType, String md5sum) {
        createFastqFile(
                fastqMd5sum: md5sum,
                seqTrack: createSeqTrack([
                        sample: createSample([
                                sampleType: sampleType,
                                individual: createIndividual([
                                        project: project
                                ])
                        ])
                ])
        )
    }

    void 'validate, all is fine'() {
        given:
        SampleIdentifier sampleIdentifier = createSampleIdentifier()
        Project project = sampleIdentifier.project
        SampleType sampleType = sampleIdentifier.sampleType
        setupRawSequenceFileHelper(project, sampleType, HelperUtils.randomMd5sum)

        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
        ${MetaDataColumn.MD5}\t${MetaDataColumn.SAMPLE_NAME}\t${MetaDataColumn.PROJECT}
        ${HelperUtils.randomMd5sum}\t${sampleIdentifier.name}\t${project.name}
        ${HelperUtils.randomMd5sum}\t${sampleIdentifier.name}\t${project.name}\
        """)

        when:
        createMultipleImportValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when md5sum already exist for project and sampleType using sampleIdentifier, adds expected errors'() {
        given:
        String md5sum1 = HelperUtils.randomMd5sum
        String md5sum2 = HelperUtils.randomMd5sum

        SampleIdentifier sampleIdentifier = createSampleIdentifier()
        Project project = sampleIdentifier.project
        SampleType sampleType = sampleIdentifier.sampleType
        setupRawSequenceFileHelper(project, sampleType, md5sum1)

        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
        ${MetaDataColumn.MD5}\t${MetaDataColumn.SAMPLE_NAME}\t${MetaDataColumn.PROJECT}
        ${md5sum1}\t${sampleIdentifier.name}\t${project.name}
        ${md5sum2}\t${sampleIdentifier.name}\t${project.name}\
        """)

        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set<Cell>, LogLevel.ERROR,
                        "A file with the md5sum '${md5sum1}' and sample type '${sampleType.name}' already exists for project '${project.name}'.",
                        "At least one file with the md5sum and sample type combination exists in the corresponding project."),
        ]

        when:
        createMultipleImportValidator().validate(context)

        then:
        TestCase.assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when md5sum already exist for project and sampleType using parser, adds expected errors'() {
        given:
        String md5sum1 = HelperUtils.randomMd5sum
        String md5sum2 = HelperUtils.randomMd5sum

        SampleIdentifier sampleIdentifier = createSampleIdentifier()
        Project project = sampleIdentifier.project
        SampleType sampleType = sampleIdentifier.sampleType
        setupRawSequenceFileHelper(project, sampleType, md5sum1)

        String idToParse = "${project.name};someIndividual;${sampleType.name};anyId"

        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
        ${MetaDataColumn.MD5}\t${MetaDataColumn.SAMPLE_NAME}\t${MetaDataColumn.PROJECT}
        ${md5sum1}\t${idToParse}\t${project.name}
        ${md5sum2}\t${idToParse}\t${project.name}\
        """)

        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set<Cell>, LogLevel.ERROR,
                        "A file with the md5sum '${md5sum1}' and sample type '${sampleType.name}' already exists for project '${project.name}'.",
                        "At least one file with the md5sum and sample type combination exists in the corresponding project."),
        ]

        when:
        createMultipleImportValidator().validate(context)

        then:
        TestCase.assertContainSame(context.problems, expectedProblems)
    }

    private MultiImportValidator createMultipleImportValidator() {
        return new MultiImportValidator([
                sampleIdentifierService: Mock(SampleIdentifierService) {
                    _ * parseSampleIdentifier(_, _) >> { String sampleIdentifier, Project project ->
                        String[] split = sampleIdentifier.split(';')
                        if (split.length == 4) {
                            return new DefaultParsedSampleIdentifier(split[0], split[1], split[2], split[3], SampleType.SpecificReferenceGenome.UNKNOWN, null)
                        }
                        return null
                    }
                }
        ])
    }

}
