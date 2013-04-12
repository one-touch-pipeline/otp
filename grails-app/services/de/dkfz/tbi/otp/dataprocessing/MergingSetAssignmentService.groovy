package de.dkfz.tbi.otp.dataprocessing

class MergingSetAssignmentService {

    List<MergingSetAssignment> findByMergingSet(MergingSet mergingSet) {
        return MergingSetAssignment.findAllByMergingSet(mergingSet)
    }
}
