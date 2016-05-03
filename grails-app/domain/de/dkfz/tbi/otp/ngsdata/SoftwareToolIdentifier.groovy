package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.Entity

class SoftwareToolIdentifier implements Entity {

    String name
    static belongsTo = [softwareTool : SoftwareTool]
    static constraints = {
        name()
    }

    static mapping = {
        softwareTool index: "software_tool_identifier_software_tool_idx"
    }
}
