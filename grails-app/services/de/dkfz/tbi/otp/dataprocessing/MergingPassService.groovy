package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*


class MergingPassService {

    void create(MergingSet mergingSet) {
        MergingPass mergingPass = new MergingPass(identifier: TODO, MergingSet: mergingSet)
        assertSave(mergingPass)
        log.debug("created a new mergingPass ${mergingPass} for mergingSet ${mergingSet}")
    }

    // TODO: discuss to move this method to some generic service,
    // to be DRY
    private def assertSave(def object) {
        object = object.save(flush: true)
        if (!object) {
            throw new SavingException(object.toString())
        }
        return object
    }
}
