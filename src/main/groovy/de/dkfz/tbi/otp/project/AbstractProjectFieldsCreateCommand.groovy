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
package de.dkfz.tbi.otp.project

import grails.validation.Validateable
import groovy.transform.CompileDynamic
import groovy.transform.ToString

import de.dkfz.tbi.otp.project.additionalField.*

@CompileDynamic
@ToString(includeNames = true)
abstract class AbstractProjectFieldsCreateCommand implements Validateable {

    ProjectFieldType projectFieldType

    ProjectCardinalityType cardinalityType = ProjectCardinalityType.SINGLE

    String name

    String descriptionConfig

    String descriptionRequest

    FieldExistenceType fieldUseForSequencingProjects = FieldExistenceType.REQUIRED

    FieldExistenceType fieldUseForDataManagementProjects = FieldExistenceType.REQUIRED

    ProjectSourceOfData sourceOfData = ProjectSourceOfData.REQUESTER

    ProjectDisplayOnConfigPage projectDisplayOnConfigPage = ProjectDisplayOnConfigPage.SHOW

    int sortNumber = 0

    boolean changeOnlyByOperator = false

    boolean usedExternally = false

    static constraints = {
        name validator: { obj ->
            if (obj && AbstractFieldDefinition.findAllByName(obj)) {
                return 'projectFields.createCommand.no.unique'
            }
        }
    }

    abstract Object getDefaultValue()

    abstract List<Object> getAllowedValues()

    protected AbstractProjectFieldsCreateCommand(ProjectFieldType projectFieldType) {
        this.projectFieldType = projectFieldType
    }

    final void copyValuesFrom(AbstractProjectFieldsCreateCommand cmd) {
        projectFieldType = cmd.projectFieldType
        cardinalityType = cmd.cardinalityType
        name = cmd.name
        descriptionConfig = cmd.descriptionConfig
        descriptionRequest = cmd.descriptionRequest
        fieldUseForSequencingProjects = cmd.fieldUseForSequencingProjects
        fieldUseForDataManagementProjects = cmd.fieldUseForDataManagementProjects
        sourceOfData = cmd.sourceOfData
        projectDisplayOnConfigPage = cmd.projectDisplayOnConfigPage
        sortNumber = cmd.sortNumber
        changeOnlyByOperator = cmd.changeOnlyByOperator
        usedExternally = cmd.usedExternally
    }
}
