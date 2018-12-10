import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*

//Create manual SamplePairs defined by pid, sampleType1, sampleType2 and seqType:
//PID SAMPLETYPE1 SAMPLETYPE2 SEQTYPE_NAME
//the sample pair may not exist yet
List<List<MergingWorkPackage>> samplePairs = """


""".split('\n').findAll().collect {
    String[] split = it.split()
    println "parsed '${it}' into ${split as List}"

    Individual individual = CollectionUtils.exactlyOneElement(Individual.findAllByPid(split[0]))
    SampleType sampleType1 = CollectionUtils.exactlyOneElement(SampleType.findAllByName(split[1]))
    SampleType sampleType2 = CollectionUtils.exactlyOneElement(SampleType.findAllByName(split[2]))
    SeqType seqType = CollectionUtils.exactlyOneElement(SeqType.findAllByNameAndLibraryLayout(split[3], LibraryLayout.PAIRED))
    Sample sample1 = CollectionUtils.exactlyOneElement(Sample.findAllByIndividualAndSampleType(individual, sampleType1))
    Sample sample2 = CollectionUtils.exactlyOneElement(Sample.findAllByIndividualAndSampleType(individual, sampleType2))

    // find the merging work packages; use AbstractMWP so it also works with imported bamfiles (ExternalMWP)
    AbstractMergingWorkPackage mergingWorkPackage1 = CollectionUtils.exactlyOneElement(AbstractMergingWorkPackage.findAllBySampleAndSeqType(sample1, seqType))
    AbstractMergingWorkPackage mergingWorkPackage2 = CollectionUtils.exactlyOneElement(AbstractMergingWorkPackage.findAllBySampleAndSeqType(sample2, seqType))
    return [mergingWorkPackage1, mergingWorkPackage2]
}

Individual.withTransaction {
    samplePairs.each {
        AbstractMergingWorkPackage mergingWorkPackage1 = it[0]
        AbstractMergingWorkPackage mergingWorkPackage2 = it[1]

        SamplePair samplePair = SamplePair.createInstance([
                mergingWorkPackage1: mergingWorkPackage1,
                mergingWorkPackage2: mergingWorkPackage2,
        ])

        samplePair.save(flush: true)
    }
    assert false: "DEBUG: intentionally fail transaction"
}
''
