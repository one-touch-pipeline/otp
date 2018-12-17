package de.dkfz.tbi.otp.dataprocessing

import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.snvcalling.AbstractSnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

class AbstractMergedBamFileIntegrationSpec extends Specification {


    void "withdraw, check that analysis also marked as withdrawn"() {
        given:
        AbstractSnvCallingInstance snvCallingInstance = DomainFactory.createRoddySnvInstanceWithRoddyBamFiles()
        IndelCallingInstance indelCallingInstance = DomainFactory.createIndelCallingInstanceWithSameSamplePair(snvCallingInstance)
        SophiaInstance sophiaInstance = DomainFactory.createSophiaInstanceWithSameSamplePair(snvCallingInstance)
        AceseqInstance aceseqInstance = DomainFactory.createAceseqInstanceWithSameSamplePair(snvCallingInstance)

        when:
        LogThreadLocal.withThreadLog(System.out) {
            snvCallingInstance.sampleType1BamFile.withdraw()
        }

        then:
        snvCallingInstance.sampleType1BamFile.withdrawn
        snvCallingInstance.withdrawn
        indelCallingInstance.withdrawn
        sophiaInstance.withdrawn
        aceseqInstance.withdrawn
    }
}
