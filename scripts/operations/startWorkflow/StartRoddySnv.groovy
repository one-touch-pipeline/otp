package operations.startWorkflow

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.jobs.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*

//sample pair defined by:
//PID SAMPLETYPE1 SAMPLETYPE2 SEQTYPE_ALIAS
//the sample pair has to be created already by OTP, the SAMPLETYPE1 has to be a disease sample type, the SAMPLETYPE2 is usually control
// for example: H000-ABCD TUMOR02 BLOOD WGS
List<SamplePair> samplePairs = """


""".split('\n').findAll().collect {
    println "check: '${it}'"
    String[] split = it.split(' ')
    println split as List
    Individual individual = CollectionUtils.exactlyOneElement(Individual.findAllByPid(split[0]))
    SampleType sampleType1 = CollectionUtils.exactlyOneElement(SampleType.findAllByName(split[1]))
    SampleType sampleType2 = CollectionUtils.exactlyOneElement(SampleType.findAllByName(split[2]))
    SeqType seqType = CollectionUtils.exactlyOneElement(SeqType.findAllByAliasAndLibraryLayout(split[3], SeqType.LIBRARYLAYOUT_PAIRED))
    Sample sample1 = CollectionUtils.exactlyOneElement(Sample.findAllByIndividualAndSampleType(individual, sampleType1))
    Sample sample2 = CollectionUtils.exactlyOneElement(Sample.findAllByIndividualAndSampleType(individual, sampleType2))
    MergingWorkPackage mergingWorkPackage1 = CollectionUtils.exactlyOneElement(MergingWorkPackage.findAllBySampleAndSeqType(sample1, seqType))
    MergingWorkPackage mergingWorkPackage2 = CollectionUtils.exactlyOneElement(MergingWorkPackage.findAllBySampleAndSeqType(sample2, seqType))
    SamplePair samplePair = CollectionUtils.exactlyOneElement(SamplePair.findAllByMergingWorkPackage1AndMergingWorkPackage2(mergingWorkPackage1, mergingWorkPackage2))
    return samplePair
}


RoddySnvCallingStartJob roddySnvCallingStartJob = ctx.roddySnvStartJob


SamplePair.withTransaction {
    samplePairs.each { SamplePair samplePair ->
        println "trigger: '${it}'"

        ConfigPerProject config = roddySnvCallingStartJob.getConfig(samplePair)

        AbstractMergedBamFile sampleType1BamFile = samplePair.mergingWorkPackage1.processableBamFileInProjectFolder
        AbstractMergedBamFile sampleType2BamFile = samplePair.mergingWorkPackage2.processableBamFileInProjectFolder

        BamFilePairAnalysis analysis = new RoddySnvCallingInstance(
                samplePair: samplePair,
                instanceName: roddySnvCallingStartJob.getInstanceName(config),
                config: config,
                sampleType1BamFile: sampleType1BamFile,
                sampleType2BamFile: sampleType2BamFile,
                latestDataFileCreationDate: AbstractBamFile.getLatestSequenceDataFileCreationDate(sampleType1BamFile, sampleType2BamFile),
        )
        analysis.save(flush: true)
        roddySnvCallingStartJob.prepareCreatingTheProcessAndTriggerTracking(analysis)
        roddySnvCallingStartJob.createProcess(analysis)
        log.debug "Analysis started for: ${analysis.toString()}"
    }
    assert false
}
