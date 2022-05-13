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
package de.dkfz.tbi.otp.dataprocessing.cellRanger

import grails.plugin.springsecurity.annotation.Secured
import grails.validation.ValidationException
import groovy.transform.Canonical

import de.dkfz.tbi.otp.FlashMessage
import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

@Secured('isFullyAuthenticated()')
class CellRangerController {
    CellRangerConfigurationService cellRangerConfigurationService
    ProjectSelectionService projectSelectionService
    SeqTypeService seqTypeService

    static allowedMethods = [
            finalRunSelection    : "GET",
            saveFinalRunSelection: "POST",
    ]

    def finalRunSelection() {
        Project project = projectSelectionService.selectedProject

        List<CellRangerMergingWorkPackage> mwps = cellRangerConfigurationService.findCellRangerMergingWorkPackageByProject(project)

        Map<GroupedMwp, List<CellRangerMergingWorkPackage>> grouped = mwps.groupBy({
            new GroupedMwp(it.sample, it.seqType, it.config.programVersion, it.referenceGenomeIndex)
        })

        List<GroupedMwp> groupedMwps = grouped.collect { k, v ->
            k.mwps = v
            return k
        }

        groupedMwps.sort()
        groupedMwps.each { it.mwps.sort() }

        return [
                groupedMwps: groupedMwps,
        ]
    }

    def saveFinalRunSelection(MwpSelectionCommand cmd) {
        if (!cmd.validate()) {
            flash.message = new FlashMessage(g.message(code: "cellRanger.selection.failure") as String, cmd.errors)
            redirect(action: "finalRunSelection")
            return
        }

        try {
            if (cmd.mwp?.isLong()) {
                cellRangerConfigurationService.selectMwpAsFinal(CellRangerMergingWorkPackage.get(cmd.mwp.toLong()))
            } else if (cmd.mwp == "delete") {
                cellRangerConfigurationService.selectNoneAsFinal(cmd.sample, cmd.seqType, cmd.programVersion, cmd.reference)
            }
            flash.message = new FlashMessage(g.message(code: "cellRanger.selection.success") as String)
        } catch (ValidationException e) {
            flash.message = new FlashMessage(g.message(code: "cellRanger.selection.failure") as String, e.errors)
        } catch (Exception e) {
            flash.message = new FlashMessage(g.message(code: "cellRanger.selection.failure") as String, e.message)
        }

        redirect(action: "finalRunSelection")
    }
}

@Canonical
class GroupedMwp implements Comparable {
    Sample sample
    SeqType seqType
    String programVersion
    ReferenceGenomeIndex reference
    List<CellRangerMergingWorkPackage> mwps

    boolean isAtLeastOneInProgress() {
        mwps.any { !it.bamFileInProjectFolder }
    }

    boolean isAnyUnsetAndNoneFinal() {
        mwps.any { it.status == CellRangerMergingWorkPackage.Status.UNSET } && !mwps.any { it.status == CellRangerMergingWorkPackage.Status.FINAL }
    }

    @Override
    int compareTo(Object o) {
        this.sample.individual.displayName <=> o.sample.individual.displayName ?:
                this.sample.sampleType.displayName <=> o.sample.sampleType.displayName ?:
                        this.seqType.nameWithLibraryLayout <=> o.seqType.nameWithLibraryLayout ?:
                                this.programVersion <=> o.programVersion ?:
                                        this.reference.toString() <=> o.reference.toString()
    }
}

class MwpSelectionCommand {
    Sample sample
    SeqType seqType
    String programVersion
    ReferenceGenomeIndex reference

    // we can't use a CellRangerMergingWorkPackage object here because we need to distinguish
    // between "delete all" and no radio button selected
    String mwp
}
