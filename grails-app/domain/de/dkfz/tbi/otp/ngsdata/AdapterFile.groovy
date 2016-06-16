package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.utils.*

class AdapterFile implements Entity {


    String fileName


    static constraints = {
        fileName blank: false, unique: true, validator: { val -> !val || OtpPath.isValidPathComponent(val) }
    }
}
