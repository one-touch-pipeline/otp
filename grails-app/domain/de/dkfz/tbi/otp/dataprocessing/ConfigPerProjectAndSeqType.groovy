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

import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.utils.Entity

/**
 * To be more flexible the configuration shall be stored in the database instead of in the code.
 * This domain stores the configuration project specific.
 * If the configuration changes, the old database entry is set to obsolete and the new entry refers to the old entry.
 */
abstract class ConfigPerProjectAndSeqType implements Entity {

    Project project

    SeqType seqType

    static belongsTo = [
            project: Project,
            seqType: SeqType,
    ]

    Pipeline pipeline

    /**
     * When changes appear in the configuration, a new ConfigPerProjectAndSeqType entry is created and the old entry is set to obsolete.
     */
    Date obsoleteDate

    /**
     * The version of the external program.
     *
     * For Roddy configurations this contains the plugin version in the Roddy expected naming
     * format, e.g.: IndelCallingWorkflow:1.0.176-8 and is used for the parameter --usePluginVersion
     */
    String programVersion

    /**
     * When a previous config files exists, it should be referred here.
     * This is needed for tracking.
     */
    ConfigPerProjectAndSeqType previousConfig

    static constraints = {
        seqType nullable: true, //needs to be nullable because of old data, should never be null for new data
                validator: { val, obj ->
                    obj.obsoleteDate ? true : val != null
                }
        obsoleteDate nullable: true
        programVersion blank: false
        previousConfig nullable: true, validator: { val, obj ->
            return (val == null || val != null && val.obsoleteDate != null)
        }
    }

    static mapping = {
        'class' index: "config_per_project_class_idx"
        obsoleteDate index: "config_per_project_project_id_seq_type_id_obsolete_date_idx"
        seqType index: "config_per_project_project_id_seq_type_id_obsolete_date_idx"
        project index: "config_per_project_project_id_seq_type_id_obsolete_date_idx"
    }
}
