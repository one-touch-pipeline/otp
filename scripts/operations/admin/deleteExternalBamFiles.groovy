import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.utils.CollectionUtils

List<String> pidsToDelete = [
        'pid1',
        'pid2',
        'pid3',
        'pid4',
]


def bamFiles = ExternallyProcessedMergedBamFile.createCriteria().listDistinct {
    workPackage {
        sample {
            individual {
                'in'('pid', pidsToDelete)
            }
        }
    }
}

assert CollectionUtils.containSame(bamFiles*.individual*.pid.unique(), pidsToDelete)

ExternallyProcessedMergedBamFile.withTransaction {
    bamFiles.each {
        ExternalMergingWorkPackage workPackage = it.workPackage
        it.delete()
        workPackage.delete()
        println "${it} deleted"
    }
    it.flush()
    assert false
}
