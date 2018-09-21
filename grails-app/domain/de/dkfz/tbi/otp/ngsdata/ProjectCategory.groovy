package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.Entity

class ProjectCategory implements Entity {

    String name

    static hasMany = [
            projects: Project,
    ]
    static belongsTo = Project

    static constraints = {
        name(nullable: false, blank: false, unique: true)
    }

    @Override
    public String toString() {
        return name
    }
}
