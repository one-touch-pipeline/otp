import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*


/**
 * Script to update reference genome for one MergingWorkPackage
 */


String referenceGenomeName = '' //Name of the new Reference Genome
String pid = ''
String sampleTypeName = ''
SeqType seqType

//seqType = SeqTypeService.exomePairedSeqType
//seqType = SeqTypeService.wholeGenomePairedSeqType
//seqType = SeqTypeService.wholeGenomeBisulfitePairedSeqType
//seqType = SeqTypeService.wholeGenomeBisulfiteTagmentationPairedSeqType
//seqType = SeqTypeService.rnaPairedSeqType
//seqType = SeqTypeService.chipSeqPairedSeqType

//--------------------------

assert referenceGenomeName
assert pid
assert sampleTypeName
assert seqType


ReferenceGenome referenceGenome = CollectionUtils.exactlyOneElement(ReferenceGenome.findAllByName(referenceGenomeName))

Individual individual = CollectionUtils.exactlyOneElement(Individual.findAllByPid(pid))
SampleType sampleType = CollectionUtils.exactlyOneElement(SampleType.findAllByName(sampleTypeName))
Sample sample = CollectionUtils.exactlyOneElement(Sample.findAllByIndividualAndSampleType(individual, sampleType))
MergingWorkPackage mergingWorkPackage = CollectionUtils.exactlyOneElement(MergingWorkPackage.findAllBySampleAndSeqType(sample, seqType))


SeqTrack.withTransaction {
    mergingWorkPackage.referenceGenome = referenceGenome
    mergingWorkPackage.save(flush: true)

    assert false
}
