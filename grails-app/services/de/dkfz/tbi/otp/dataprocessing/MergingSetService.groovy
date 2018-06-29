package de.dkfz.tbi.otp.dataprocessing

class MergingSetService {

    MergingSet mergingSetInStateNeedsProcessing(short minPriority) {
        return MergingSet.createCriteria().get {
            eq ('status', MergingSet.State.NEEDS_PROCESSING)
            mergingWorkPackage {
                sample {
                    individual {
                        project {
                            ge('processingPriority', minPriority)
                            order("processingPriority", "desc")
                        }
                    }
                }
            }
            order("id", "asc")
            maxResults(1)
        }
    }
}
