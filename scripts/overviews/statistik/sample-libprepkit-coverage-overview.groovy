import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*

def d = RoddyBamFile.createCriteria().list {
    workPackage {
        eq('seqType', SeqType.exomePairedSeqType)
    }
}.findAll {
    it.fileOperationStatus == AbstractMergedBamFile.FileOperationStatus.PROCESSED && it.isMostRecentBamFile()
}.collect {
    [it.project, it.individual, it.sampleType.name, it.seqType.displayName, it.seqType.libraryLayout, it.mergingWorkPackage.libraryPreparationKit.name, it.coverage].join(',')
}.sort()

println d.join('\n')
