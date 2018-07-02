package de.dkfz.tbi.otp.dataprocessing.runYapsa

import de.dkfz.tbi.otp.dataprocessing.*

class RunYapsaConfig extends ConfigPerProject {

    String programVersion

    static constraints = {
        programVersion (blank: false)
    }
}
