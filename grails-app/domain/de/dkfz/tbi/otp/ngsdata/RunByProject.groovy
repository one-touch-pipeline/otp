package de.dkfz.tbi.otp.ngsdata

class RunByProject {
    Project project
    Run run

    static constraints = {
        project(nullable: true)
        run(nullable: true)
    }
}
