package de.dkfz.tbi.otp.workflowExecution

/*
 * Copyright 2011-2021 The OTP authors
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

import de.dkfz.tbi.otp.CheckAndCall
import de.dkfz.tbi.otp.ngsdata.LibraryPreparationKit
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.project.Project

/**
 * The BaseWorkflowConfigController contains common methods used
 * by WorkflowConfigController and WorkflowConfigViewerController.
 */
@SuppressWarnings('ControllerMethodNotInAllowedMethods')
trait BaseWorkflowConfigController extends CheckAndCall {

    Set<Workflow> getWorkflows() {
        return Workflow.findAllByDeprecatedDateIsNull().sort {
            a, b -> String.CASE_INSENSITIVE_ORDER.compare(a.toString(), b.toString())
        }
    }

    Set<WorkflowVersion> getWorkflowVersions() {
        return WorkflowVersion.all.sort { a, b ->
            String.CASE_INSENSITIVE_ORDER.compare(a.workflow.toString(), b.workflow.toString()) ?:
                    String.CASE_INSENSITIVE_ORDER.compare(a.workflowVersion, b.workflowVersion)
        }
    }

    Set<Project> getProjects() {
        return Project.all.sort { a, b -> String.CASE_INSENSITIVE_ORDER.compare(a.name, b.name) }
    }

    Set<SeqType> getSeqTypes() {
        return SeqType.all.sort {
            a, b -> String.CASE_INSENSITIVE_ORDER.compare(a.displayNameWithLibraryLayout,
             b.displayNameWithLibraryLayout)
        }
    }

    Set<ReferenceGenome> getReferenceGenomes() {
        return ReferenceGenome.all.sort {
            a, b -> String.CASE_INSENSITIVE_ORDER.compare(a.name, b.name)
        }
    }

    Set<LibraryPreparationKit> getLibraryPreparationKits() {
        return LibraryPreparationKit.all.sort {
            a, b -> String.CASE_INSENSITIVE_ORDER.compare(a.name, b.name)
        }
    }

    Set<SelectorType> getSelectorTypes() {
        return SelectorType.values().sort { a, b -> String.CASE_INSENSITIVE_ORDER.compare(a.name(), b.name()) }
    }

}
