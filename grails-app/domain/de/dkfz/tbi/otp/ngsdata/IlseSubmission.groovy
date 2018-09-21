package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.utils.*

class IlseSubmission implements Entity, Commentable {

    int ilseNumber

    boolean warning = false

    Comment comment

    static constraints = {
        ilseNumber unique: true, min: 1000, max: 999999
        comment nullable: true, validator: { comment, ilseSubmission ->
            if (ilseSubmission.warning) {
                if (!comment?.comment) {
                    return 'a comment need to be provided'
                }
            }
        }
    }

    @Override
    Project getProject()  {
        return null
    }
}
