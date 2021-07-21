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
package de.dkfz.tbi.otp.ngsdata

import grails.testing.gorm.DataTest
import org.springframework.context.ApplicationContext
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidator
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel

import java.nio.file.Path
import java.nio.file.Paths

import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame

class BamMetadataImportServiceSpec extends Specification implements DomainFactoryCore, DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                ExternalMergingWorkPackage,
                ExternallyProcessedMergedBamFile,
                ExternalProcessedMergedBamFileQualityAssessment,
                QualityAssessmentMergedPass,
                ImportProcess,
                Individual,
                Project,
                Pipeline,
                Realm,
                ReferenceGenome,
                Sample,
                SampleType,
                SeqType,
                LibraryPreparationKit,
        ]
    }

    void "getImplementedValidations returns descriptions of validations"() {
        given:
        BamMetadataImportService service = new BamMetadataImportService()
        service.applicationContext = Mock(ApplicationContext) {
            getBeansOfType(BamMetadataValidator) >>
                    [
                            'validator1': [
                                    getDescriptions: { ['description1', 'description2'] }
                            ] as BamMetadataValidator,
                            'validator2': [
                                    getDescriptions: { ['description3'] }
                            ] as BamMetadataValidator,
                    ]
        }

        expect:
        containSame(service.implementedValidations, ['description1', 'description2', 'description3'])
    }

    void "validate creates context and calls validators"() {
        given:
        File testDirectory = TestCase.createEmptyTestDirectory()
        Path metadataFile = Paths.get(testDirectory.path, 'bamMetadata.tsv')
        metadataFile.bytes = 'Header\nI am metadata!'.getBytes(BamMetadataValidationContext.CHARSET)
        File qualityDirectory = new File(testDirectory, "quality")
        assert qualityDirectory.mkdirs()
        new File(qualityDirectory, "file.qc")
        List<String> furtherFiles = ["/quality"]

        BamMetadataImportService service = new BamMetadataImportService()
        service.applicationContext = Mock(ApplicationContext) {
            getBeansOfType(BamMetadataValidator) >>
                    [
                            'validator1': [
                                    validate: { BamMetadataValidationContext context ->
                                        context.addProblem(Collections.emptySet(), LogLevel.ERROR, 'message1')
                                        context.addProblem(Collections.emptySet(), LogLevel.ERROR, 'message2')
                                    }
                            ] as BamMetadataValidator,
                            'validator2': [
                                    validate: { BamMetadataValidationContext context ->
                                        context.addProblem(Collections.emptySet(), LogLevel.ERROR, 'message3')
                                    }
                            ] as BamMetadataValidator,
                    ]
        }
        service.fileSystemService = new TestFileSystemService()

        when:
        BamMetadataValidationContext context = service.validate(metadataFile.toString(), furtherFiles, false)

        then:
        containSame(context.problems*.message, ['message1', 'message2', 'message3'])
        context.metadataFile == metadataFile
        context.spreadsheet.header.cells[0].text == 'Header'
        context.spreadsheet.dataRows[0].cells[0].text == 'I am metadata!'

        cleanup:
        testDirectory.deleteDir()
    }
}
