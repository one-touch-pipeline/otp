package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.utils.Entity

class MergingWorkPackageAlignmentProperty implements Entity {

    MergingWorkPackage mergingWorkPackage

    String name

    String value


    static belongsTo = [
            mergingWorkPackage: MergingWorkPackage,
    ]

    static constraints = {
        name blank: false, unique: ['mergingWorkPackage']
        value blank: false, maxSize: 500
    }
}
