package de.dkfz.tbi.otp.dataprocessing.runYapsa

import de.dkfz.tbi.otp.dataprocessing.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

class RunYapsaConfig extends ConfigPerProjectAndSeqType {

    String programVersion

    static constraints = {
        programVersion(blank: false)
        obsoleteDate validator: { val, obj ->
            if (!val) {
                RunYapsaConfig runYapsaConfig = atMostOneElement(RunYapsaConfig.findAllWhere(
                        project: obj.project,
                        seqType: obj.seqType,
                        pipeline: obj.pipeline,
                        obsoleteDate: null,
                ))
                !runYapsaConfig || runYapsaConfig == obj
            }
        }
    }
}
