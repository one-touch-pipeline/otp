package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.dataprocessing.sophia.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.utils.logging.*
import grails.test.mixin.*
import spock.lang.*


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
