package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import groovy.transform.*

class Pipeline implements Entity {

    @TupleConstructor
    static enum Name {
        DEFAULT_OTP         (Type.ALIGNMENT, false, 'bwa\u00A0aln'),
        PANCAN_ALIGNMENT    (Type.ALIGNMENT, true,  'bwa\u00A0mem'),
        EXTERNALLY_PROCESSED(Type.ALIGNMENT, false, 'external'),
        @Deprecated OTP_SNV (Type.SNV,       false, null),
        RODDY_SNV           (Type.SNV,       true,  null),
        RODDY_INDEL         (Type.INDEL,     true,  null),
        RODDY_RNA_ALIGNMENT (Type.ALIGNMENT, true, 'STAR'),
        RODDY_SOPHIA        (Type.SOPHIA, true, null),
        RODDY_ACESEQ        (Type.ACESEQ, true, null),

        final Type type
        final boolean usesRoddy
        final String displayName

        static Name forSeqType(SeqType seqType) {
            assert seqType
            switch (SeqTypeNames.fromSeqTypeName(seqType.name)) {
                case SeqTypeNames.EXOME:
                case SeqTypeNames.WHOLE_GENOME:
                case SeqTypeNames.WHOLE_GENOME_BISULFITE:
                case SeqTypeNames.WHOLE_GENOME_BISULFITE_TAGMENTATION:
                case SeqTypeNames.CHIP_SEQ:
                    return Name.PANCAN_ALIGNMENT
                case SeqTypeNames.RNA:
                    return Name.RODDY_RNA_ALIGNMENT
                default:
                    return null
            }
        }

        Pipeline getPipeline() {
            CollectionUtils.exactlyOneElement(Pipeline.findAllByTypeAndName(
                    type,
                    this,
            ))
        }
    }
    Name name

    static enum Type {
        ALIGNMENT,
        SNV,
        INDEL,
        SOPHIA,
        ACESEQ,
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
