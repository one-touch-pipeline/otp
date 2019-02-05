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

import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.utils.Entity

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

class SampleType implements Entity {

    /**
     * This enum specifies if the sample type belongs to a disease or a control.
     *
     * @see #getCategory(Project)
     */
    enum Category {
        /**
         * Sample types with their category configured as IGNORED should be silently ignored and not be processed by
         * workflows for which the category is relevant.
         * In contrast, if no category is configured (i.e. no {@link SampleTypePerProject} instance exists for a
         * combination of project and sample type), such workflows should warn about the sample type category being
         * unknown.
         */
        UNDEFINED {
            @Override
            Category correspondingCategory() {
                return null
            }
        },
        DISEASE {
            @Override
            Category correspondingCategory() {
                return CONTROL
            }
        },
        CONTROL {
            @Override
            Category correspondingCategory() {
                return DISEASE
            }
        },
        IGNORED {
            @Override
            Category correspondingCategory() {
                return null
            }
        },

        abstract Category correspondingCategory()
    }

    /**
     * enum to define, if the {@link Project} default or a {@link SampleType}-specific {@link ReferenceGenome} should be used.
     */
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

    String name

    SpecificReferenceGenome specificReferenceGenome = SpecificReferenceGenome.UNKNOWN

    static constraints = {
        name(unique: true, validator: { OtpPath.isValidPathComponent(it) && !it.contains('_') })
        // TODO: OTP-1122: unique constraint for dirName
    }

    String getDirName() {
        return name.toLowerCase()
    }

    String getDisplayName() {
        return name
    }

    /**
     * @return The category of this sample type or <code>null</code> if it is not configured.
     */
    Category getCategory(final Project project) {
        assert project
        return atMostOneElement(SampleTypePerProject.findAllWhere(project: project, sampleType: this))?.category
    }

    @Override
    String toString() {
        return name
    }
}
