/*
 * Copyright 2011-2024 The OTP authors
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
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore

class MetaDataFileServiceSpec extends Specification implements ServiceUnitTest<MetaDataFileService>, DataTest, DomainFactoryCore {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                MetaDataFile,
                FastqImportInstance,
        ]
    }

    void "getByFastqImportInstance, should return the metaDataFile"() {
        given:
        FastqImportInstance fastqImportInstance = createFastqImportInstance()

        MetaDataFile metaDataFile1 = DomainFactory.createMetaDataFile(fastqImportInstance: fastqImportInstance)
        DomainFactory.createMetaDataFile()

        when:
        MetaDataFile metaDataFile = service.findByFastqImportInstance(fastqImportInstance)

        then:
        metaDataFile == metaDataFile1
    }

    void "getByFastqImportInstance, should throw exception if more than one MetaDataFiles are found"() {
        given:
        FastqImportInstance fastqImportInstance = createFastqImportInstance()
        DomainFactory.createMetaDataFile()
        DomainFactory.createMetaDataFile(fastqImportInstance: fastqImportInstance)
        DomainFactory.createMetaDataFile(fastqImportInstance: fastqImportInstance)

        when:
        service.findByFastqImportInstance(fastqImportInstance)

        then:
        thrown AssertionError
    }
}
