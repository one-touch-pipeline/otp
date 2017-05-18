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

    void "checkFile with callingInstance and no File"(){
        given:
        SnvConfig snvConfig = DomainFactory.createSnvConfig()
        ProcessedMergedBamFile processedMergedBamFile1 = DomainFactory.createProcessedMergedBamFile()
        ProcessedMergedBamFile processedMergedBamFile2 = DomainFactory.createProcessedMergedBamFile()
        SamplePair samplePair = DomainFactory.createSamplePair()

        DomainFactory.createRealm(
                name: samplePair.project.realmName,
                operationType: Realm.OperationType.DATA_MANAGEMENT,
        )

        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvCallingInstance(
                instanceName: "INSTANCE_NAME",
                config: snvConfig,
                sampleType1BamFile: processedMergedBamFile1,
                sampleType2BamFile: processedMergedBamFile2,
                samplePair: samplePair,
                processingState: AnalysisProcessingStates.FINISHED,
        )
        IndelCallingInstance indelCallingInstance = DomainFactory.createIndelCallingInstance(
                instanceName: "INSTANCE_NAME",
                config: snvConfig,
                sampleType1BamFile: processedMergedBamFile1,
                sampleType2BamFile: processedMergedBamFile2,
                samplePair: samplePair,
                processingState: AnalysisProcessingStates.FINISHED,
        )

        when:
        File file1
        File file2
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            file1 = analysisService.checkFile(snvCallingInstance)
            file2 = analysisService.checkFile(indelCallingInstance)
        }

        then:
        !file1
        !file2
    }
}
