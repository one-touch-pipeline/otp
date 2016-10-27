package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.Entity

class ProjectContactPerson implements Serializable, Entity {

    Project project
    ContactPerson contactPerson
    ContactPersonRole contactPersonRole

    static constraints = {
        contactPersonRole(nullable: true)
    }
}
