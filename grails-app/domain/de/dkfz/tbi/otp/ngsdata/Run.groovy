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
package de.dkfz.tbi.otp.ngsdata

import grails.gorm.hibernate.annotation.ManagedEntity

import de.dkfz.tbi.otp.job.processing.ProcessParameterObject
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.Entity
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

/**
 * Run represents one sequencing Run. It is one of the most important classes
 * in the NGS database. The run is typically submitted all at once, but it
 * could be submitted in parts from different locations and belonging to different
 * projects. The initial locations are stored in FastqImportInstance objects.
 */
@ManagedEntity
class Run implements ProcessParameterObject, Entity {

    String name                      // run name

    Date dateExecuted = null

    boolean blacklisted = false      // run is known to be invalid

    SeqCenter seqCenter
    SeqPlatform seqPlatform

    static belongsTo = [
            seqCenter  : SeqCenter,
            seqPlatform: SeqPlatform,
    ]

    static constraints = {
        name(blank: false, unique: true, shared: "pathComponent")
        dateExecuted(nullable: true)
    }

    @Override
    String toString() {
        name
    }

    /*
     * returns null if a run has more than one sequencing type,
     * because this case is unusable for creating Cluster Jobs
     */

    // the method is needed for compatibility of very old data of the old workflow system and shouldn't be used in new code
    @Deprecated
    @Override
    SeqType getSeqType() {
        List<SeqType> seqTypes = SeqTrack.findAllByRun(this)*.seqType
        if (seqTypes.unique().size() == 1) {
            return seqTypes.get(0)
        }
        return null
    }

    // the method is needed for compatibility of very old data of the old workflow system and shouldn't be used in new code
    @Deprecated
    @Override
    Project getProject() {
        return individual?.project
    }

    /*
     * returns the individual being sequenced in this run
     * returns null if a run has more than one individual
     */
    // the method is needed for compatibility of very old data of the old workflow system  and shouldn't be used in new code
    @Deprecated
    @Override
    Individual getIndividual() {
        List<Individual> individuals = SeqTrack.findAllByRun(this)*.individual
        if (individuals.unique().size() == 1) {
            return individuals.first()
        }
        return null
    }

    @Override
    Set<SeqTrack> getContainedSeqTracks() {
        return new HashSet<SeqTrack>(SeqTrack.findAllByRun(this))
    }

    /**
     * It returns the highest priority of the corresponding projects.
     */
    @Override
    ProcessingPriority getProcessingPriority() {
        return RawSequenceFile.findAllByRun(this)*.project*.processingPriority.max {
            it.priority
        }
    }

    static mapping = {
        seqCenter index: "run_seq_center_idx"
        seqPlatform index: "run_seq_platform_idx"
    }

    String getDirName() {
        return "run${name}"
    }
}
