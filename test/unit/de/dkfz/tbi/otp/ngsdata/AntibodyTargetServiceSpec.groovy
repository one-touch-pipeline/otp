package de.dkfz.tbi.otp.ngsdata

import grails.test.mixin.Mock

@Mock([
        AntibodyTarget,
])
class AntibodyTargetServiceSpec extends MetadataFieldsServiceSpec<AntibodyTarget> {
    AntibodyTargetService antibodyTargetService = new AntibodyTargetService()

    protected MetadataFieldsService getService() {
        return antibodyTargetService
    }
}
