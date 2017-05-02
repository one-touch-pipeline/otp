package de.dkfz.tbi.otp.monitor

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*
import spock.lang.*

class VariantCallingPipelinesCheckerSpec extends Specification {

    void "handle, when no bamFiles given, then return empty list and create no output"() {
        given:
        MonitorOutputCollector output = Mock(MonitorOutputCollector)
        VariantCallingPipelinesChecker checker = new VariantCallingPipelinesChecker()

        when:
        List<SamplePair> result = checker.handle([], output)

        then:
        [] == result
        0 * output._
    }

    void "handle, if bamFiles given, then return SamplePairs with finished all analysis and create output for the others"() {
        given:
        MonitorOutputCollector output = Mock(MonitorOutputCollector)
        VariantCallingPipelinesChecker checker = new VariantCallingPipelinesChecker()

        and: 'sample pair with all analysis'
        SamplePair finishedSamplePair = DomainFactory.createSamplePairPanCan([
                snvProcessingStatus  : SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED,
                indelProcessingStatus: SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED,
                sophiaProcessingStatus: SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED,
                aceseqProcessingStatus: SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED,
        ])
        DomainFactory.createSampleTypePerProjectForMergingWorkPackage(finishedSamplePair.mergingWorkPackage2, SampleType.Category.CONTROL)
        BamFilePairAnalysis snvFinished = DomainFactory.createRoddySnvInstanceWithRoddyBamFiles([
                samplePair     : finishedSamplePair,
                processingState: AnalysisProcessingStates.FINISHED,
        ])
        BamFilePairAnalysis indelFinished = DomainFactory.createIndelCallingInstanceWithRoddyBamFiles([
                samplePair     : finishedSamplePair,
                processingState: AnalysisProcessingStates.FINISHED,
        ])
        BamFilePairAnalysis sophiaFinished = DomainFactory.createSophiaInstanceWithRoddyBamFiles([
                samplePair     : finishedSamplePair,
                processingState: AnalysisProcessingStates.FINISHED,
        ])
        BamFilePairAnalysis aceseqFinished = DomainFactory.createAceseqInstanceWithRoddyBamFiles([
                samplePair     : finishedSamplePair,
                processingState: AnalysisProcessingStates.FINISHED,
        ])

        and: 'sample pair with only snv'
        BamFilePairAnalysis onlySnvFinished = DomainFactory.createRoddySnvInstanceWithRoddyBamFiles([
                processingState: AnalysisProcessingStates.FINISHED,
                samplePair     : DomainFactory.createSamplePairPanCan([
                        snvProcessingStatus: SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED,
                ])
        ])
        DomainFactory.createSampleTypePerProjectForBamFile(onlySnvFinished.sampleType2BamFile, SampleType.Category.CONTROL)

        and: 'sample pair with only indel'
        BamFilePairAnalysis onlyIndelFinished = DomainFactory.createIndelCallingInstanceWithRoddyBamFiles([
                processingState: AnalysisProcessingStates.FINISHED,
                samplePair     : DomainFactory.createSamplePairPanCan([
                        indelProcessingStatus: SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED,
                ])
        ])
        DomainFactory.createSampleTypePerProjectForBamFile(onlyIndelFinished.sampleType2BamFile, SampleType.Category.CONTROL)

        and: 'sample pair with only sophia'
        BamFilePairAnalysis onlySophiaFinished = DomainFactory.createSophiaInstanceWithRoddyBamFiles([
                processingState: AnalysisProcessingStates.FINISHED,
                samplePair     : DomainFactory.createSamplePairPanCan([
                        sophiaProcessingStatus: SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED,
                ])
        ])
        DomainFactory.createSampleTypePerProjectForBamFile(onlySophiaFinished.sampleType2BamFile, SampleType.Category.CONTROL)

        and: 'sample pair with only aceseq'
        BamFilePairAnalysis onlyAceseqFinished = DomainFactory.createAceseqInstanceWithRoddyBamFiles([
                processingState: AnalysisProcessingStates.FINISHED,
                samplePair     : DomainFactory.createSamplePairPanCan([
                        aceseqProcessingStatus: SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED,
                ])
        ])
        DomainFactory.createSampleTypePerProjectForBamFile(onlyAceseqFinished.sampleType2BamFile, SampleType.Category.CONTROL)

        and: 'all bam files with processing threshold'
        List<AbstractMergedBamFile> bamFiles = [
                snvFinished,
                indelFinished,
                sophiaFinished,
                aceseqFinished,
                onlySnvFinished,
                onlyIndelFinished,
                onlySophiaFinished,
                onlyAceseqFinished,
        ].collect {
            [it.sampleType1BamFile, it.sampleType2BamFile]
        }.flatten().each {
            DomainFactory.createProcessingThresholdsForBamFile(it, [coverage: null, numberOfLanes: 1])
        }

        List<SamplePair> expectedSamplePairs = [
                finishedSamplePair,
        ]

        when:
        List<SamplePair> result = checker.handle(bamFiles, output)

        then:
        expectedSamplePairs == result

        1 * output.showWorkflow('Sample pairs')

        then:
        1 * output.showWorkflow('SnvWorkflow')
        1 * output.showWorkflow('IndelWorkflow')
        1 * output.showWorkflow('SophiaWorkflow')
        1 * output.showWorkflow('ACEseqWorkflow')
        0 * output.showWorkflow(_)
    }

}
