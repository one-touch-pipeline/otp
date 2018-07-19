package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.test.spock.*
import spock.lang.*

class SophiaServiceIntegrationSpec extends IntegrationSpec {

    SamplePair samplePair1
    ConfigPerProjectAndSeqType roddyConfig1
    AbstractMergedBamFile bamFile1_1
    AbstractMergedBamFile bamFile2_1

    SophiaService sophiaService

    def setup() {
        def map = DomainFactory.createProcessableSamplePair()

        samplePair1 = map.samplePair
        bamFile1_1 = map.bamFile1
        bamFile2_1 = map.bamFile2
        roddyConfig1 = map.roddyConfig

        DomainFactory.createAllAnalysableSeqTypes()
    }

    void "samplePairForProcessing, for Sophia pipeline, only PMBF available, should not return any bam file"() {
        given:

        RoddyBamFile.list().each {
            it.withdrawn = true
            assert it.save(flush: true)
        }
        DomainFactory.createSamplePairWithProcessedMergedBamFiles()

        expect:
        !sophiaService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY)
    }

    @Unroll
    void "samplePairForProcessing, for Sophia pipeline, only EPMBF available with #property is #value, should not return any bam file"() {
        given:
        RoddyBamFile.list().each {
            it.withdrawn = true
            assert it.save(flush: true)
        }
        DomainFactory.createSamplePairWithExternalProcessedMergedBamFiles(true, [(property): value])


        expect:
        !sophiaService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY)

        where:
        property             | value
        'coverage'           | 5
        'insertSizeFile'     | null
        'maximumReadLength'  | null
    }

    @Unroll
    void "samplePairForProcessing, for Sophia pipeline, only EPMBF available with #property is #value, should return new instance"() {
        given:
        RoddyBamFile.list().each {
            it.withdrawn = true
            assert it.save(flush: true)
        }
        SamplePair samplePair = DomainFactory.createSamplePairWithExternalProcessedMergedBamFiles(true, [(property): value])

        expect:
        samplePair == sophiaService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY)

        where:
        property             | value
        'coverage'           | 30
        'coverage'           | null
        'insertSizeFile'     | 'insertSize.txt'
        'maximumReadLength'  | 5
        'maximumReadLength'  | 200
    }


}
