package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.utils.Entity

class UserProjectRole implements Serializable, Entity {

    Project project
    User user
    ProjectRole projectRole
    boolean enabled = true
    boolean manageUsers = false

    boolean getManageUsers() {
        return manageUsers || projectRole.manageUsersAndDelegate
    }

    static mapping = {
        contactPerson index: "project_contact_person_contact_person_idx"
        project index: "project_contact_person_contact_person_idx"
    }
}
