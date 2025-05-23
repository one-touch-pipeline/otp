/*
 * Copyright 2011-2025 The OTP authors
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
package de.dkfz.tbi.otp.workflowExecution.decider

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflowExecution.Artefact
import de.dkfz.tbi.otp.workflowExecution.WorkflowArtefact

@ToString(includePackage = false, includeNames = true)
@EqualsAndHashCode(cache = true)
class ArtefactData<A extends Artefact> {
    final WorkflowArtefact workflowArtefact
    final A artefact
    final String version
    final Project project
    final SeqType seqType

    ArtefactData(WorkflowArtefact workflowArtefact, A artefact, String version, Project project, SeqType seqType) {
        this.workflowArtefact = workflowArtefact
        this.artefact = artefact
        this.version = versionHelper(version)
        this.project = project
        this.seqType = seqType
    }

    /**
     * Helper to get the version number for roddy plugins in the old workflow system.
     *
     * In the old workflow system, the plugin version was saved together with the plugin name, seperated by column.
     * In the new system only the version is saved.
     * For correct handling of version comparison, the workflow name is removed, so that only the version is saved.
     */
    static private String versionHelper(String version) {
        return version && version.contains(':') ? version.split(':')[1] : version
    }
}
