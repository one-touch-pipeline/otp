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
package de.dkfz.tbi.otp.dataprocessing

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import grails.validation.ValidationException
import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.DomainFactory

@Rollback
@Integration
class ImportProcessIntegrationSpec extends Specification {

    ImportProcess importProcess01
    ImportProcess importProcess02
    ImportProcess importProcess03
    ExternallyProcessedMergedBamFile epmbf01
    ExternallyProcessedMergedBamFile epmbf02
    ExternallyProcessedMergedBamFile epmbf03
    ExternallyProcessedMergedBamFile epmbf04
    ExternallyProcessedMergedBamFile epmbf05

    void setupData() {
        importProcess01 = new ImportProcess()
        importProcess02 = new ImportProcess()
        importProcess03 = new ImportProcess()
        epmbf01 = DomainFactory.createExternallyProcessedMergedBamFile(fileName: 'epmbf01')
        epmbf02 = DomainFactory.createExternallyProcessedMergedBamFile(fileName: 'epmbf02')
        epmbf03 = DomainFactory.createExternallyProcessedMergedBamFile(fileName: 'epmbf03')
        epmbf04 = DomainFactory.createExternallyProcessedMergedBamFile(fileName: 'epmbf04')
        epmbf05 = DomainFactory.createExternallyProcessedMergedBamFile(fileName: 'epmbf05')
    }

    void "test validate duplicated set of externally processed merged bam files throws an exception"() {
        given:
        setupData()

        Set<ExternallyProcessedMergedBamFile> externallyProcessedMergedBamFiles = [epmbf01, epmbf02, epmbf03]
        importProcess01.externallyProcessedMergedBamFiles = externallyProcessedMergedBamFiles
        assert importProcess01.save(flush: true)

        when:
        importProcess02.externallyProcessedMergedBamFiles = externallyProcessedMergedBamFiles
        importProcess02.save(flush: true)

        then:
        ValidationException e = thrown()
        e.message.contains('already.imported')
    }

    void "test validate set of externally processed merged bam files partially overlapped by another one throwns an expection"() {
        given:
        setupData()

        Set<ExternallyProcessedMergedBamFile> externallyProcessedMergedBamFiles01 = [epmbf01, epmbf02]
        Set<ExternallyProcessedMergedBamFile> externallyProcessedMergedBamFiles02 = [epmbf01, epmbf03]
        importProcess01.externallyProcessedMergedBamFiles = externallyProcessedMergedBamFiles01
        assert importProcess01.save(flush: true)

        when:
        importProcess02.externallyProcessedMergedBamFiles = externallyProcessedMergedBamFiles02
        importProcess02.save(flush: true)

        then:
        ValidationException e = thrown()
        e.message.contains('already.imported')
    }

    void "test validate two sets of externally processed merged bam files partially overlapped by another one throwns an expection"() {
        given:
        setupData()

        Set<ExternallyProcessedMergedBamFile> externallyProcessedMergedBamFiles01 = [epmbf01, epmbf02]
        Set<ExternallyProcessedMergedBamFile> externallyProcessedMergedBamFiles02 = [epmbf03, epmbf04]
        Set<ExternallyProcessedMergedBamFile> externallyProcessedMergedBamFiles03 = [epmbf02, epmbf04, epmbf05]
        importProcess01.externallyProcessedMergedBamFiles = externallyProcessedMergedBamFiles01
        assert importProcess01.save(flush: true)
        importProcess02.externallyProcessedMergedBamFiles = externallyProcessedMergedBamFiles02
        importProcess02.save(flush: true)

        when:
        importProcess03.externallyProcessedMergedBamFiles = externallyProcessedMergedBamFiles03
        importProcess03.save(flush: true)

        then:
        ValidationException e = thrown()
        e.message.contains('already.imported')
    }
}
