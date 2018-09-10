package de.dkfz.tbi.otp.dataprocessing.singleCell

import de.dkfz.tbi.otp.dataprocessing.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

class SingleCellConfig extends ConfigPerProjectAndSeqType implements WithProgramVersion, AlignmentConfig {

    static constraints = {
        programVersion(blank: false)
        obsoleteDate validator: { val, obj ->
            if (!val) {
                SingleCellConfig singleCellConfig = atMostOneElement(SingleCellConfig.findAllWhere(
                        project: obj.project,
                        seqType: obj.seqType,
                        pipeline: obj.pipeline,
                        obsoleteDate: null,
                ))
                !singleCellConfig || singleCellConfig == obj
            }
        }
    }
}
