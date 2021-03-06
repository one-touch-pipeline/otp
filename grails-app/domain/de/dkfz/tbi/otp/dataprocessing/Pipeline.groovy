/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.dataprocessing

import groovy.transform.TupleConstructor

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.Entity

class Pipeline implements Entity {

    @TupleConstructor
    static enum Name {
        @Deprecated DEFAULT_OTP(Type.ALIGNMENT, false, 'bwa\u00A0aln', { SeqTypeService.defaultOtpAlignableSeqTypes }),
        PANCAN_ALIGNMENT       (Type.ALIGNMENT, true,  'bwa\u00A0mem', { SeqTypeService.panCanAlignableSeqTypes }),
        EXTERNALLY_PROCESSED   (Type.ALIGNMENT, false, 'external', { [] }),
        RODDY_RNA_ALIGNMENT    (Type.ALIGNMENT, true,  'STAR', { SeqTypeService.rnaAlignableSeqTypes }),
        CELL_RANGER            (Type.ALIGNMENT, false, 'cell ranger', { SeqTypeService.cellRangerAlignableSeqTypes }),
        @Deprecated OTP_SNV    (Type.SNV,       false, null, { SeqTypeService.snvPipelineSeqTypes }),
        RODDY_SNV              (Type.SNV,       true,  null, { SeqTypeService.snvPipelineSeqTypes }),
        RODDY_INDEL            (Type.INDEL,     true,  null, { SeqTypeService.indelPipelineSeqTypes }),
        RODDY_SOPHIA           (Type.SOPHIA, true, null, { SeqTypeService.sophiaPipelineSeqTypes }),
        RODDY_ACESEQ           (Type.ACESEQ, true, null, { SeqTypeService.aceseqPipelineSeqTypes }),
        RUN_YAPSA              (Type.MUTATIONAL_SIGNATURE, false, null, { SeqTypeService.runYapsaPipelineSeqTypes }),

        final Type type
        final boolean usesRoddy
        final String displayName
        final Closure<List<SeqType>> seqTypes

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
                case SeqTypeNames._10X_SCRNA:
                    return Name.CELL_RANGER
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

        List<SeqType> getSeqTypes() {
            return seqTypes()
        }

        static List<Name> getAlignmentPipelineNames() {
            return values().findAll {
                it.type == Type.ALIGNMENT
            }.findAll {
                !it.class.getField(it.name()).isAnnotationPresent(Deprecated)
            }.findAll {
                it != EXTERNALLY_PROCESSED
            }
        }
    }
    Name name

    static enum Type {
        ACESEQ,
        ALIGNMENT,
        INDEL,
        MUTATIONAL_SIGNATURE,
        SNV,
        SOPHIA,

        static Pipeline.Type findByName(String name) {
            return values().find {
                it.name() == name
            }
        }
    }
    Type type

    static constraints = {
        name unique: true
        type validator: { Type type, Pipeline pipeline -> type == pipeline?.name?.type }
    }

    boolean usesRoddy() {
        return name.usesRoddy
    }

    String getDisplayName() {
        return name.displayName
    }

    @Override
    String toString() {
        return "${name} ${type}"
    }

    final List<SeqType> getSeqTypes() {
        return name.getSeqTypes()
    }
}
