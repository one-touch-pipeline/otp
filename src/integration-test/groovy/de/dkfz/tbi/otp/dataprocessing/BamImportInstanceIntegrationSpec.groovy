/*
 * Copyright 2011-2023 The OTP authors
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
import grails.gorm.transactions.Rollback
import grails.validation.ValidationException
import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.DomainFactory

@Rollback
@Integration
class BamImportInstanceIntegrationSpec extends Specification {

    BamImportInstance importInstance01
    BamImportInstance importInstance02
    BamImportInstance importInstance03
    ExternallyProcessedBamFile epmbf01
    ExternallyProcessedBamFile epmbf02
    ExternallyProcessedBamFile epmbf03
    ExternallyProcessedBamFile epmbf04
    ExternallyProcessedBamFile epmbf05

    void setupData() {
        importInstance01 = new BamImportInstance()
        importInstance02 = new BamImportInstance()
        importInstance03 = new BamImportInstance()
        epmbf01 = DomainFactory.createExternallyProcessedBamFile(fileName: 'epmbf01')
        epmbf02 = DomainFactory.createExternallyProcessedBamFile(fileName: 'epmbf02')
        epmbf03 = DomainFactory.createExternallyProcessedBamFile(fileName: 'epmbf03')
        epmbf04 = DomainFactory.createExternallyProcessedBamFile(fileName: 'epmbf04')
        epmbf05 = DomainFactory.createExternallyProcessedBamFile(fileName: 'epmbf05')
    }

    void "test validate duplicated set of externally processed bam files throws an exception"() {
        given:
        setupData()

        Set<ExternallyProcessedBamFile> externallyProcessedBamFiles = [epmbf01, epmbf02, epmbf03]
        importInstance01.externallyProcessedBamFiles = externallyProcessedBamFiles
        assert importInstance01.save(flush: true)

        when:
        importInstance02.externallyProcessedBamFiles = externallyProcessedBamFiles
        importInstance02.save(flush: true)

        then:
        ValidationException e = thrown()
        e.message.contains('already.imported')
    }

    void "test validate set of externally processed bam files partially overlapped by another one throwns an expection"() {
        given:
        setupData()

        Set<ExternallyProcessedBamFile> externallyProcessedBamFiles01 = [epmbf01, epmbf02]
        Set<ExternallyProcessedBamFile> externallyProcessedBamFiles02 = [epmbf01, epmbf03]
        importInstance01.externallyProcessedBamFiles = externallyProcessedBamFiles01
        assert importInstance01.save(flush: true)

        when:
        importInstance02.externallyProcessedBamFiles = externallyProcessedBamFiles02
        importInstance02.save(flush: true)

        then:
        ValidationException e = thrown()
        e.message.contains('already.imported')
    }

    void "test validate two sets of externally processed bam files partially overlapped by another one throwns an expection"() {
        given:
        setupData()

        Set<ExternallyProcessedBamFile> externallyProcessedBamFiles01 = [epmbf01, epmbf02]
        Set<ExternallyProcessedBamFile> externallyProcessedBamFiles02 = [epmbf03, epmbf04]
        Set<ExternallyProcessedBamFile> externallyProcessedBamFiles03 = [epmbf02, epmbf04, epmbf05]
        importInstance01.externallyProcessedBamFiles = externallyProcessedBamFiles01
        assert importInstance01.save(flush: true)
        importInstance02.externallyProcessedBamFiles = externallyProcessedBamFiles02
        importInstance02.save(flush: true)

        when:
        importInstance03.externallyProcessedBamFiles = externallyProcessedBamFiles03
        importInstance03.save(flush: true)

        then:
        ValidationException e = thrown()
        e.message.contains('already.imported')
    }
}
