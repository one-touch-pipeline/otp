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

import grails.gorm.transactions.Transactional
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.OtpRuntimeException
import de.dkfz.tbi.otp.project.additionalField.*

@Transactional
class ProjectFieldsService {

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<AbstractFieldDefinition> listAndFetchValueLists() {
        return AbstractFieldDefinition.list().sort { a, b ->
            a.sortNumber <=> b.sortNumber ?: a.name.compareToIgnoreCase(b.name)
        }.each {
            it.valueList.iterator() //initialize the lazy lists
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<AbstractFieldDefinition> usedDefinitions() {
        return AbstractFieldValue.createCriteria().listDistinct {
            projections {
                property('definition')
            }
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    AbstractFieldDefinition create(ProjectFieldsCreateCommand cmd) {
        assert cmd
        assert cmd.validate()

        ProjectFieldType projectFieldType = cmd.projectFieldType
        AbstractFieldDefinition definition = projectFieldType.definitionClass.newInstance()
        definition.with {
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
        if (cmd.projectFieldType.propertyDefaultValue) {
            definition[cmd.projectFieldType.propertyDefaultValue] = cmd.defaultValue ?: null
        }
        if (cmd.projectFieldType.propertyAllowedValue) {
            definition[cmd.projectFieldType.propertyAllowedValue] = cmd.allowedValues
        }
        if (cmd.projectFieldType == ProjectFieldType.TEXT) {
            definition.typeValidator = cmd.typeValidator
            definition.regularExpression = cmd.regularExpression
            definition.regularExpressionError = cmd.regularExpressionError
        }
        if (cmd.projectFieldType == ProjectFieldType.DOMAIN_REFERENCE) {
            definition.domainClassName = cmd.domainClassName
            definition.allowCustomValue = cmd.allowCustomValue
        }

        definition.save()
        return definition
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void deleteFieldDefinition(AbstractFieldDefinition definition) {
        assert definition: "definition not given"
        long count = AbstractFieldValue.countByDefinition(definition)
        if (count > 0) {
            throw new OtpRuntimeException("The field '${definition}' is already used")
        }
        definition.delete()
    }
}

