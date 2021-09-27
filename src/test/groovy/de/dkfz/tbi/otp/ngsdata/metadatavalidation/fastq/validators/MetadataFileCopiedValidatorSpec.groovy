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
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import java.nio.file.Files
import java.nio.file.Path

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

class MetadataFileCopiedValidatorSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                SeqCenter,
        ]
    }

    MetadataFileCopiedValidator metadataFileCopiedValidator

    static final String CONTEXT = "${CENTER_NAME}\t${ILSE_NO}\nCenter\t1234"

    @Rule
    TemporaryFolder temporaryFolder

    Path seqCenterMetadataFile

    void setup() {
        Path seqCenterDir = temporaryFolder.newFolder('seqCenterInbox').toPath()

        seqCenterMetadataFile = seqCenterDir.resolve('metadata_fastq.tsv')
        seqCenterMetadataFile.text = CONTEXT

        metadataFileCopiedValidator = new MetadataFileCopiedValidator()
        metadataFileCopiedValidator.metadataImportService = Spy(MetadataImportService) {
            getIlseFolder('1234', _) >> seqCenterDir
            getIlseFolder('invalid', _) >> null
        }
    }

    @Unroll
    void 'validate, when seqCenter contextString = #contextString and copyMetadataFile = #copyMetadataFile, succeeds'() {
        given:
        MetadataValidationContext context = createContextAndSeqCenter(contextString, copyMetadataFile)

        when:
        metadataFileCopiedValidator.validate(context)

        then:
        context.problems.empty

        where:
        contextString                                   | copyMetadataFile
        CONTEXT                                         | false
        CONTEXT                                         | true
        "${ILSE_NO}\t1234"                              | true
        "${PROJECT}\nProject"                           | true
        "${CENTER_NAME}\tCenter"                        | true
        "${CENTER_NAME}\t${ILSE_NO}\nCenter\tinvalid"   | true
    }

    //false positives, since rule can not recognize calling class
    @SuppressWarnings('ExplicitFlushForDeleteForUnitTestRule')
    void 'validate, when seqCenterInbox metadataFile is not there, succeeds'() {
        given:
        MetadataValidationContext context = createContextAndSeqCenter(CONTEXT)
        Files.delete(seqCenterMetadataFile)

        when:
        metadataFileCopiedValidator.validate(context)

        then:
        context.problems.empty
    }

    //false positives, since rule can not recognize calling class
    @SuppressWarnings('ExplicitFlushForDeleteForUnitTestRule')
    void 'validate, when seqCenterInbox metadataFile is a directorys, succeeds'() {
        given:
        MetadataValidationContext context = createContextAndSeqCenter(CONTEXT)
        Files.delete(seqCenterMetadataFile)
        Files.createDirectory(seqCenterMetadataFile)

        when:
        metadataFileCopiedValidator.validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when seqCenterInbox metadataFile is not readable, succeeds'() {
        given:
        MetadataValidationContext context = createContextAndSeqCenter(CONTEXT)
        Files.setPosixFilePermissions(seqCenterMetadataFile, [] as Set)

        when:
        metadataFileCopiedValidator.validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when seqCenterInbox metadataFile is empty, succeeds'() {
        given:
        MetadataValidationContext context = createContextAndSeqCenter(CONTEXT)
        seqCenterMetadataFile.bytes = []

        when:
        metadataFileCopiedValidator.validate(context)

        then:
        Problem problem = CollectionUtils.exactlyOneElement(context.problems)
        problem.level == LogLevel.ERROR
        TestCase.assertContainSame(problem.affectedCells*.cellAddress, [])
        problem.message.contains("There is already a file in the seqcenter inbox but it is different from this metadata file.")
    }

    void 'validate, when contents are different, adds Error'() {
        given:
        MetadataValidationContext context = createContextAndSeqCenter(CONTEXT + "\nCenter\t1234")

        when:
        metadataFileCopiedValidator.validate(context)

        then:
        Problem problem = CollectionUtils.exactlyOneElement(context.problems)
        problem.level == LogLevel.ERROR
        TestCase.assertContainSame(problem.affectedCells*.cellAddress, [])
        problem.message.contains("There is already a file in the seqcenter inbox but it is different from this metadata file.")
    }

    private MetadataValidationContext createContextAndSeqCenter(String contextString, boolean copyMetadataFile = true) {
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                contextString,
                [
                        content: contextString.bytes,
                ]
        )
        DomainFactory.createSeqCenter(
                name: 'Center',
                copyMetadataFile: copyMetadataFile,
        )
        return context
    }
}
