package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import spock.lang.*

class AnalysisDeletionServiceIntegrationSpec extends Specification {

    AnalysisDeletionService analysisDeletionService

    SnvCallingInstanceTestData testData
    SnvCallingInstance snvCallingInstance
    IndelCallingInstance indelCallingInstance
    AceseqInstance aceseqInstance
    ProcessedMergedBamFile bamFileTumor2
    SamplePair samplePair2
    List<File> analysisInstancesDirectories
    List<File> analysisSamplePairsDirectories
    List<SamplePair> samplePairs

    void setup() {
        testData = new SnvCallingInstanceTestData()
        testData.createSnvObjects()
        snvCallingInstance = testData.createSnvCallingInstance()
        assert snvCallingInstance.save()
        indelCallingInstance = DomainFactory.createIndelInstanceWithSameSamplePair(snvCallingInstance)
        aceseqInstance = DomainFactory.createAceseqInstanceWithSameSamplePair(snvCallingInstance)
        Realm realm = DomainFactory.createRealmDataManagement()
        assert realm.save()
        createAllJobResults(snvCallingInstance)
        (bamFileTumor2, samplePair2) = testData.createDisease(testData.bamFileControl.mergingWorkPackage)

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
        testData = null
        snvCallingInstance = null
        indelCallingInstance = null
        aceseqInstance = null
        analysisInstancesDirectories = null
        analysisSamplePairsDirectories = null
        samplePairs = null
    }

    void createAllJobResults(SnvCallingInstance instance) {
        SnvJobResult callingResult = testData.createAndSaveSnvJobResult(instance, SnvCallingStep.CALLING)
        SnvJobResult annotationResult = testData.createAndSaveSnvJobResult(instance, SnvCallingStep.SNV_ANNOTATION, callingResult)
        SnvJobResult deepAnnotationResult = testData.createAndSaveSnvJobResult(instance, SnvCallingStep.SNV_DEEPANNOTATION, annotationResult)
        testData.createAndSaveSnvJobResult(instance, SnvCallingStep.FILTER_VCF, deepAnnotationResult)
    }

    def "delete instance and then delete sample pairs without analysis instances from analysis deletion service with analysis instances finished for control tumor 1"() {
        given:
        snvCallingInstance.processingState = AnalysisProcessingStates.FINISHED
        indelCallingInstance.processingState = AnalysisProcessingStates.FINISHED
        aceseqInstance.processingState = AnalysisProcessingStates.FINISHED
        List<File> instancesDirectories = new ArrayList<>()
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
        SnvCallingInstance.list() == []
        SnvJobResult.list() == []
        IndelCallingInstance.list() == []
        AceseqInstance.list() == []
        SamplePair.list() == [samplePair2]
    }

    def "delete instance and then delete sample pairs without analysis instances from analysis deletion service with analysis instances finished for control tumor 1 and control tumor 2"() {
        given:
        snvCallingInstance.processingState = AnalysisProcessingStates.FINISHED
        indelCallingInstance.processingState = AnalysisProcessingStates.FINISHED
        aceseqInstance.processingState = AnalysisProcessingStates.FINISHED

        SnvCallingInstance snvCallingInstance2 = testData.createSnvCallingInstance([
                sampleType1BamFile: bamFileTumor2,
                processingState: AnalysisProcessingStates.FINISHED,
                samplePair: samplePair2,
        ])
        assert snvCallingInstance2.save()
        IndelCallingInstance indelCallingInstance2 = DomainFactory.createIndelInstanceWithSameSamplePair(snvCallingInstance2)
        AceseqInstance aceseqInstance2 = DomainFactory.createAceseqInstanceWithSameSamplePair(snvCallingInstance2)
        List<File> instancesDirectories = new ArrayList<>()
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
        SnvCallingInstance.list() == []
        IndelCallingInstance.list() == []
        AceseqInstance.list() == []
        SamplePair.list() == []
    }
}
