package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.Entity

class ProjectGroup implements Entity {
    String name

    static constraints = {
        name(blank: false, unique: true)
    }

    @Override
    String toString() {
        return name
    }
}
