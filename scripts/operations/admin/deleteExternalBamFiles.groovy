import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.utils.CollectionUtils

List<String> pidsToDelete = [
        'pid1',
        'pid2',
        'pid3',
        'pid4',
]


def bamFiles = ExternallyProcessedMergedBamFile.createCriteria().listDistinct {
    fastqSet {
        seqTracks {
            sample {
                individual {
                    'in' ('pid', pidsToDelete)
                }
            }
        }
    }
}

assert CollectionUtils.containSame(bamFiles*.individual*.pid.unique(), pidsToDelete)

ExternallyProcessedMergedBamFile.withTransaction {
    bamFiles.each {
        FastqSet set = it.fastqSet
        it.delete()
        set.delete()
        println "${it} deleted"
    }
    it.flush()
    assert false
}
