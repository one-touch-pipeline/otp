package de.dkfz.tbi.otp.ngsdata

class ProjectGroup {
    String name

    static constraints = {
        name(blank: false, unique: true)
    }

    public String toString() {
        return name
    }
}
