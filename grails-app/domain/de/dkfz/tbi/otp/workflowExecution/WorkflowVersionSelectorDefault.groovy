/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.workflowExecution

import grails.gorm.hibernate.annotation.ManagedEntity
import groovy.transform.ToString

import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.utils.Entity

/**
 * A single workflow/version entry of a {@link WorkflowDefaultGroup}. The workflow is derived from
 * {@link WorkflowVersion#getWorkflow()}, matching {@link WorkflowVersionSelector}. referenceGenome/species are only
 * set for alignment defaults, analysis defaults leave them empty.
 */
@ToString(includeNames = true, includePackage = false)
@ManagedEntity
class WorkflowVersionSelectorDefault implements Entity {

    WorkflowDefaultGroup group
    WorkflowVersion workflowVersion

    ReferenceGenome referenceGenome
    Set<SpeciesWithStrain> species

    static belongsTo = [
            group: WorkflowDefaultGroup,
    ]

    static hasMany = [
            species: SpeciesWithStrain,
    ]

    static Closure constraints = {
        referenceGenome nullable: true
    }

    static Closure mapping = {
        group index: "workflow_version_selector_default_group_idx"
        workflowVersion index: "workflow_version_selector_default_workflow_version_idx"
        referenceGenome index: "workflow_version_selector_default_reference_genome_idx"
    }

    @Override
    String toString() {
        return "WorkflowVersionSelectorDefault ${id}: (${group}) -> (${workflowVersion})" +
                (referenceGenome ? " [${referenceGenome}]" : "") +
                (species ? " [${species*.toString().sort().join("+")}]" : "")
    }
}
