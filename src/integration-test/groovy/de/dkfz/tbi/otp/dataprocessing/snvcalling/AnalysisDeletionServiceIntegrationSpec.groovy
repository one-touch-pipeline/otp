/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.dataprocessing.snvcalling

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.DomainFactory

@Rollback
@Integration
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

    void setupData() {
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

    void cleanupData() {
        analysisDeletionService = null
        snvCallingInstance = null
        indelCallingInstance = null
        aceseqInstance = null
        analysisInstancesDirectories = null
        analysisSamplePairsDirectories = null
        samplePairs = null
    }

    void "delete instance and then delete sample pairs without analysis instances from analysis deletion service with analysis instances finished for control tumor 1"() {
        given:
        setupData()

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

        cleanup:
        cleanupData()
    }

    void "delete instance and then delete sample pairs without analysis instances from analysis deletion service with analysis instances finished for control tumor 1 and control tumor 2"() {
        given:
        setupData()

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
        assert snvCallingInstance2.save(flush: true)
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

        cleanup:
        cleanupData()
    }
}
