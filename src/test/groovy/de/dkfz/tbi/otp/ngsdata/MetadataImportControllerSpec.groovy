/*
 * Copyright 2011-2021 The OTP authors
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

import grails.plugin.springsecurity.acl.AclSid
import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import org.springframework.http.HttpStatus
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.utils.error.ForbiddenErrorPlainResponseException
import de.dkfz.tbi.otp.utils.error.InternalServerErrorPlainResponseException
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.directorystructures.DirectoryStructureBeanName
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.Problems

import java.nio.file.Paths

class MetadataImportControllerSpec extends Specification implements ControllerUnitTest<MetadataImportController>, UserAndRoles, DataTest, DomainFactoryCore {

    static final String VALID_SECRET = "secret"

    @Override
    Class[] getDomainClassesToMock() {
        return [
                AclSid,
                Role,
                User,
                UserRole,
                MetaDataFile,
        ]
    }

    void setupData() {
        createUserAndRoles()
        controller.configService = new TestConfigService([(OtpProperty.CONFIG_AUTO_IMPORT_SECRET): VALID_SECRET])
    }

    @Unroll
    void "autoImport, should fail with ForbiddenErrorPlainResponseException caught by PlainResponseExceptionHandler if secret is #secret"() {
        given:
        setupData()

        when:
        controller.autoImport(secret)

        then:
        notThrown(ForbiddenErrorPlainResponseException)
        response.text.startsWith(ForbiddenErrorPlainResponseException.name)
        !(response.text ==~ /^(?!.*<[^>]+>).*/)
        response.status == HttpStatus.FORBIDDEN.value()

        where:
        secret << ["wrongSecret", null]
    }

    void "autoImport, should fail with ForbiddenErrorPlainResponseException caught by PlainResponseExceptionHandler if no secret is defined"() {
        given:
        setupData()
        controller.configService = new TestConfigService()

        when:
        controller.autoImport(VALID_SECRET)

        then:
        notThrown(ForbiddenErrorPlainResponseException)
        response.text.startsWith(ForbiddenErrorPlainResponseException.name)
        !(response.text ==~ /^(?!.*<[^>]+>).*/)
        response.status == HttpStatus.FORBIDDEN.value()
    }

    void "autoImport, should fail with InternalServerErrorPlainResponseException caught by PlainResponseExceptionHandler when doAutoImport fail with uncaught exception"() {
        given:
        setupData()
        controller.processingOptionService = Mock(ProcessingOptionService) {
            1 * findOptionAsBoolean(_) >> false
        }

        when:
        controller.autoImport(VALID_SECRET)

        then:
        notThrown(InternalServerErrorPlainResponseException)
        response.text.startsWith(InternalServerErrorPlainResponseException.name)
        !(response.text ==~ /^(?!.*<[^>]+>).*/)
        response.status == HttpStatus.INTERNAL_SERVER_ERROR.value()
    }

    void "autoImport, should succeeded with 200 and text of each MetadataFile and created link"() {
        given:
        setupData()
        final String ticketNumber = "426964"
        final String ilseNumbers = "1111+2222+3333"
        final List<ValidateAndImportResult> resultList = []

        for (int i : 1..3) {
            MetaDataFile metadataFile = DomainFactory.createMetaDataFile()
            resultList.add(new ValidateAndImportResult(
                    MetadataValidationContextFactory.createContext([metadataFile: Paths.get("${metadataFile.filePath}/${metadataFile.fileName}")]),
                    metadataFile,
                    "new_path_${i}",
            ))
        }

        String expected = """\
                          Automatic import succeeded :-)

                          ${resultList[0].context.metadataFile} --> ${resultList[0].copiedFile}
                          http://example.com/metadataImport/details/${resultList[0].metadataFile.fastqImportInstance.id}

                          ${resultList[1].context.metadataFile} --> ${resultList[1].copiedFile}
                          http://example.com/metadataImport/details/${resultList[1].metadataFile.fastqImportInstance.id}

                          ${resultList[2].context.metadataFile} --> ${resultList[2].copiedFile}
                          http://example.com/metadataImport/details/${resultList[2].metadataFile.fastqImportInstance.id}
                          """.stripIndent().trim()

        controller.processingOptionService = Mock(ProcessingOptionService) {
            1 * findOptionAsBoolean(_) >> true
        }
        controller.metadataImportService = Mock(MetadataImportService) {
            1 * validateAndImportMultiple(ticketNumber, ilseNumbers, true) >> resultList
            0 * validateAndImportMultiple(ticketNumber, ilseNumbers, false)
        }

        controller.params.ticketNumber = ticketNumber
        controller.params.ilseNumbers = ilseNumbers
        controller.params.ignoreMd5sumError = "TRUE"

        when:
        controller.autoImport(VALID_SECRET)

        then:
        response.status == HttpStatus.OK.value()
        response.text == expected
        notThrown(Throwable)
    }

    void "autoImport, should succeeded with 200 and text of each MetadataFile, a Link for manual creation and problems"() {
        given:
        setupData()
        final String ticketNumber = "426964"
        final String ilseNumbers = "1111+2222+3333"
        final List<MetadataValidationContext> contexts = []
        final List<MetaDataFile> metaDataFiles = []

        Problems problems = new Problems()
        problems.addProblem([] as Set, LogLevel.ERROR, "An Error occurred")
        for (int i : 1..3) {
            MetaDataFile metadataFile = DomainFactory.createMetaDataFile()
            metaDataFiles.add(metadataFile)
            contexts.add(MetadataValidationContextFactory.createContext([
                    metadataFile: Paths.get("${metadataFile.filePath}/${metadataFile.fileName}"),
                    problems    : problems,
            ]))
        }

        final String link = "http://example.com/metadataImport/index?ticketNumber=$ticketNumber" +
                "&paths=${MetaDataFile.name}+%3A+${metaDataFiles[0].id}" +
                "&paths=${MetaDataFile.name}+%3A+${metaDataFiles[1].id}" +
                "&paths=${MetaDataFile.name}+%3A+${metaDataFiles[2].id}" +
                "&directoryStructure=$DirectoryStructureBeanName.GPCF_SPECIFIC"

        final String expected = """\
                                These metadata files failed validation:
                                ${contexts[0].metadataFile}
                                ${contexts[1].metadataFile}
                                ${contexts[2].metadataFile}

                                Click here for manual import:
                                $link

                                The following validation summary messages were returned for ${contexts[0].metadataFile.fileName}:
                                ${contexts[0].problemsObject.sortedProblemListString}

                                The following validation summary messages were returned for ${contexts[1].metadataFile.fileName}:
                                ${contexts[1].problemsObject.sortedProblemListString}

                                The following validation summary messages were returned for ${contexts[2].metadataFile.fileName}:
                                ${contexts[2].problemsObject.sortedProblemListString}

                                """.stripIndent()

        controller.processingOptionService = Mock(ProcessingOptionService) {
            1 * findOptionAsBoolean(_) >> true
        }
        controller.metadataImportService = Mock(MetadataImportService) {
            1 * validateAndImportMultiple(ticketNumber, ilseNumbers, true) >> { throw new MultiImportFailedException(contexts, metaDataFiles) }
            0 * validateAndImportMultiple(ticketNumber, ilseNumbers, false)
        }

        controller.params.ticketNumber = ticketNumber
        controller.params.ilseNumbers = ilseNumbers
        controller.params.ignoreMd5sumError = "TRUE"

        when:
        controller.autoImport(VALID_SECRET)

        then:
        response.status == HttpStatus.OK.value()
        response.text == expected
        notThrown(Throwable)
    }

    void "autoImport, if first import contains only some lines and second all, then both should succeeded with 200 and text of each MetadataFile, a Link for manual creation and problems"() {
        given:
        setupData()
        final String ticketNumber = "426964"
        final String ilseNumbers = "1111+2222+3333"
        final List<ValidateAndImportResult> resultList = []

        for (int i : 1..3) {
            MetaDataFile metadataFile = DomainFactory.createMetaDataFile()
            resultList.add(new ValidateAndImportResult(
                    MetadataValidationContextFactory.createContext([metadataFile: Paths.get("${metadataFile.filePath}/${metadataFile.fileName}")]),
                    metadataFile,
                    "new_path_${i}",
            ))
        }

        String expected = """\
                          Automatic import succeeded :-)

                          ${resultList[0].context.metadataFile} --> ${resultList[0].copiedFile}
                          http://example.com/metadataImport/details/${resultList[0].metadataFile.fastqImportInstance.id}

                          ${resultList[1].context.metadataFile} --> ${resultList[1].copiedFile}
                          http://example.com/metadataImport/details/${resultList[1].metadataFile.fastqImportInstance.id}

                          ${resultList[2].context.metadataFile} --> ${resultList[2].copiedFile}
                          http://example.com/metadataImport/details/${resultList[2].metadataFile.fastqImportInstance.id}
                          """.stripIndent().trim()

        controller.processingOptionService = Mock(ProcessingOptionService) {
            2 * findOptionAsBoolean(_) >> true
        }
        controller.metadataImportService = Mock(MetadataImportService) {
            2 * validateAndImportMultiple(ticketNumber, ilseNumbers, true) >> resultList
            0 * validateAndImportMultiple(ticketNumber, ilseNumbers, false)
        }

        controller.params.ticketNumber = ticketNumber
        controller.params.ilseNumbers = ilseNumbers
        controller.params.ignoreMd5sumError = "TRUE"

        when:
        controller.autoImport(VALID_SECRET)

        then:
        response.status == HttpStatus.OK.value()
        response.text == expected
        notThrown(Throwable)

        when:
        response.reset()
        controller.autoImport(VALID_SECRET)

        then:
        response.status == HttpStatus.OK.value()
        response.text == expected
        notThrown(Throwable)
    }
}
