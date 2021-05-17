/*
 * Copyright 2011-2020 The OTP authors
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

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.security.UserAndRoles

@Rollback
@Integration
class ReferenceGenomeServiceIntegrationSpec extends Specification implements UserAndRoles, DomainFactoryCore {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder()

    ReferenceGenomeService referenceGenomeService

    ReferenceGenome referenceGenome

    void setupData() {
        referenceGenomeService = new ReferenceGenomeService(
                configService          : new TestConfigService(),
                fileService            : new FileService(),
                fileSystemService      : new TestFileSystemService(),
                processingOptionService: new ProcessingOptionService(),
        )
        referenceGenomeService.configService.processingOptionService = referenceGenomeService.processingOptionService
        referenceGenomeService.fileService.configService = referenceGenomeService.configService
        referenceGenome = createReferenceGenome()

        File referenceGenomeDirectory = temporaryFolder.newFolder("reference_genomes", referenceGenome.path)
        DomainFactory.createProcessingOptionBasePathReferenceGenome(referenceGenomeDirectory.parent)
        DomainFactory.createDefaultRealmWithProcessingOption()
    }

    void "createReferenceGenomeMetafile, file is created with expected content"() {
        given:
        setupData()

        [1, 2, 3].each {
            DomainFactory.createReferenceGenomeEntry(
                    referenceGenome: referenceGenome,
                    name           : "chr_${it}",
                    length         : it,
                    lengthWithoutN : it,
            )
        }

        File expectedFile = referenceGenomeService.referenceGenomeMetaInformationPath(referenceGenome)
        String expectedContent = """\
            |chr_1\t1\t1
            |chr_2\t2\t2
            |chr_3\t3\t3""".stripMargin()

        when:
        referenceGenomeService.createReferenceGenomeMetafile(referenceGenome)

        then:
        expectedFile.exists()
        expectedFile.text == expectedContent
    }
}
