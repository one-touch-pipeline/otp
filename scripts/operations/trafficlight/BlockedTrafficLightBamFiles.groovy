import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*

/**
 * Script to show all blocked bam files for the given seq types.
 */

List<SeqType> seqTypes = []
seqTypes << SeqTypeService.wholeGenomePairedSeqType
seqTypes << SeqTypeService.exomePairedSeqType
seqTypes << SeqTypeService.wholeGenomeBisulfitePairedSeqType
seqTypes << SeqTypeService.wholeGenomeBisulfiteTagmentationPairedSeqType
seqTypes << SeqTypeService.rnaPairedSeqType
seqTypes << SeqTypeService.rnaSingleSeqType
seqTypes << SeqTypeService.'10xSingleCellRnaSeqType'

List<AbstractMergedBamFile> mergedBamFiles = AbstractMergedBamFile.createCriteria().list {
    eq('qcTrafficLightStatus', AbstractMergedBamFile.QcTrafficLightStatus.BLOCKED)
    workPackage {
        'in'('seqType', seqTypes)
    }
}.findAll {
    it.isMostRecentBamFile()
}

println mergedBamFiles.size()
println mergedBamFiles.sort {
    it.id
}.join('\n')

