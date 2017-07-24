import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*

/**
 * Create manual a Sample Pair for the given pid, seqType, sampleType1 and sampleType2
 */

String pid = ''
String sampleType1Name = ''
String sampleType2Name = ''
SeqType seqType
//seqType = SeqType.exomePairedSeqType
//seqType = SeqType.wholeGenomePairedSeqType

//---------------------------------------------------------------------------

assert pid
assert seqType
assert sampleType1Name
assert sampleType2Name

Individual.withTransaction {

    Individual individual = CollectionUtils.exactlyOneElement(Individual.findAllByPid(pid))
    SampleType sampleType1 = CollectionUtils.exactlyOneElement(SampleType.findAllByName(sampleType1Name))
    SampleType sampleType2 = CollectionUtils.exactlyOneElement(SampleType.findAllByName(sampleType2Name))

    Sample sample1 = CollectionUtils.exactlyOneElement(Sample.findAllByIndividualAndSampleType(individual, sampleType1))
    Sample sample2 = CollectionUtils.exactlyOneElement(Sample.findAllByIndividualAndSampleType(individual, sampleType2))

    MergingWorkPackage mergingWorkPackage1 = CollectionUtils.exactlyOneElement(MergingWorkPackage.findAllBySampleAndSeqType(sample1, seqType))
    MergingWorkPackage mergingWorkPackage2 = CollectionUtils.exactlyOneElement(MergingWorkPackage.findAllBySampleAndSeqType(sample2, seqType))

    SamplePair samplePair = SamplePair.createInstance([
            mergingWorkPackage1: mergingWorkPackage1,
            mergingWorkPackage2: mergingWorkPackage2,
    ])

    samplePair.save(flush: true)

    assert false
}
