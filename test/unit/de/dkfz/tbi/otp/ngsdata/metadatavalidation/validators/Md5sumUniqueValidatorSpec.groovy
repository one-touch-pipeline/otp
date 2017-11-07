package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import grails.test.mixin.*
import spock.lang.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*

@Mock([
        DataFile,
        ExternallyProcessedMergedBamFile,
        ExternalMergingWorkPackage,
        FileType,
        Individual,
        Pipeline,
        Project,
        ProjectCategory,
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
])
class Md5sumUniqueValidatorSpec extends Specification {

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
        problem.message.contains("Mandatory column 'MD5' is missing.")
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
                        "The MD5 sum '${md5sum2}' is not unique in the metadata file."),
                new Problem(context.spreadsheet.dataRows[2].cells as Set, Level.WARNING,
                        "A fastq file with the MD5 sum '${md5sum3}' is already registered in OTP."),
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
                        "The MD5 sum '${md5sum2}' is not unique in the metadata file."),
                new Problem(context.spreadsheet.dataRows[2].cells as Set, Level.WARNING,
                        "A bam file with the MD5 sum '${md5sum3}' is already registered in OTP."),
        ]

        when:
        new Md5sumUniqueValidator().validate(context)

        then:
        TestCase.assertContainSame(context.problems, expectedProblems)
    }
}
