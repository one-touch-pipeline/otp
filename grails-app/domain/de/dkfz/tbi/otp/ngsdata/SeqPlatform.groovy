package de.dkfz.tbi.otp.ngsdata

class SeqPlatform {

    String name   // eg. solid, illumina
    String model

    /**
     * If {@code null}, data from this {@link SeqPlatform} will not be aligned.
     */
    SeqPlatformGroup seqPlatformGroup

    static constraints = {
        name(blank: false)
        model(nullable: true, unique: 'name')
        seqPlatformGroup(nullable: true)
    }

    String toString() {
        final int expressiveModelNameLimit = 4
        if (!model) {
            return name
        }
        if (model.size() > expressiveModelNameLimit) {
            return model
        }
        return name + " " + model
    }

    String fullName() {
        if (model) {
            return name + " " + model
        }
        return name
    }
}
