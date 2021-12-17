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
import grails.testing.services.ServiceUnitTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService

class BedFileServiceSpec extends Specification implements DataTest, ServiceUnitTest<BedFileService> {

    @Override
    Class[] getDomainClassesToMock() {
        [
                BedFile,
                ProcessingOption,
        ]
    }

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder()

    BedFile bedFile
    File referenceGenomesBaseDirectory

    void setup() {
        bedFile = DomainFactory.createBedFile([fileName: 'bedFileName'])

        referenceGenomesBaseDirectory = temporaryFolder.newFolder("reference_genomes", bedFile.referenceGenome.path, "targetRegions")
        referenceGenomesBaseDirectory.mkdirs()

        DomainFactory.createProcessingOptionBasePathReferenceGenome(referenceGenomesBaseDirectory.parentFile.parent)

        service.referenceGenomeService = new ReferenceGenomeService()
        service.referenceGenomeService.processingOptionService = new ProcessingOptionService()
    }

    void "test filePath, when bedFile is null, should fail"() {
        when:
        service.filePath(null)

        then:
        thrown(IllegalArgumentException)
    }

    void "test filePath, when bed file does not exist, should fail"() {
        when:
        service.filePath(bedFile)

        then:
        def e = thrown(RuntimeException)
        e.message.contains("the bedFile can not be read")
    }

    void "test filePath, when bed file exists, should return path to file"() {
        given:
        new File(referenceGenomesBaseDirectory, 'bedFileName').createNewFile()

        expect:
        service.filePath(bedFile) == "${referenceGenomesBaseDirectory.parentFile.path}/targetRegions/bedFileName" as String
    }
}
