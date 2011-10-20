package de.dkfz.tbi.otp.ngsdata

import org.springframework.dao.DataIntegrityViolationException

class RunController {

    static allowedMethods = [save: "POST", update: "POST", delete: "POST"]

    def index() {
        redirect(action: "list", params: params)
    }

    def list() {
        params.max = Math.min(params.max ? params.int('max') : 10, 100)
        [runInstanceList: Run.list(params), runInstanceTotal: Run.count()]
    }

    def create() {
        [runInstance: new Run(params)]
    }

    def save() {
        def runInstance = new Run(params)
        if (!runInstance.save(flush: true)) {
            render(view: "create", model: [runInstance: runInstance])
            return
        }

		flash.message = message(code: 'default.created.message', args: [message(code: 'run.label', default: 'Run'), runInstance.id])
        redirect(action: "show", id: runInstance.id)
    }

    def show() {
        def runInstance = Run.get(params.id)
        if (!runInstance) {
			flash.message = message(code: 'default.not.found.message', args: [message(code: 'run.label', default: 'Run'), params.id])
            redirect(action: "list")
            return
        }

        [runInstance: runInstance]
    }

    def edit() {
        def runInstance = Run.get(params.id)
        if (!runInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'run.label', default: 'Run'), params.id])
            redirect(action: "list")
            return
        }

        [runInstance: runInstance]
    }

    def update() {
        def runInstance = Run.get(params.id)
        if (!runInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'run.label', default: 'Run'), params.id])
            redirect(action: "list")
            return
        }

        if (params.version) {
            def version = params.version.toLong()
            if (runInstance.version > version) {
                runInstance.errors.rejectValue("version", "default.optimistic.locking.failure",
                          [message(code: 'run.label', default: 'Run')] as Object[],
                          "Another user has updated this Run while you were editing")
                render(view: "edit", model: [runInstance: runInstance])
                return
            }
        }

        runInstance.properties = params

        if (!runInstance.save(flush: true)) {
            render(view: "edit", model: [runInstance: runInstance])
            return
        }

		flash.message = message(code: 'default.updated.message', args: [message(code: 'run.label', default: 'Run'), runInstance.id])
        redirect(action: "show", id: runInstance.id)
    }

    def delete() {
        def runInstance = Run.get(params.id)
        if (!runInstance) {
			flash.message = message(code: 'default.not.found.message', args: [message(code: 'run.label', default: 'Run'), params.id])
            redirect(action: "list")
            return
        }

        try {
            runInstance.delete(flush: true)
			flash.message = message(code: 'default.deleted.message', args: [message(code: 'run.label', default: 'Run'), params.id])
            redirect(action: "list")
        }
        catch (DataIntegrityViolationException e) {
			flash.message = message(code: 'default.not.deleted.message', args: [message(code: 'run.label', default: 'Run'), params.id])
            redirect(action: "show", id: params.id)
        }
    }
}
