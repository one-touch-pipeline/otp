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
            result = analysisService.getFiles(null, null)
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
            file = analysisService.getFiles(analysisInstance, plotType) ? analysisService.getFiles(analysisInstance, plotType).first() : null
        }

        then:
        !file

        where:
        analysis       | instance               | plotType
        "Snv"          | SnvCallingInstance     | PlotType.SNV
        "IndelCalling" | IndelCallingInstance   | PlotType.INDEL
        "IndelCalling" | IndelCallingInstance   | PlotType.INDEL_TINDA
        "Aceseq"       | AceseqInstance         | PlotType.ACESEQ_ALL
        "Aceseq"       | AceseqInstance         | PlotType.ACESEQ_WG_COVERAGE
        "Sophia"       | SophiaInstance         | PlotType.SOPHIA
    }
}
