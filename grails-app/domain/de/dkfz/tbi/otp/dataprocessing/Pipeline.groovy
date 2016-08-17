package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.utils.Entity

class Pipeline implements Entity {

    enum Name {
        DEFAULT_OTP("bwa&nbsp;aln"),
        PANCAN_ALIGNMENT("bwa&nbsp;mem"),
        OTP_SNV(""),
        RODDY_SNV(""),


        final String html

        private Name(String html) {
            this.html = html
        }
    }
    Name name

    enum Type {
        ALIGNMENT,
        SNV,
    }
    Type type

    static constraints = {
        name unique: 'type', validator: { name, pipeline ->
            switch (name) {
                case [Name.DEFAULT_OTP, Name.PANCAN_ALIGNMENT]:
                    return pipeline.type == Type.ALIGNMENT
                case [Name.OTP_SNV, Name.RODDY_SNV]:
                    return pipeline.type == Type.SNV
                default:
                    assert false : "The pipeline ${name} is not defined."
            }
        }
    }

    public getHtml() {
        return name.html
    }

    @Override
    String toString() {
        return "${name} ${type}"
    }

}
