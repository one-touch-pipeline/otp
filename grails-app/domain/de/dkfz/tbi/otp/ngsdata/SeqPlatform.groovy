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
package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.MergingCriteria
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.Entity
import de.dkfz.tbi.otp.utils.Legacy

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

/** This table is used externally. Please discuss a change in the team */
class SeqPlatform implements Entity, Legacy {

    /** This attribute is used externally. Please discuss a change in the team */
    String name   // eg. solid, illumina
    SeqPlatformModelLabel seqPlatformModelLabel
    SequencingKitLabel sequencingKitLabel

    static hasMany = [
            seqPlatformGroups: SeqPlatformGroup,
    ]

    static constraints = {
        /*
        The GORM unique constraint is seemingly bugged and does not recognize objects with different attributes as unique
        name(blank: false, unique: ['seqPlatformModelLabel', 'sequencingKitLabel'])
        Because of this we implemented the same constraint in a custom validator:
         */
        name(blank: false, validator: { val, obj ->
            SeqPlatform seqPlatform = SeqPlatform.findByNameAndSeqPlatformModelLabelAndSequencingKitLabel(val, obj.seqPlatformModelLabel, obj.sequencingKitLabel)
            return !seqPlatform || seqPlatform == obj
        })
        seqPlatformModelLabel(nullable: true)
        sequencingKitLabel(nullable: true)
    }

    @Override
    String toString() {
        return fullName()
    }

    SeqPlatformGroup getSeqPlatformGroupForMergingCriteria(Project project, SeqType seqType) {
        if (!(project && seqType)) {
            return null
        }

        MergingCriteria mergingCriteria = MergingCriteria.findByProjectAndSeqType(project, seqType)

        if (!mergingCriteria) {
            return null
        }

        List<SeqPlatformGroup> seqPlatformGroups = SeqPlatformGroup.withCriteria {
            seqPlatforms {
                eq("id", this.id)
            }
            if (mergingCriteria.useSeqPlatformGroup == MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT) {
                isNull("mergingCriteria")
            } else {
                eq("mergingCriteria", mergingCriteria)
            }
        }
        return atMostOneElement(seqPlatformGroups)
    }

    String fullName() {
        return [
                name,
                seqPlatformModelLabel?.name,
                sequencingKitLabel?.name ?: 'unknown kit',
        ].findAll().join(' ')
    }

    static mapping = {
        sequencingKitLabel index: "seq_platform_sequencing_kit_label_idx"
        seqPlatformModelLabel index: "seq_platform_seq_platform_model_label_idx"
    }
}
