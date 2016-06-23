package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.utils.Entity

class Pipeline implements Entity {

    enum Name {
        DEFAULT_OTP("bwa&nbsp;aln"),
        PANCAN_ALIGNMENT("bwa&nbsp;mem")

        final String html

        private Name(String html) {
            this.html = html
        }
    }
    Name name

    enum Type {
        ALIGNMENT
    }
    Type type

    static constraints = {
        name unique: 'type'
    }

    public getHtml() {
        return name.html
    }

    @Override
    String toString() {
        return "${name} ${type}"
    }

}
