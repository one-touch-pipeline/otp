package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*


class MergingPassService {

    ConfigService configService

    MergingSetService mergingSetService

    MergingPass create() {
        MergingSet mergingSet = mergingSetService.nextMergingSet()
        if (mergingSet) {
            int pass = MergingPass.countByMergingSet(mergingSet)
            MergingPass mergingPass = new MergingPass(identifier: pass, mergingSet: mergingSet)
            assertSave(mergingPass)
            log.debug("created a new mergingPass ${mergingPass} for mergingSet ${mergingSet}")
            return mergingPass
        }
        return null
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

    public Realm realmForDataProcessing(MergingPass mergingPass) {
        return configService.getRealmDataProcessing(project(mergingPass))
    }

    public Project project(MergingPass mergingPass) {
        return mergingPass.mergingSet.mergingWorkPackage.sample.individual.project
    }

    public void mergingPassFinished(MergingPass mergingPass) {
        mergingPass.status = MergingPass.State.SUCCEED
        assertSave(mergingPass)
        mergingSetService.mergingSetFinished(mergingPass.mergingSet)
    }

}
