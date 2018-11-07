package de.dkfz.tbi.otp.dataprocessing.cellRanger

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeIndex

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

class CellRangerConfig extends ConfigPerProjectAndSeqType implements WithProgramVersion, AlignmentConfig {

    ReferenceGenomeIndex referenceGenomeIndex

    static constraints = {
        programVersion(blank: false)
        obsoleteDate validator: { val, obj ->
            if (!val) {
                CellRangerConfig cellRangerConfig = atMostOneElement(CellRangerConfig.findAllWhere(
                        project: obj.project,
                        seqType: obj.seqType,
                        pipeline: obj.pipeline,
                        obsoleteDate: null,
                ))
                !cellRangerConfig || cellRangerConfig == obj
            }
        }
    }

    @Override
    AlignmentInfo getAlignmentInformation() {
        return new SingleCellAlignmentInfo(alignmentProgram: "cellranger", alignmentParameter: "")
    }
}
