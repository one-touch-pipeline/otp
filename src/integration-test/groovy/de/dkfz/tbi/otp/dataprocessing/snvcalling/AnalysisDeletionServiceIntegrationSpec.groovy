/*
 * Copyright 2011-2023 The OTP authors
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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.ngsdata.DomainFactory

@Rollback
@Integration
class AnalysisDeletionServiceIntegrationSpec extends Specification implements IsRoddy {

    AnalysisDeletionService analysisDeletionService

    SamplePair samplePair
    RoddySnvCallingInstance snvCallingInstance
    IndelCallingInstance indelCallingInstance
    SophiaInstance sophiaInstance
    AceseqInstance aceseqInstance
    AbstractBamFile bamFileTumor2
    SamplePair samplePair2
    List<File> analysisInstancesDirectories
    List<File> analysisSamplePairsDirectories
    List<SamplePair> samplePairs

    void setupData() {
        samplePair = DomainFactory.createSamplePairWithBamFiles()
        snvCallingInstance = DomainFactory.createRoddySnvCallingInstance(
                sampleType1BamFile: samplePair.mergingWorkPackage1.bamFileInProjectFolder,
                sampleType2BamFile: samplePair.mergingWorkPackage2.bamFileInProjectFolder,
                samplePair: samplePair,
                config: DomainFactory.createRoddyWorkflowConfig(pipeline: DomainFactory.createRoddySnvPipelineLazy())
        )
        indelCallingInstance = DomainFactory.createIndelCallingInstanceWithSameSamplePair(snvCallingInstance)
        sophiaInstance = DomainFactory.createSophiaInstanceWithSameSamplePair(snvCallingInstance)
        aceseqInstance = DomainFactory.createAceseqInstanceWithSameSamplePair(snvCallingInstance)
        DomainFactory.createRealm()

        samplePair2 = DomainFactory.createDisease(samplePair.mergingWorkPackage2)
        bamFileTumor2 = createRoddyBamFile(
                DomainFactory.randomBamFileProperties,
                samplePair2.mergingWorkPackage1,
                RoddyBamFile,
        )

        analysisInstancesDirectories = [
                analysisDeletionService.aceseqService.getWorkDirectory(aceseqInstance),
                analysisDeletionService.snvCallingService.getWorkDirectory(snvCallingInstance),
                analysisDeletionService.sophiaService.getWorkDirectory(sophiaInstance),
                analysisDeletionService.indelCallingService.getWorkDirectory(indelCallingInstance),
        ].collect { analysisDeletionService.fileService.toFile(it) }
        analysisSamplePairsDirectories = [
                analysisDeletionService.aceseqService.getSamplePairPath(aceseqInstance.samplePair),
                analysisDeletionService.snvCallingService.getSamplePairPath(snvCallingInstance.samplePair),
                analysisDeletionService.sophiaService.getSamplePairPath(sophiaInstance.samplePair),
                analysisDeletionService.indelCallingService.getSamplePairPath(indelCallingInstance.samplePair),
        ].collect { analysisDeletionService.fileService.toFile(it) }
        samplePairs = [
                snvCallingInstance.samplePair,
                indelCallingInstance.samplePair,
                sophiaInstance.samplePair,
                aceseqInstance.samplePair,
        ]
    }

    void "delete instance and then delete sample pairs without analysis instances from analysis deletion service with analysis instances finished for control tumor 1"() {
        given:
        setupData()

        snvCallingInstance.processingState = AnalysisProcessingStates.FINISHED
        indelCallingInstance.processingState = AnalysisProcessingStates.FINISHED
        sophiaInstance.processingState = AnalysisProcessingStates.FINISHED
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
        SophiaInstance.list() == []
        AceseqInstance.list() == []
        SamplePair.list() == [samplePair2]
    }

    void "delete instance and then delete sample pairs without analysis instances from analysis deletion service with analysis instances finished for control tumor 1 and control tumor 2"() {
        given:
        setupData()

        snvCallingInstance.processingState = AnalysisProcessingStates.FINISHED
        indelCallingInstance.processingState = AnalysisProcessingStates.FINISHED
        sophiaInstance.processingState = AnalysisProcessingStates.FINISHED
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
        SophiaInstance sophiaInstance2 = DomainFactory.createSophiaInstanceWithSameSamplePair(snvCallingInstance2)
        AceseqInstance aceseqInstance2 = DomainFactory.createAceseqInstanceWithSameSamplePair(snvCallingInstance2)
        List<File> instancesDirectories = []
        List<File> samplePairsDirectories

        analysisInstancesDirectories.addAll([
                        analysisDeletionService.aceseqService.getWorkDirectory(aceseqInstance2),
                        analysisDeletionService.snvCallingService.getWorkDirectory(snvCallingInstance2),
                        analysisDeletionService.sophiaService.getWorkDirectory(sophiaInstance2),
                        analysisDeletionService.indelCallingService.getWorkDirectory(indelCallingInstance2),
                ].collect { analysisDeletionService.fileService.toFile(it) }
        )
        analysisSamplePairsDirectories.addAll([
                        analysisDeletionService.aceseqService.getSamplePairPath(aceseqInstance2.samplePair),
                        analysisDeletionService.snvCallingService.getSamplePairPath(snvCallingInstance2.samplePair),
                        analysisDeletionService.sophiaService.getSamplePairPath(sophiaInstance2.samplePair),
                        analysisDeletionService.indelCallingService.getSamplePairPath(indelCallingInstance2.samplePair),
                ].collect { analysisDeletionService.fileService.toFile(it) }
        )
        samplePairs.addAll(
                snvCallingInstance2.samplePair,
                indelCallingInstance2.samplePair,
                sophiaInstance2.samplePair,
                aceseqInstance2.samplePair,
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
        SophiaInstance.list() == []
        AceseqInstance.list() == []
        SamplePair.list() == []
    }
}
