package operations.startWorkflow

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.jobs.indelCalling.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*

//sample pair defined by:
//PID SAMPLETYPE1 SAMPLETYPE2 SEQTYPE_NAME
//the sample pair has to be created already by OTP, the SAMPLETYPE1 has to be a disease sample type, the SAMPLETYPE2 is usually control
List<SamplePair> samplePairs = """


""".split('\n').findAll().collect {
    println "check: '${it}'"
    String[] split = it.split(' ')
    println split as List
    Individual individual = CollectionUtils.exactlyOneElement(Individual.findAllByPid(split[0]))
    SampleType sampleType1 = CollectionUtils.exactlyOneElement(SampleType.findAllByName(split[1]))
    SampleType sampleType2 = CollectionUtils.exactlyOneElement(SampleType.findAllByName(split[2]))
    SeqType seqType = CollectionUtils.exactlyOneElement(SeqType.findAllByNameAndLibraryLayout(split[3], SeqType.LIBRARYLAYOUT_PAIRED))
    Sample sample1 = CollectionUtils.exactlyOneElement(Sample.findAllByIndividualAndSampleType(individual, sampleType1))
    Sample sample2 = CollectionUtils.exactlyOneElement(Sample.findAllByIndividualAndSampleType(individual, sampleType2))
    MergingWorkPackage mergingWorkPackage1 = CollectionUtils.exactlyOneElement(MergingWorkPackage.findAllBySampleAndSeqType(sample1, seqType))
    MergingWorkPackage mergingWorkPackage2 = CollectionUtils.exactlyOneElement(MergingWorkPackage.findAllBySampleAndSeqType(sample2, seqType))
    SamplePair samplePair = CollectionUtils.exactlyOneElement(SamplePair.findAllByMergingWorkPackage1AndMergingWorkPackage2(mergingWorkPackage1, mergingWorkPackage2))
    return samplePair
}


RoddyIndelCallingStartJob roddyIndelCallingStartJob = ctx.roddyIndelStartJob


SamplePair.withTransaction {
    samplePairs.each { SamplePair samplePair ->
        println "trigger: '${it}'"

        ConfigPerProject config = roddyIndelCallingStartJob.getConfig(samplePair)

        AbstractMergedBamFile sampleType1BamFile = samplePair.mergingWorkPackage1.processableBamFileInProjectFolder
        AbstractMergedBamFile sampleType2BamFile = samplePair.mergingWorkPackage2.processableBamFileInProjectFolder

        BamFilePairAnalysis analysis = new IndelCallingInstance(
                samplePair: samplePair,
                instanceName: roddyIndelCallingStartJob.getInstanceName(config),
                config: config,
                sampleType1BamFile: sampleType1BamFile,
                sampleType2BamFile: sampleType2BamFile,
        )
        analysis.save(flush: true)
        roddyIndelCallingStartJob.prepareCreatingTheProcessAndTriggerTracking(analysis)
        roddyIndelCallingStartJob.createProcess(analysis)
        log.debug "Analysis started for: ${analysis.toString()}"
    }
}
