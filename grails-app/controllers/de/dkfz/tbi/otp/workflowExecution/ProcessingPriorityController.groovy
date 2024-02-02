/*
 * Copyright 2011-2024 The OTP authors
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

import grails.converters.JSON
import grails.validation.ValidationException
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize

@PreAuthorize("hasRole('ROLE_OPERATOR')")
class ProcessingPriorityController {

    ProcessingPriorityService processingPriorityService

    static allowedMethods = [
            index: "GET",
            refer: "GET",
            save: "POST",
            update: "POST",
            delete: "DELETE",
    ]

    def index() {
        params.max = 1000
        return [
                processingPriorityList: processingPriorityService.getPriorityList(params),
                processingPriorityCount: processingPriorityService.priorityListCount,
        ]
    }

    JSON save(ProcessingPriority processingPriority) {
        try {
            ProcessingPriority savedProcessingPriority = processingPriorityService.savePriority(processingPriority)
            response.contentType = "application/json"
            render(savedProcessingPriority as JSON)
        } catch (ValidationException e) {
            log.debug(e.localizedMessage)
            response.sendError(HttpStatus.BAD_REQUEST.value(), g.message(code: "processingPriority.store.failure") as String)
        }
    }

    @SuppressWarnings(['ExplicitFlushForSaveRule'])
    JSON update(ProcessingPriority processingPriority) {
        log.debug("update: ${processingPriority}")
        if (processingPriority == null) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), g.message(code: "processingPriority.store.failure") as String)
        }

        return save(processingPriority)
    }

    def delete(Long id) {
        if (id == null) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), g.message(code: "processingPriority.store.failure") as String)
        }

        processingPriorityService.deletePriority(id)
        render(view: 'index')
    }

    JSON refer(Long id) {
        if (id == null) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), g.message(code: "processingPriority.fetch.failure") as String)
        }

        return render(processingPriorityService.getReferences(processingPriorityService.getPriority(id)) as JSON)
    }
}
