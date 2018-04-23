package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.utils.Entity

class UserProjectRole implements Serializable, Entity {

    Project project
    User user
    ProjectRole projectRole
    boolean enabled = true
    boolean manageUsers = false

    static constraints = {
        user(unique: ['project'])
    }

    boolean getManageUsers() {
        return manageUsers || projectRole.manageUsersAndDelegate
    }
}
