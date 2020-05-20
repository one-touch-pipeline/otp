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

import grails.converters.JSON
import grails.validation.Validateable
import grails.validation.ValidationException
import groovy.json.JsonSlurper
import groovy.transform.TupleConstructor

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.FlashMessage
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CommentCommand
import de.dkfz.tbi.otp.utils.DataTableCommand

class IndividualController {

    IndividualService individualService
    SeqTrackService seqTrackService
    ProjectService projectService
    CommentService commentService
    SampleIdentifierService sampleIdentifierService

    def show() {
        Individual individual
        if (params.id) {
            individual = individualService.getIndividual(params.id as Long)
        } else if (params.mockPid) {
            individual = individualService.getIndividualByMockPid(params.mockPid)
        } else {
            response.sendError(404)
            return
        }

        if (!individual) {
            return response.sendError(404)
        }

        return [
                individual         : individual,
                comment            : individual.comment,
                typeDropDown       : Individual.Type.values(),
                sampleTypeDropDown : individualService.getSampleTypeNames(),
                projectBlacklisted : ProjectOverviewService.hideSampleIdentifier(individual.project),
                groupedSeqTrackSets: seqTrackService.getSeqTrackSetsGroupedBySeqTypeAndSampleType(individual.getSeqTracks()),
        ]
    }

    def list() {
        return [
                tableHeader    : IndividualColumn.values()*.message,
                filterTree: [
                        [name : 'projectSelection', msgcode: 'individual.search.project',
                         type : 'LIST', from: projectService.getAllProjects(),,
                         value: 'displayName', key: 'id'],
                        [name: 'pidSearch', msgcode: 'individual.search.pid', type: 'TEXT'],
                        [name: 'mockFullNameSearch', msgcode: 'individual.search.mockFullName', type: 'TEXT'],
                        [name: 'mockPidSearch', msgcode: 'individual.search.mockPid', type: 'TEXT'],
                        [name: 'typeSelection', msgcode: 'individual.search.type',
                         type: 'LIST', from: Individual.Type.values()],
                ]
        ]
    }

    def dataTableSource(DataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()

        IndividualFiltering filtering = IndividualFiltering.fromJSON(params.filtering)

        dataToRender.iTotalRecords = individualService.countIndividual(filtering, cmd.sSearch)
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords

        individualService.listIndividuals(cmd.sortOrder, IndividualColumn.fromDataTable(cmd.iSortCol_0), filtering, cmd.sSearch).each { individual ->
            dataToRender.aaData << [
                    id          : individual.id,
                    pid         : individual.pid,
                    mockFullName: individual.mockFullName,
                    mockPid     : individual.mockPid,
                    project     : individual.project.toString(),
                    type        : individual.type.toString(),
            ]
        }
        render dataToRender as JSON
    }

    def insert() {
        List<Individual.Type> individualTypes = Individual.Type.values()
        List<String> sampleTypes = individualService.getSampleTypeNames()
        return [
                individualTypes: individualTypes,
                sampleTypes    : sampleTypes,
                cmd            : flash.cmd as IndividualCommand,
        ]
    }

    def save(IndividualCommand cmd) {
        if (cmd.hasErrors()) {
            flash.cmd = cmd
            flash.message = new FlashMessage(g.message(code: "individual.update.error") as String, cmd.errors)
            redirect(action: "insert")
            return
        }
        try {
            Individual individual = individualService.createIndividual(cmd)
            flash.message = new FlashMessage(g.message(code: "individual.update.create.success") as String)
            redirect(action: cmd.checkRedirect ? "show" : "insert", id: individual.id)
        } catch (ValidationException e) {
            flash.message = new FlashMessage(g.message(code: "individual.update.create.error") as String, e.errors)
            flash.cmd = cmd
            redirect(action: "insert")
        }
    }

    def updateField(UpdateFieldCommand cmd) {
        Individual individual = individualService.getIndividual(cmd.id)
        if (!individual) {
            Map data = [error: g.message(code: "individual.update.notFound", args: [cmd.id])]
            render data as JSON
            return
        }
        Map data = [:]
        try {
            individualService.updateField(individual, cmd.key, cmd.value)
            data.put("success", true)
        } catch (IndividualUpdateException e) {
            data.put("error", g.message(code: "individual.update.error"))
        }
        render data as JSON
    }

    def newSampleType(UpdateFieldCommand cmd) {
        Individual individual = individualService.getIndividual(cmd.id)
        if (!individual) {
            Map data = [error: g.message(code: "individual.update.notFound", args: [cmd.id])]
            render data as JSON
            return
        }
        Map data = [:]
        try {
            individualService.createSample(individual, cmd.value)
            data.put("success", true)
        } catch (Exception e) {
            data = [error: e.message]
        }
        render data as JSON
    }

    def saveIndividualComment(CommentCommand cmd) {
        Individual individual = individualService.getIndividual(cmd.id)
        commentService.saveComment(individual, cmd.comment)
        def dataToRender = [date: individual.comment.modificationDate.format('EEE, d MMM yyyy HH:mm'), author: individual.comment.author]
        render dataToRender as JSON
    }

    def editNewSampleIdentifier(UpdateSampleCommand cmd) {
        if (cmd.hasErrors()) {
            flash.cmd = cmd
            flash.message = new FlashMessage(g.message(code: "individual.update.error") as String, cmd.errors)
        } else {
            try {
                Individual.withTransaction {
                    cmd.editedSampleIdentifiers.each { EditedSampleIdentifierCommand editedSampleIdentifierCommand ->
                        if (editedSampleIdentifierCommand.delete) {
                            sampleIdentifierService.deleteSampleIdentifier(editedSampleIdentifierCommand.sampleIdentifier)
                        } else {
                            sampleIdentifierService.updateSampleIdentifierName(editedSampleIdentifierCommand.sampleIdentifier, editedSampleIdentifierCommand.name)
                        }
                    }
                    cmd.newIdentifiersNames.findAll().each { String name ->
                        sampleIdentifierService.createSampleIdentifier(name, cmd.sample)
                    }
                }
                flash.message = new FlashMessage(g.message(code: "individual.update.create.success") as String)
            } catch (ValidationException e) {
                flash.message = new FlashMessage(g.message(code: "individual.update.error") as String, e.errors)
            }
        }
        redirect(action: "show", id: cmd.sample.individual.id)
    }
}

@TupleConstructor
enum IndividualColumn {
    PID('individual.list.pid', "pid"),
    MOCKFULLNAME('individual.list.mockName', "mockFullName"),
    MOCKPID('individual.list.mockPid', "mockPid"),
    PROJECT('individual.list.project', "project"),
    TYPE('individual.list.type', "type"),

    final String message
    final String columnName

    static IndividualColumn fromDataTable(int column) {
        if (column >= values().size() || column < 0) {
            return TYPE
        }
        return values()[column]
    }
}

class IndividualCommand implements Validateable {
    IndividualService individualService

    String identifier
    Project individualProject
    String alias
    String displayedIdentifier
    String internIdentifier
    Individual.Type type
    List<SampleCommand> samples
    boolean checkRedirect

    static constraints = {
        identifier(blank: false, validator: { val, obj ->
            if (!(val && !obj.individualService.individualExists(val))) {
                return 'alreadyExists'
            }
        })
        alias(blank: false)
        displayedIdentifier(blank: false)
        internIdentifier(nullable: true, blank: true)
    }
}

class SampleCommand {
    String sampleType
    List<String> sampleIdentifiers
}

class UpdateFieldCommand {
    IndividualService individualService

    Long id
    String key
    String value

    static constraints = {
        id(min: 0L, validator: { val, obj ->
            return val && (obj.individualService.getIndividual(val) != null)
        })
        key(blank: false)
        value(blank: false)
    }
}

/**
 * Class describing the various filtering to do on Individual page.
 */
class IndividualFiltering {
    List<Long> project = []
    List<String> pid = []
    List<String> mockPid = []
    List<String> mockFullName = []
    List<Individual.Type> type = []

    boolean enabled = false

    static IndividualFiltering fromJSON(String json) {
        IndividualFiltering filtering = new IndividualFiltering()
        if (!json) {
            return filtering
        }
        def slurper = new JsonSlurper()
        slurper.parseText(json).each {
            switch (it.type) {
                case "projectSelection":
                    if (it.value.isLong()) {
                        filtering.project << (it.value as Long)
                        filtering.enabled = true
                    }
                    break
                case "pidSearch":
                    if (it.value && it.value.length() >= 3) {
                        filtering.pid << it.value
                        filtering.enabled = true
                    }
                    break
                case "mockFullNameSearch":
                    if (it.value && it.value.length() >= 3) {
                        filtering.mockFullName << it.value
                        filtering.enabled = true
                    }
                    break
                case "mockPidSearch":
                    if (it.value && it.value.length() >= 3) {
                        filtering.mockPid << it.value
                        filtering.enabled = true
                    }
                    break
                case "typeSelection":
                    if (it.value) {
                        filtering.type << (it.value as Individual.Type)
                        filtering.enabled = true
                    }
                    break
            }
        }
        return filtering
    }

}

class UpdateSampleCommand implements Validateable {
    List<EditedSampleIdentifierCommand> editedSampleIdentifiers
    Sample sample
    List<String> newIdentifiersNames
}

class EditedSampleIdentifierCommand implements Validateable {
    String name
    SampleIdentifier sampleIdentifier
    boolean delete

    static constraints = {
        name(blank: false)
    }
}
