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
package de.dkfz.tbi.otp.project

import grails.core.GrailsApplication
import grails.plugin.springsecurity.annotation.Secured
import grails.validation.Validateable
import grails.validation.ValidationException
import groovy.transform.ToString
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.FlashMessage
import de.dkfz.tbi.otp.config.TypeValidators
import de.dkfz.tbi.otp.project.additionalField.*
import de.dkfz.tbi.otp.utils.AbstractGeneralDomainPropertyUpdateController

@Secured("hasRole('ROLE_OPERATOR')")
class ProjectFieldsController extends AbstractGeneralDomainPropertyUpdateController<AbstractFieldDefinition> {

    static allowedMethods = [
            index                : "GET",
            create               : "GET",
            createText           : "POST",
            createFlag           : "POST",
            createInteger        : "POST",
            createDecimalNumber  : "POST",
            createDate           : "POST",
            createDomainReference: "POST",
            deleteFieldDefinition: "POST",
    ]

    static final List<TypeValidators> VALIDATORS = [
            TypeValidators.SINGLE_WORD_TEXT,
            TypeValidators.SINGLE_LINE_TEXT,
            TypeValidators.MULTI_LINE_TEXT,
            TypeValidators.MAIL,
    ].asImmutable()

    ProjectFieldsService projectFieldsService

    @Autowired
    GrailsApplication grailsApplication

    final Class<AbstractFieldDefinition> entityClass = AbstractFieldDefinition

    def index() {
        List<AbstractFieldDefinition> fieldDefinitions = projectFieldsService.listAndFetchValueLists()
        List<AbstractFieldDefinition> usedFieldDefinitions = projectFieldsService.usedDefinitions()
        List<ProjectFieldReferenceAble> referenceAbles = referenceAbleDomains
        Map<String, List<ProjectFieldReferenceAble>> data = fieldDefinitions.findAll {
            it.projectFieldType == ProjectFieldType.DOMAIN_REFERENCE
        }*.domainClassName.unique().collectEntries {
            Class<? extends ProjectFieldReferenceAble> clazz = DomainReferenceFieldValue.classLoader.loadClass(it)
            List<ProjectFieldReferenceAble> projectFieldReferenceAbles = clazz.list()
            projectFieldReferenceAbles.sort {
                it.stringForProjectFieldDomainReference
            }
            [(it): projectFieldReferenceAbles]
        }
        Map<AbstractFieldDefinition, Boolean> usedFieldDefinitionMap = fieldDefinitions.collectEntries {
            [(it): usedFieldDefinitions.contains(it)]
        }

        return [
                fieldDefinitions      : fieldDefinitions,
                usedFieldDefinitionMap: usedFieldDefinitionMap,
                validators            : VALIDATORS,
                referenceAbleDomains  : referenceAbles,
                data                  : data,
        ]
    }

    def create() {
        ProjectFieldsCreateCommand cmd = flash.cmd ?: new ProjectFieldsCreateTextCommand()
        // set the default value to true, so that this checkBox is checked in create.gsp
        cmd.changeOnlyByOperator = true
        return [
                cmd                 : cmd,
                validators          : VALIDATORS,
                referenceAbleDomains: referenceAbleDomains,
        ]
    }

    private List<ProjectFieldReferenceAble> getReferenceAbleDomains() {
        return grailsApplication.domainClasses.findAll {
            ProjectFieldReferenceAble.isAssignableFrom(it.clazz)
        }*.clazz*.name
    }

    def createText(ProjectFieldsCreateTextCommand cmd) {
        handleSubmitEvent(cmd)
    }

    def createFlag(ProjectFieldsCreateFlagCommand cmd) {
        handleSubmitEvent(cmd)
    }

    def createInteger(ProjectFieldsCreateIntegerCommand cmd) {
        handleSubmitEvent(cmd)
    }

    def createDecimalNumber(ProjectFieldsCreateDecimalNumbersCommand cmd) {
        handleSubmitEvent(cmd)
    }

    def createDate(ProjectFieldsCreateDateCommand cmd) {
        handleSubmitEvent(cmd)
    }

    def createDomainReference(ProjectFieldsCreateDomainReferenceCommand cmd) {
        handleSubmitEvent(cmd)
    }

    private void handleSubmitEvent(ProjectFieldsCreateCommand cmd) {
        if (params.create) {
            handleCreateEvent(cmd)
            return
        }
        handleChangeTypeEvent(cmd)
    }

    private void handleChangeTypeEvent(ProjectFieldsCreateCommand cmd) {
        flash.cmd = cmd
        flash.message = new FlashMessage(g.message(code: 'projectFields.create.changeType', args: [cmd.projectFieldType]))
        redirect(action: "create")
    }

    @SuppressWarnings('CatchRuntimeException')
    private void handleCreateEvent(ProjectFieldsCreateCommand cmd) {
        cmd.validate()
        if (cmd.hasErrors()) {
            flash.cmd = cmd
            flash.message = new FlashMessage(g.message(code: "projectFields.create.failure", args: [cmd?.name ?: '']) as String, cmd.errors)
            redirect(action: "create")
            return
        }
        try {
            projectFieldsService.create(cmd)
            flash.message = new FlashMessage(g.message(code: "projectFields.create.success", args: [cmd?.name ?: '']) as String)
            redirect(action: "index")
            return
        } catch (ValidationException e) {
            flash.message = new FlashMessage(g.message(code: "projectFields.create.failure", args: [cmd?.name ?: '']) as String, e.errors)
        } catch (AssertionError | RuntimeException e) {
            flash.message = new FlashMessage(g.message(code: "projectFields.create.failure", args: [cmd?.name ?: '']) as String, e.message)
        }
        flash.cmd = cmd
        redirect(action: "create")
    }

    @SuppressWarnings('CatchRuntimeException')
    def deleteFieldDefinition(DeleteFieldDefinitionCommand cmd) {
        cmd.validate()
        String name = cmd?.fieldDefinition?.name ?: ''
        if (cmd.hasErrors()) {
            flash.cmd = cmd
            flash.message = new FlashMessage(g.message(code: "projectFields.delete.failure", args: [name]) as String, cmd.errors)
            redirect(action: "index")
            return
        }
        try {
            projectFieldsService.deleteFieldDefinition(cmd.fieldDefinition)
            flash.message = new FlashMessage(g.message(code: "projectFields.delete.success", args: [name]) as String)
            redirect(action: "index")
            return
        } catch (ValidationException e) {
            flash.message = new FlashMessage(g.message(code: "projectFields.delete.failure", args: [name]) as String, e.errors)
        } catch (AssertionError | RuntimeException e) {
            flash.message = new FlashMessage(g.message(code: "projectFields.delete.failure", args: [name]) as String, e.message)
        }
        flash.cmd = cmd
        redirect(action: "index")
    }
}

@ToString(includeNames = true)
class DeleteFieldDefinitionCommand implements Validateable {
    AbstractFieldDefinition fieldDefinition
}

