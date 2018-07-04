package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.utils.*

class ProjectInfo implements Entity {

    String fileName
    Date dateCreated = new Date()

    static belongsTo = [project: Project]

    static constraints = {
        fileName(blank: false, unique: 'project', validator: { String val ->
            OtpPath.isValidPathComponent(val)
        })
    }

    static mapping = {
        fileName type: "text"
    }

    String getPath() {
        return "${project.getProjectDirectory().toString()}/${ProjectService.PROJECT_INFO}/${fileName}"
    }
}
