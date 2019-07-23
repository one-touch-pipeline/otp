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

package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators


import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.BamMetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem

class Md5sumUniqueValidatorSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                DataFile,
                ExternallyProcessedMergedBamFile,
                ExternalMergingWorkPackage,
                FileType,
                Individual,
                Pipeline,
                Project,
                Realm,
                ReferenceGenome,
                Run,
                RunSegment,
                Sample,
                SampleType,
                SeqCenter,
                SeqPlatform,
                SeqPlatformGroup,
                SeqPlatformModelLabel,
                SeqTrack,
                SeqType,
                SoftwareTool,
        ]
    }

    void 'validate concerning metadata, when column is missing, adds error'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
SomeColumn
SomeValue
""")

        when:
        new Md5sumUniqueValidator().validate(context)

        then:
        Problem problem = CollectionUtils.exactlyOneElement(context.problems)
        problem.level == Level.ERROR
        TestCase.assertContainSame(problem.affectedCells*.cellAddress, [])
        problem.message.contains("Required column 'MD5' is missing.")
    }

    void 'validate concerning bam metadata, when column is missing, adds warning'() {
        given:
        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext("""\
SomeColumn
SomeValue
""")

        when:
        new Md5sumUniqueValidator().validate(context)

        then:
        Problem problem = CollectionUtils.exactlyOneElement(context.problems)
        problem.level == Level.WARNING
        TestCase.assertContainSame(problem.affectedCells*.cellAddress, [])
        problem.message.contains("Optional column 'MD5' is missing.")
    }

    void 'validate, all are fine'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${MetaDataColumn.MD5}
${HelperUtils.getRandomMd5sum()}
${HelperUtils.getRandomMd5sum()}
${HelperUtils.getRandomMd5sum()}
${HelperUtils.getRandomMd5sum()}
${HelperUtils.getRandomMd5sum()}
""")

        when:
        new Md5sumUniqueValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate concerning metadata, adds expected errors'() {
        given:
        String md5sum1 = HelperUtils.getRandomMd5sum()
        String md5sum2 = HelperUtils.getRandomMd5sum()
        String md5sum3 = HelperUtils.getRandomMd5sum()
        String md5sum4 = HelperUtils.getRandomMd5sum()

        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${MetaDataColumn.MD5}
${md5sum1}
${md5sum2}
${md5sum3}
${md5sum2}
""")
        DomainFactory.createDataFile(md5sum: md5sum3)
        DomainFactory.createDataFile(md5sum: md5sum4)

        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[1].cells + context.spreadsheet.dataRows[3].cells as Set, Level.WARNING,
                        "The MD5 sum '${md5sum2}' is not unique in the metadata file.", "At least one MD5 sum is not unique in the metadata file."),
                new Problem(context.spreadsheet.dataRows[2].cells as Set, Level.WARNING,
                        "A fastq file with the MD5 sum '${md5sum3}' is already registered in OTP.", "At least one fastq file has a MD5 sum which is already registered in OTP."),
        ]

        when:
        new Md5sumUniqueValidator().validate(context)

        then:
        TestCase.assertContainSame(context.problems, expectedProblems)
    }

    void 'validate concerning bam metadata, adds expected errors'() {
        given:
        String md5sum1 = HelperUtils.getRandomMd5sum()
        String md5sum2 = HelperUtils.getRandomMd5sum()
        String md5sum3 = HelperUtils.getRandomMd5sum()
        String md5sum4 = HelperUtils.getRandomMd5sum()

        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext("""\
${BamMetadataColumn.MD5}
${md5sum1}
${md5sum2}
${md5sum3}
${md5sum2}
""")
        DomainFactory.createExternallyProcessedMergedBamFile(
                md5sum: md5sum3,
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.PROCESSED,
                fileSize: 1
        )
        DomainFactory.createExternallyProcessedMergedBamFile(
                md5sum: md5sum4,
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.PROCESSED,
                fileSize: 1
        )
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[1].cells + context.spreadsheet.dataRows[3].cells as Set, Level.WARNING,
                        "The MD5 sum '${md5sum2}' is not unique in the metadata file.", "At least one MD5 sum is not unique in the metadata file."),
                new Problem(context.spreadsheet.dataRows[2].cells as Set, Level.WARNING,
                        "A bam file with the MD5 sum '${md5sum3}' is already registered in OTP.", "At least one bam file has a MD5 sum is already registered in OTP."),
        ]

        when:
        new Md5sumUniqueValidator().validate(context)

        then:
        TestCase.assertContainSame(context.problems, expectedProblems)
    }
}
