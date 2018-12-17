package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.Entity

class TumorEntity implements Entity {

    String name

    static constraints = {
        name unique: true
    }

    static hasMany = [
            projects: Project,
    ]

    @Override
    String toString() {
        return name
    }
}
