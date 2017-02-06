package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import grails.validation.*
import spock.lang.*

class ImportProcessIntegrationSpec extends Specification {

    ImportProcess importProcess01
    ImportProcess importProcess02
    ImportProcess importProcess03
    ExternallyProcessedMergedBamFile epmbf01
    ExternallyProcessedMergedBamFile epmbf02
    ExternallyProcessedMergedBamFile epmbf03
    ExternallyProcessedMergedBamFile epmbf04
    ExternallyProcessedMergedBamFile epmbf05

    def setup() {
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
        Set<ExternallyProcessedMergedBamFile> externallyProcessedMergedBamFiles = [epmbf01, epmbf02, epmbf03]
        importProcess01.externallyProcessedMergedBamFiles = externallyProcessedMergedBamFiles
        assert importProcess01.save(flush: true)

        when:
        importProcess02.externallyProcessedMergedBamFiles = externallyProcessedMergedBamFiles
        importProcess02.save(flush: true)

        then:
        ValidationException e = thrown()
        e.message.contains('This set of bam files was already imported')
    }

    void "test validate set of externally processed merged bam files partially overlapped by another one throwns an expection"() {
        given:
        Set<ExternallyProcessedMergedBamFile> externallyProcessedMergedBamFiles01 = [epmbf01, epmbf02]
        Set<ExternallyProcessedMergedBamFile> externallyProcessedMergedBamFiles02 = [epmbf01, epmbf03]
        importProcess01.externallyProcessedMergedBamFiles = externallyProcessedMergedBamFiles01
        assert importProcess01.save(flush: true)

        when:
        importProcess02.externallyProcessedMergedBamFiles = externallyProcessedMergedBamFiles02
        importProcess02.save(flush: true)

        then:
        ValidationException e = thrown()
        e.message.contains('This set of bam files was already imported')
    }

    void "test validate two sets of externally processed merged bam files partially overlapped by another one throwns an expection"() {
        given:
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
        e.message.contains('This set of bam files was already imported')
    }
}
