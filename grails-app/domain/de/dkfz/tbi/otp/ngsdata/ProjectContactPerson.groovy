package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.Entity

class ProjectContactPerson implements Serializable, Entity {

    Project project
    ContactPerson contactPerson
    ContactPersonRole contactPersonRole

    static constraints = {
        contactPersonRole(nullable: true)
    }

    static mapping = {
        contactPerson index: "project_contact_person_contact_person_idx"
        project index: "project_contact_person_contact_person_idx"
    }
}
