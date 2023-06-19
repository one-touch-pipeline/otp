/*
 * Copyright 2011-2020 The OTP authors
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
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.Entity

@ManagedEntity
@ToString(includeNames = true, includePackage = false)
class ReferenceGenomeSelector implements Entity {

    Project project
    SeqType seqType
    Set<SpeciesWithStrain> species
    Workflow workflow

    ReferenceGenome referenceGenome

    static Closure constraints = {
        referenceGenome validator: { val, obj ->
            if (!(val.species.every { s -> s in obj.species*.species } &&
                    val.speciesWithStrain.every { sws -> sws in obj.species } &&
                    obj.species.every { sws -> sws in val.speciesWithStrain || sws.species in val.species })) {
                return "species"
            }
        }
    }

    static Closure mapping = {
        project index: 'reference_genome_selector_project_idx'
        seqType index: 'reference_genome_selector_seq_type_idx'
        workflow index: 'reference_genome_selector_workflow_idx'
        referenceGenome index: 'reference_genome_selector_reference_genome_idx'
    }

    @Override
    String toString() {
        return "RGS: (${project} ${seqType} ${species.sort { it.displayString }.join("+")} ${workflow}) -> (${referenceGenome})"
    }
}
