/*
 * Copyright 2011-2024 The OTP authors
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

import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.Entity
import de.dkfz.tbi.otp.utils.Legacy
import de.dkfz.tbi.otp.utils.validation.OtpPathValidator

/** This table is used externally. Please discuss a change in the team */
@ManagedEntity
class SampleType implements Entity, Legacy {

    /**
     * enum to define, if the {@link Project} default or a {@link SampleType}-specific {@link ReferenceGenome} should be used.
     */
    @Deprecated
    enum SpecificReferenceGenome {
        /**
         * For this {@link SampleType} the {@link Project} {@link SeqType} default {@link ReferenceGenome} should be used.
         */
        USE_PROJECT_DEFAULT,
        /**
         * For this {@link SampleType} the {@link Project} {@link SeqType} {@link SampleType} specific {@link ReferenceGenome} should be used.
         */
        USE_SAMPLE_TYPE_SPECIFIC,
        /**
         * For this {@link SampleType} it is not defined yet. It should be used only for automatically created {@link SampleType}s
         * and needs to be changed later.
         */
        UNKNOWN
    }

    /** This attribute is used externally. Please discuss a change in the team */
    String name

    @Deprecated
    SpecificReferenceGenome specificReferenceGenome = SpecificReferenceGenome.UNKNOWN

    static constraints = {
        name(unique: true, blank: false, validator: { val, obj ->
            if (val == null) {
                return true // case checked as nullable constraint
            }
            if (obj.name != obj.name.toLowerCase()) {
                return 'validator.obj.name.toLowerCase'
            }
            if (!OtpPathValidator.isValidPathComponent(val)) {
                return 'validator.path.component'
            }
            if (!obj.id && val.contains('_')) {
                // Since roddy has problems with underscores in name of SampleTypes, it should not be allowed for new objects.
                // But for legacy reasons the underscore should be allowed for already existing objects
                return 'underscore'
            }
        })
    }

    static mapping = {
        name index: "sample_type_name_idx"
    }

    String getDirName() {
        return name.toLowerCase()
    }

    String getDisplayName() {
        return name
    }

    @Override
    String toString() {
        return name
    }
}
