/*
 * Copyright 2011-2025 The OTP authors
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

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqInstance
import de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqService
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelCallingInstance
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelCallingService
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaInstance
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaService
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaService
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.pipelines.externalBam.ExternalBamFactoryInstance
import de.dkfz.tbi.otp.ngsdata.DomainFactory

@Rollback
@Integration
class AnalysisDeletionServiceIntegrationSpec extends Specification implements IsRoddy {

    AnalysisDeletionService analysisDeletionService
    AceseqService aceseqService
    SnvCallingService snvCallingService
    SophiaService sophiaService
    IndelCallingService indelCallingService
    RunYapsaService runYapsaService

    SamplePair samplePair
    RoddySnvCallingInstance snvCallingInstance
    IndelCallingInstance indelCallingInstance
    SophiaInstance sophiaInstance
    AceseqInstance aceseqInstance
    RunYapsaInstance runYapsaInstance
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
        runYapsaInstance = DomainFactory.createRunYapsaInstanceWithSameSamplePair(snvCallingInstance)

        snvCallingInstance.processingState = AnalysisProcessingStates.FINISHED
        indelCallingInstance.processingState = AnalysisProcessingStates.FINISHED
        sophiaInstance.processingState = AnalysisProcessingStates.FINISHED
        aceseqInstance.processingState = AnalysisProcessingStates.FINISHED
        runYapsaInstance.processingState = AnalysisProcessingStates.FINISHED

        samplePair2 = DomainFactory.createDisease(samplePair.mergingWorkPackage2)
        bamFileTumor2 = createRoddyBamFile(
                DomainFactory.randomBamFileProperties,
                samplePair2.mergingWorkPackage1,
                RoddyBamFile,
        )

        analysisInstancesDirectories = [
                analysisDeletionService.analysisWorkFileServiceFactoryService.getService(AceseqInstance).getDirectoryPath(aceseqInstance),
                analysisDeletionService.analysisWorkFileServiceFactoryService.getService(RoddySnvCallingInstance).getDirectoryPath(snvCallingInstance),
                analysisDeletionService.analysisWorkFileServiceFactoryService.getService(SophiaInstance).getDirectoryPath(sophiaInstance),
                analysisDeletionService.analysisWorkFileServiceFactoryService.getService(IndelCallingInstance).getDirectoryPath(indelCallingInstance),
                analysisDeletionService.analysisWorkFileServiceFactoryService.getService(RunYapsaInstance).getDirectoryPath(runYapsaInstance),
        ].collect { analysisDeletionService.fileService.toFile(it) }
        analysisSamplePairsDirectories = [
                analysisDeletionService.analysisLinkFileServiceFactoryService.getService(AceseqInstance).getSamplePairPath(aceseqInstance.samplePair),
                analysisDeletionService.analysisLinkFileServiceFactoryService.getService(RoddySnvCallingInstance).getSamplePairPath(snvCallingInstance.samplePair),
                analysisDeletionService.analysisLinkFileServiceFactoryService.getService(SophiaInstance).getSamplePairPath(sophiaInstance.samplePair),
                analysisDeletionService.analysisLinkFileServiceFactoryService.getService(IndelCallingInstance).getSamplePairPath(indelCallingInstance.samplePair),
                analysisDeletionService.analysisLinkFileServiceFactoryService.getService(RunYapsaInstance).getSamplePairPath(runYapsaInstance.samplePair),
        ].collect { analysisDeletionService.fileService.toFile(it) }
        samplePairs = [
                snvCallingInstance.samplePair,
                indelCallingInstance.samplePair,
                sophiaInstance.samplePair,
                aceseqInstance.samplePair,
                runYapsaInstance.samplePair,
        ]
    }

    void "delete instance and then delete sample pairs without analysis instances from analysis deletion service with analysis instances finished for control tumor 1"() {
        given:
        setupData()

        List<File> instancesDirectories = []
        List<File> samplePairsDirectories

        when:
        BamFilePairAnalysis.findAll().each {
            instancesDirectories.addAll(analysisDeletionService.deleteInstance(it))
        }

        and:
        samplePairsDirectories = analysisDeletionService.deleteSamplePairsWithoutAnalysisInstances(samplePairs)

        then:
        TestCase.assertContainSame(instancesDirectories*.toString(), analysisInstancesDirectories*.toString())
        TestCase.assertContainSame(samplePairsDirectories*.toString(), analysisSamplePairsDirectories*.toString())
        !RoddySnvCallingInstance.count()
        !IndelCallingInstance.count()
        !SophiaInstance.count()
        !AceseqInstance.count()
        !RunYapsaInstance.count()
        SamplePair.list() == [samplePair2]
    }

    void "delete instance and then delete sample pairs without analysis instances from analysis deletion service with analysis instances finished for control tumor 1 and control tumor 2"() {
        given:
        setupData()

        RoddySnvCallingInstance snvCallingInstance2 = DomainFactory.createRoddySnvCallingInstance([
                sampleType1BamFile: bamFileTumor2,
                sampleType2BamFile: samplePair.mergingWorkPackage2.bamFileInProjectFolder,
                processingState   : AnalysisProcessingStates.FINISHED,
                samplePair        : samplePair2,
                config            : snvCallingInstance.config,
        ])
        assert snvCallingInstance2.save(flush: true)
        IndelCallingInstance indelCallingInstance2 = DomainFactory.createIndelCallingInstanceWithSameSamplePair(snvCallingInstance2)
        SophiaInstance sophiaInstance2 = DomainFactory.createSophiaInstanceWithSameSamplePair(snvCallingInstance2)
        AceseqInstance aceseqInstance2 = DomainFactory.createAceseqInstanceWithSameSamplePair(snvCallingInstance2)
        RunYapsaInstance runYapsaInstance2 = DomainFactory.createRunYapsaInstanceWithSameSamplePair(snvCallingInstance2)
        List<File> instancesDirectories = []
        List<File> samplePairsDirectories

        analysisInstancesDirectories.addAll([
                analysisDeletionService.analysisWorkFileServiceFactoryService.getService(AceseqInstance).getDirectoryPath(aceseqInstance2),
                analysisDeletionService.analysisWorkFileServiceFactoryService.getService(RoddySnvCallingInstance).getDirectoryPath(snvCallingInstance2),
                analysisDeletionService.analysisWorkFileServiceFactoryService.getService(SophiaInstance).getDirectoryPath(sophiaInstance2),
                analysisDeletionService.analysisWorkFileServiceFactoryService.getService(IndelCallingInstance).getDirectoryPath(indelCallingInstance2),
                analysisDeletionService.analysisWorkFileServiceFactoryService.getService(RunYapsaInstance).getDirectoryPath(runYapsaInstance2),
        ].collect { analysisDeletionService.fileService.toFile(it) }
        )
        analysisSamplePairsDirectories.addAll([
                analysisDeletionService.analysisLinkFileServiceFactoryService.getService(AceseqInstance).getSamplePairPath(aceseqInstance2.samplePair),
                analysisDeletionService.analysisLinkFileServiceFactoryService.getService(RoddySnvCallingInstance).getSamplePairPath(snvCallingInstance2.samplePair),
                analysisDeletionService.analysisLinkFileServiceFactoryService.getService(SophiaInstance).getSamplePairPath(sophiaInstance2.samplePair),
                analysisDeletionService.analysisLinkFileServiceFactoryService.getService(IndelCallingInstance).getSamplePairPath(indelCallingInstance2.samplePair),
                analysisDeletionService.analysisLinkFileServiceFactoryService.getService(RunYapsaInstance).getSamplePairPath(runYapsaInstance2.samplePair),
        ].collect { analysisDeletionService.fileService.toFile(it) }
        )
        samplePairs.addAll([
                snvCallingInstance2.samplePair,
                indelCallingInstance2.samplePair,
                sophiaInstance2.samplePair,
                aceseqInstance2.samplePair,
                runYapsaInstance2.samplePair,
        ])

        when:
        BamFilePairAnalysis.findAll().each {
            instancesDirectories.addAll(analysisDeletionService.deleteInstance(it))
        }

        and:
        samplePairsDirectories = analysisDeletionService.deleteSamplePairsWithoutAnalysisInstances(samplePairs)

        then:
        TestCase.assertContainSame(instancesDirectories*.toString(), analysisInstancesDirectories*.toString())
        TestCase.assertContainSame(samplePairsDirectories*.toString(), analysisSamplePairsDirectories*.toString())
        !RoddySnvCallingInstance.count()
        !IndelCallingInstance.count()
        !SophiaInstance.count()
        !AceseqInstance.count()
        !RunYapsaInstance.count()
        SamplePair.list() == []
    }

    void "deleteSamplePairsWithoutAnalysisInstances, when two sample pairs exist for same sample types and seqtype and other has still data, then do not delete the sample directory"() {
        given:
        setupData()
        List<File> samplePairsDirectories

        SamplePair samplePair3 = DomainFactory.createSamplePair([
                mergingWorkPackage1: ExternalBamFactoryInstance.INSTANCE.createMergingWorkPackage([
                        sample : samplePair.mergingWorkPackage1.sample,
                        seqType: samplePair.mergingWorkPackage1.seqType,
                ]),
                mergingWorkPackage2: ExternalBamFactoryInstance.INSTANCE.createMergingWorkPackage([
                        sample : samplePair.mergingWorkPackage2.sample,
                        seqType: samplePair.mergingWorkPackage2.seqType,
                ]),
        ])

        when:
        samplePairsDirectories = analysisDeletionService.deleteSamplePairsWithoutAnalysisInstances([samplePair3])

        then:
        !SamplePair.get(samplePair3.id)
        samplePairsDirectories.empty
    }
}
