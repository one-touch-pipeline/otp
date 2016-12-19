package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.testing.*
import grails.plugin.springsecurity.*
import grails.test.spock.*
import spock.lang.Unroll

class AnalysisServiceIntegrationSpec extends IntegrationSpec implements UserAndRoles {

    AnalysisService analysisService = new AnalysisService()

    @Unroll
    void "getCallingInstancesForProject with SnvCallingInstance"(){
        given:
        createUserAndRoles()

        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvInstanceWithRoddyBamFiles()

        when:
        List callingInstances
        SpringSecurityUtils.doWithAuth("operator") {
            callingInstances = analysisService.getCallingInstancesForProject(SnvCallingInstance, snvCallingInstance.samplePair.project.name)
        }

        then:
        callingInstances.size() == 1
    }

    @Unroll
    void "getCallingInstancesForProject with IndelCallingInstance"(){
        given:
        createUserAndRoles()

        IndelCallingInstance indelCallingInstance = DomainFactory.createIndelCallingInstanceWithRoddyBamFiles()

        when:
        List callingInstances
        SpringSecurityUtils.doWithAuth("operator") {
            callingInstances = analysisService.getCallingInstancesForProject(IndelCallingInstance, indelCallingInstance.samplePair.project.name)
        }

        then:
        callingInstances.size() == 1
    }

    void "checkFile with no callingInstance"() {
        expect:
        analysisService.checkFile(null) == null
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
        File file1 = analysisService.checkFile(snvCallingInstance)
        File file2 = analysisService.checkFile(indelCallingInstance)

        then:
        !file1
        !file2
    }
}
