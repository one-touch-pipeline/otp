package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import grails.buildtestdata.mixin.Build
import org.junit.*
import org.junit.rules.*
import spock.lang.*

@Build([
        AlignmentPass,
        MergingCriteria,
        MergingSetAssignment,
        ProcessedMergedBamFile,
        QualityAssessmentMergedPass,
        Realm,
])
class ProcessedMergedBamFileQaFileServiceSpecification extends Specification {

    ProcessedMergedBamFileQaFileService service = new ProcessedMergedBamFileQaFileService(
            processedMergedBamFileService: new ProcessedMergedBamFileService(
                    dataProcessingFilesService: new DataProcessingFilesService()
            )
    )

    @Rule
    TemporaryFolder temporaryFolder

    TestConfigService configService

    void setup() {
        configService = new TestConfigService(['otp.processing.root.path': temporaryFolder.newFolder().path])
    }

    void cleanup() {
        configService.clean()
    }

    void "test validateQADataFiles allFine"() {
        setup:
        QualityAssessmentMergedPass pass = DomainFactory.createQualityAssessmentMergedPass()
        CreateFileHelper.createFile(new File(service.coverageDataFilePath(pass)))
        CreateFileHelper.createFile(new File(service.qualityAssessmentDataFilePath(pass)))
        CreateFileHelper.createFile(new File(service.insertSizeDataFilePath(pass)))

        when:
        service.validateQADataFiles(pass)

        then:
        notThrown(RuntimeException)
    }

    @Unroll
    void "test validateQADataFiles fail"() {
        setup:
        QualityAssessmentMergedPass pass = DomainFactory.createQualityAssessmentMergedPass()
        if (coverage) {
            CreateFileHelper.createFile(new File(service.coverageDataFilePath(pass)))
        }
        if (quality) {
            CreateFileHelper.createFile(new File(service.qualityAssessmentDataFilePath(pass)))
        }
        if (inserSize) {
            CreateFileHelper.createFile(new File(service.insertSizeDataFilePath(pass)))
        }

        when:
        service.validateQADataFiles(pass)

        then:
        RuntimeException exception = thrown()
        coverage != exception.message.contains('The coverage file is not valid')
        quality != exception.message.contains('The qualityAssessment file is not valid')
        inserSize != exception.message.contains('The insertSizeData file is not valid')

        where:
        coverage | quality | inserSize || messages
        false    | true    | true      || ['The coverage file is not valid']
        true     | false   | true      || ['The qualityAssessment file is not valid']
        true     | true    | false     || ['The insertSizeData file is not valid']
        false    | false   | true      || ['The coverage file is not valid', 'The qualityAssessment file is not valid']
        false    | true    | false     || ['The coverage file is not valid', 'The insertSizeData file is not valid']
        true     | false   | false     || ['The qualityAssessment file is not valid', 'The insertSizeData file is not valid']
        false    | false   | false     || ['The coverage file is not valid', 'The qualityAssessment file is not valid', 'The insertSizeData file is not valid']
    }


}
