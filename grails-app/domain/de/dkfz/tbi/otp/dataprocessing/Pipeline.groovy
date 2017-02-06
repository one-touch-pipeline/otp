package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.utils.*
import groovy.transform.*

class Pipeline implements Entity {

    @TupleConstructor
    static enum Name {
        DEFAULT_OTP     (Type.ALIGNMENT, false, 'bwa\u00A0aln'),
        PANCAN_ALIGNMENT(Type.ALIGNMENT, true,  'bwa\u00A0mem'),
        EXTERNALLY_PROCESSED(Type.ALIGNMENT, false, 'external'),
        OTP_SNV         (Type.SNV,       false, null),
        RODDY_SNV       (Type.SNV,       true,  null),
        RODDY_INDEL     (Type.INDEL,     true,  null),
        RODDY_RNA_ALIGNMENT (Type.ALIGNMENT, true, 'STAR'),

        final Type type
        final boolean usesRoddy
        final String displayName
    }
    Name name

    static enum Type {
        ALIGNMENT,
        SNV,
        INDEL,
    }
    Type type

    static constraints = {
        name unique: true
        type validator: { Type type, Pipeline pipeline -> type == pipeline?.name?.type }
    }

    public boolean usesRoddy() {
        return name.usesRoddy
    }

    public getDisplayName() {
        return name.displayName
    }

    @Override
    String toString() {
        return "${name} ${type}"
    }

}
