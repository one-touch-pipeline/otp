package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.dataprocessing.sophia.*
import de.dkfz.tbi.otp.testing.*
import grails.plugin.springsecurity.*
import grails.test.spock.*
import spock.lang.*

class AnalysisServiceIntegrationSpec extends IntegrationSpec implements UserAndRoles {

    AnalysisService analysisService = new AnalysisService()

    void setup() {
        createUserAndRoles()
    }

    @Unroll
    void "getCallingInstancesForProject with #analysis Instance"(){
        given:
        BamFilePairAnalysis analysisInstance = DomainFactory."create${analysis}InstanceWithRoddyBamFiles"()

        when:
        List callingInstances
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            callingInstances = analysisService.getCallingInstancesForProject(instance, analysisInstance.samplePair.project.name)
        }

        then:
        callingInstances.size() == 1

        where:
        analysis       | instance
        "Snv"          | SnvCallingInstance
        "IndelCalling" | IndelCallingInstance
        "Aceseq"       | AceseqInstance
        "Sophia"       | SophiaInstance
    }

    void "checkFile with no callingInstance"() {
        when:
        File result
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            result = analysisService.checkFile(null)
        }

        then:
        result == null
    }

    @Unroll
    void "checkFile with #instance and no File"(){
        given:
        BamFilePairAnalysis analysisInstance = DomainFactory."create${analysis}InstanceWithRoddyBamFiles"(
        )
        DomainFactory.createRealm()

        when:
        File file
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            file = analysisService.checkFile(analysisInstance)
        }

        then:
        !file

        where:
        analysis       | instance
        "Snv"          | SnvCallingInstance
        "IndelCalling" | IndelCallingInstance
        "Aceseq"       | AceseqInstance
        "Sophia"       | SophiaInstance
    }
}
