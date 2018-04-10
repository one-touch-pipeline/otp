package de.dkfz.tbi.otp.ngsdata

import org.springframework.security.access.prepost.*

class AntibodyTargetService extends MetadataFieldsService<AntibodyTarget> {

    @Override
    protected Class getClazz() {
        return AntibodyTarget
    }
}
