package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.*

class TumorEntity implements Entity {

    String name

    static constraints = {
        name unique: true
    }

    static hasMany = [
            projects: Project
    ]

    public String toString() {
        return name
    }
}
