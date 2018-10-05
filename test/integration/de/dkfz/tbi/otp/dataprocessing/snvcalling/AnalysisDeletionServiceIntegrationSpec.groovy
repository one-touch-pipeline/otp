package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import spock.lang.*

class AnalysisDeletionServiceIntegrationSpec extends Specification {

    AnalysisDeletionService analysisDeletionService

    SamplePair samplePair
    RoddySnvCallingInstance snvCallingInstance
    IndelCallingInstance indelCallingInstance
    AceseqInstance aceseqInstance
    ProcessedMergedBamFile bamFileTumor2
    SamplePair samplePair2
    List<File> analysisInstancesDirectories
    List<File> analysisSamplePairsDirectories
    List<SamplePair> samplePairs

    void setup() {
        samplePair = DomainFactory.createSamplePairWithProcessedMergedBamFiles()
        snvCallingInstance = DomainFactory.createRoddySnvCallingInstance(
                sampleType1BamFile  : samplePair.mergingWorkPackage1.bamFileInProjectFolder,
                sampleType2BamFile: samplePair.mergingWorkPackage2.bamFileInProjectFolder,
                samplePair    : samplePair,
        )
        indelCallingInstance = DomainFactory.createIndelCallingInstanceWithSameSamplePair(snvCallingInstance)
        aceseqInstance = DomainFactory.createAceseqInstanceWithSameSamplePair(snvCallingInstance)
        DomainFactory.createRealm()

        samplePair2 = DomainFactory.createDisease(samplePair.mergingWorkPackage2)
        bamFileTumor2 = DomainFactory.createProcessedMergedBamFile(
                samplePair2.mergingWorkPackage1,
                DomainFactory.randomProcessedBamFileProperties
        )

        analysisInstancesDirectories = [
                snvCallingInstance.getInstancePath().getAbsoluteDataManagementPath(),
                indelCallingInstance.getInstancePath().getAbsoluteDataManagementPath(),
                aceseqInstance.getInstancePath().getAbsoluteDataManagementPath(),
        ]
        analysisSamplePairsDirectories = [
                snvCallingInstance.samplePair.getSnvSamplePairPath().getAbsoluteDataManagementPath(),
                indelCallingInstance.samplePair.getIndelSamplePairPath().getAbsoluteDataManagementPath(),
                aceseqInstance.samplePair.getAceseqSamplePairPath().getAbsoluteDataManagementPath(),
        ]
        samplePairs = [
                snvCallingInstance.samplePair,
                indelCallingInstance.samplePair,
                aceseqInstance.samplePair,
        ]
    }

    void cleanup() {
        analysisDeletionService = null
        snvCallingInstance = null
        indelCallingInstance = null
        aceseqInstance = null
        analysisInstancesDirectories = null
        analysisSamplePairsDirectories = null
        samplePairs = null
    }

    def "delete instance and then delete sample pairs without analysis instances from analysis deletion service with analysis instances finished for control tumor 1"() {
        given:
        snvCallingInstance.processingState = AnalysisProcessingStates.FINISHED
        indelCallingInstance.processingState = AnalysisProcessingStates.FINISHED
        aceseqInstance.processingState = AnalysisProcessingStates.FINISHED
        List<File> instancesDirectories = []
        List<File> samplePairsDirectories


        when:
        BamFilePairAnalysis.findAll().each {
            instancesDirectories.add(analysisDeletionService.deleteInstance(it))
        }

        and:
        samplePairsDirectories = analysisDeletionService.deleteSamplePairsWithoutAnalysisInstances(samplePairs)

        then:
        instancesDirectories.containsAll(analysisInstancesDirectories)
        samplePairsDirectories.containsAll(analysisSamplePairsDirectories)
        RoddySnvCallingInstance.list() == []
        IndelCallingInstance.list() == []
        AceseqInstance.list() == []
        SamplePair.list() == [samplePair2]
    }

    def "delete instance and then delete sample pairs without analysis instances from analysis deletion service with analysis instances finished for control tumor 1 and control tumor 2"() {
        given:
        snvCallingInstance.processingState = AnalysisProcessingStates.FINISHED
        indelCallingInstance.processingState = AnalysisProcessingStates.FINISHED
        aceseqInstance.processingState = AnalysisProcessingStates.FINISHED

        RoddySnvCallingInstance snvCallingInstance2 = DomainFactory.createRoddySnvCallingInstance([
                sampleType1BamFile: bamFileTumor2,
                sampleType2BamFile: samplePair.mergingWorkPackage2.bamFileInProjectFolder,
                processingState: AnalysisProcessingStates.FINISHED,
                samplePair: samplePair2,
                config: snvCallingInstance.config,
        ])
        assert snvCallingInstance2.save()
        IndelCallingInstance indelCallingInstance2 = DomainFactory.createIndelCallingInstanceWithSameSamplePair(snvCallingInstance2)
        AceseqInstance aceseqInstance2 = DomainFactory.createAceseqInstanceWithSameSamplePair(snvCallingInstance2)
        List<File> instancesDirectories = []
        List<File> samplePairsDirectories

        analysisInstancesDirectories.addAll(
                snvCallingInstance2.getInstancePath().getAbsoluteDataManagementPath(),
                indelCallingInstance2.getInstancePath().getAbsoluteDataManagementPath(),
                aceseqInstance2.getInstancePath().getAbsoluteDataManagementPath()
        )
        analysisSamplePairsDirectories.addAll(
                snvCallingInstance2.samplePair.getSnvSamplePairPath().getAbsoluteDataManagementPath(),
                indelCallingInstance2.samplePair.getIndelSamplePairPath().getAbsoluteDataManagementPath(),
                aceseqInstance2.samplePair.getAceseqSamplePairPath().getAbsoluteDataManagementPath()
        )
        samplePairs.addAll(
                snvCallingInstance2.samplePair,
                indelCallingInstance2.samplePair,
                aceseqInstance2.samplePair
        )

        when:
        BamFilePairAnalysis.findAll().each {
            instancesDirectories.add(analysisDeletionService.deleteInstance(it))
        }

        and:
        samplePairsDirectories = analysisDeletionService.deleteSamplePairsWithoutAnalysisInstances(samplePairs)

        then:
        instancesDirectories.containsAll(analysisInstancesDirectories)
        samplePairsDirectories.containsAll(analysisSamplePairsDirectories)
        RoddySnvCallingInstance.list() == []
        IndelCallingInstance.list() == []
        AceseqInstance.list() == []
        SamplePair.list() == []
    }
}
