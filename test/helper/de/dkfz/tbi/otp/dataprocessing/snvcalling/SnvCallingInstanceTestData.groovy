package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.ExternalScript

class SnvCallingInstanceTestData extends TestData {

    ProcessedMergedBamFile bamFileTumor
    ProcessedMergedBamFile bamFileTumor2
    ProcessedMergedBamFile bamFileControl
    SampleTypeCombinationPerIndividual sampleTypeCombination
    SampleTypeCombinationPerIndividual sampleTypeCombination2
    SnvConfig snvConfig

    void createSnvObjects() {
        project = createProject()
        assert project.save(flush: true, failOnError: true)

        Individual individual = createIndividual([project: project])
        assert individual.save(flush: true, failOnError: true)

        SeqType seqType = createSeqType()
        assert seqType.save(flush: true, failOnError: true)

        bamFileTumor = createProcessedMergedBamFile(individual, seqType)
        bamFileTumor2 = createProcessedMergedBamFile(individual, seqType)
        bamFileControl = createProcessedMergedBamFile(individual, seqType)

        sampleTypeCombination = new SampleTypeCombinationPerIndividual(
                individual: individual,
                sampleType1: bamFileTumor.sampleType,
                sampleType2: bamFileControl.sampleType,
                seqType: seqType
                )
        assert sampleTypeCombination.save()

        sampleTypeCombination2 = new SampleTypeCombinationPerIndividual(
                individual: individual,
                sampleType1: bamFileTumor2.sampleType,
                sampleType2: bamFileControl.sampleType,
                seqType: seqType
                )
        assert sampleTypeCombination2.save()

        snvConfig = new SnvConfig(
                project: project,
                seqType: seqType,
                configuration: "testConfig"
        )
        assert snvConfig.save()
    }

    SnvJobResult createAndSaveSnvJobResult(SnvCallingInstance instance, SnvCallingStep step, SnvJobResult inputResult = null, SnvProcessingStates processingState = SnvProcessingStates.FINISHED, boolean withdrawn = false) {
        final ExternalScript externalScript = ExternalScript.findOrSaveWhere(
            scriptIdentifier: step.externalScriptIdentifier,
            deprecatedDate: null,
            filePath: "/dev/null/otp-test/${step.externalScriptIdentifier}",
            author: "otptest",
        )
        assert externalScript

        final SnvJobResult result = new SnvJobResult([
            snvCallingInstance: instance,
            step: step,
            inputResult: inputResult,
            processingState: processingState,
            withdrawn: withdrawn,
            externalScript: externalScript
        ])
        assert result.save(failOnError: true)
        return result
    }

    SnvCallingInstance createAndSaveSnvCallingInstance(Map properties = [:]) {
        final SnvCallingInstance instance = createSnvCallingInstance(properties)
        assert instance.save(failOnError: true)
        return instance
    }

    SnvCallingInstance createSnvCallingInstance(Map properties = [:]) {
        return new SnvCallingInstance([
            processingState: SnvProcessingStates.IN_PROGRESS,
            tumorBamFile: bamFileTumor,
            controlBamFile: bamFileControl,
            config: snvConfig,
            instanceName: "2014-08-25_15h32",
            sampleTypeCombination: sampleTypeCombination
        ] + properties)
    }


    ProcessedMergedBamFile createProcessedMergedBamFile(Individual individual, SeqType seqType) {
        SampleType sampleType = new SampleType(
                name: "sampleType${TestCase.uniqueString}")
        assert sampleType.save(flush: true, failOnError: true)

        Sample sample = new Sample (
                individual: individual,
                sampleType: sampleType)
        assert sample.save(flush: true, failOnError: true)

        MergingWorkPackage workPackage = new MergingWorkPackage(
                sample: sample,
                seqType: seqType
                )
        assert workPackage.save(flush: true, failOnError: true)

        MergingSet mergingSet = new MergingSet(
                mergingWorkPackage: workPackage)
        assert mergingSet.save(flush: true, failOnError: true)

        MergingPass mergingPass = new MergingPass(
                identifier: 1,
                mergingSet: mergingSet)
        assert mergingPass.save(flush: true, failOnError: true)

        ProcessedMergedBamFile bamFile = new ProcessedMergedBamFile(
                type: AbstractBamFile.BamType.SORTED,
                mergingPass: mergingPass)
        assert bamFile.save(flush: true, failOnError: true)

        return bamFile
    }
}
