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

import grails.gorm.hibernate.annotation.ManagedEntity

import de.dkfz.tbi.otp.Comment
import de.dkfz.tbi.otp.CommentableWithHistory
import de.dkfz.tbi.otp.dataprocessing.MergingCriteria
import de.dkfz.tbi.otp.utils.Entity
import de.dkfz.tbi.otp.utils.validation.ValidatorUtil

/**
 * {@link SeqTrack}s from {@link SeqPlatform}s in the same {@link SeqPlatformGroup} can be merged.
 * Depending on the value of {@link MergingCriteria#useSeqPlatformGroup}, a SeqPlatformGroup where {@link #mergingCriteria}
 * is null or refers to the project and seqType is used
 */
@ManagedEntity
class SeqPlatformGroup implements Entity, CommentableWithHistory {

    Set<SeqPlatform> seqPlatforms
    MergingCriteria mergingCriteria

    List<Comment> comments = []

    static hasMany = [
            seqPlatforms: SeqPlatform,
            comments: Comment,
    ]

    static belongsTo = SeqPlatform

    static constraints = {
        mergingCriteria nullable: true, validator: { MergingCriteria mergingCriteria1 ->
            if (mergingCriteria1) {
                return mergingCriteria1.useSeqPlatformGroup == MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC
            }
            return true
        }
        seqPlatforms validator: ValidatorUtil.messageArgs("seqPlatforms") { Set<SeqPlatform> seqPlatforms1, SeqPlatformGroup seqPlatformGroup ->
            seqPlatforms1.each { SeqPlatform seqPlatform ->
                List<SeqPlatformGroup> l = withCriteria {
                    seqPlatforms {
                        eq("id", seqPlatform.id)
                    }
                    if (seqPlatformGroup.mergingCriteria == null) {
                        isNull("mergingCriteria")
                    } else {
                        eq("mergingCriteria", seqPlatformGroup.mergingCriteria)
                    }
                    if (seqPlatformGroup.id != null) {
                        ne("id", seqPlatformGroup.id)
                    }
                }
                if (l.size() > 0) {
                    rejectValue('invalid', [seqPlatformGroup.mergingCriteria])
                }
            }
            return
        }
        comments nullable: true
    }

    static Closure mapping = {
        mergingCriteria index: "seq_platform_group_merging_criteria_idx"
    }

    @Override
    String toString() {
        StringBuilder sb = new StringBuilder()
        if (mergingCriteria) {
            sb << "'${mergingCriteria.project} ${mergingCriteria.seqType?.displayNameWithLibraryLayout}'"
        } else {
            sb << "'OTP global'"
        }
        sb << " seq platform group with "
        if (seqPlatforms) {
            sb << seqPlatforms*.toString().sort().collect {
                "'${it}'"
            }.join(", ")
        } else {
            sb << " no seq platforms"
        }
        return sb.toString()
    }
}
