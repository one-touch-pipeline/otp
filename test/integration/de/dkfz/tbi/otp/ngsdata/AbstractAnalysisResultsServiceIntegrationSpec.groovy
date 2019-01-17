package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.aceseq.*
import de.dkfz.tbi.otp.dataprocessing.indelcalling.*
import de.dkfz.tbi.otp.dataprocessing.runYapsa.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.dataprocessing.sophia.*
import de.dkfz.tbi.otp.security.*
import grails.plugin.springsecurity.*
import grails.test.spock.*
import spock.lang.*

class AbstractAnalysisResultsServiceIntegrationSpec extends IntegrationSpec implements UserAndRoles {

    AbstractAnalysisResultsService abstractAnalysisResultsService

    void setup() {
        createUserAndRoles()
    }

    @Unroll
    void "getCallingInstancesForProject with #analysis Instance"(){
        given:
        BamFilePairAnalysis analysisInstance = DomainFactory."create${analysis}InstanceWithRoddyBamFiles"()
        abstractAnalysisResultsService = service.newInstance()
        abstractAnalysisResultsService.projectService = [
                getProjectByName: { projectName -> analysisInstance.project }
        ] as ProjectService

        when:
        List callingInstances
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            callingInstances = abstractAnalysisResultsService.getCallingInstancesForProject(analysisInstance.samplePair.project.name)
        }

        then:
        callingInstances.size() == 1

        where:
        service                | analysis       | instance
        SnvResultsService      | "RoddySnv"     | RoddySnvCallingInstance
        IndelResultsService    | "IndelCalling" | IndelCallingInstance
        AceseqResultsService   | "Aceseq"       | AceseqInstance
        SophiaResultsService   | "Sophia"       | SophiaInstance
        RunYapsaResultsService | "RunYapsa"     | RunYapsaInstance
    }

    void "checkFile with no callingInstance"() {
        when:
         abstractAnalysisResultsService = Mock(AbstractAnalysisResultsService)
       File result
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            result = abstractAnalysisResultsService.getFiles(null, null)
        }

        then:
        result == null
    }

    @Unroll
    void "checkFile with #instance and no File"(){
        given:
        abstractAnalysisResultsService = Mock(AbstractAnalysisResultsService)
        BamFilePairAnalysis analysisInstance = DomainFactory."create${analysis}InstanceWithRoddyBamFiles"()
        DomainFactory.createRealm()

        when:
        File file
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            file = abstractAnalysisResultsService.getFiles(analysisInstance, plotType) ? abstractAnalysisResultsService.getFiles(analysisInstance, plotType).first() : null
        }

        then:
        !file

        where:
        analysis       | instance               | plotType
        "RoddySnv"     | RoddySnvCallingInstance| PlotType.SNV
        "IndelCalling" | IndelCallingInstance   | PlotType.INDEL
        "IndelCalling" | IndelCallingInstance   | PlotType.INDEL_TINDA
        "Aceseq"       | AceseqInstance         | PlotType.ACESEQ_ALL
        "Aceseq"       | AceseqInstance         | PlotType.ACESEQ_WG_COVERAGE
        "Sophia"       | SophiaInstance         | PlotType.SOPHIA
    }
}
