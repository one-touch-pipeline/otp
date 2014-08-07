package de.dkfz.tbi.otp.ngsdata

class Project {
    String name
    String dirName
    String realmName

    String emailAddressOfContactPerson

    static belongsTo = [
        projectGroup: ProjectGroup
    ]

    static constraints = {
        name(blank: false, unique: true)
        dirName(blank: false)
        realmName(blank: false)
        projectGroup(nullable: true)
        emailAddressOfContactPerson (nullable: true)
    }

    String toString() {
        name
    }

    static mapping = {
        projectGroup index: "project_project_group_idx"
    }
}
