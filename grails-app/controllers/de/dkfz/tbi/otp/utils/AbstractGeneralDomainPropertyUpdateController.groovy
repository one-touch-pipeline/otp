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
package de.dkfz.tbi.otp.utils

import grails.converters.JSON
import grails.validation.Validateable
import groovy.transform.ToString
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.CheckAndCall

/**
 * A generic base class for update properties of a domain class using {@link UpdateDomainPropertyService}.
 * @param <T> The domain class to support updates
 */
abstract class AbstractGeneralDomainPropertyUpdateController<T extends Entity> implements CheckAndCall {

    UpdateDomainPropertyService updateDomainPropertyService

    static allowedMethods = [
            updateField     : "POST",
            updateMultiField: "POST",
    ]

    /**
     * Callback to get the domain class to use for property updates.
     */
    protected abstract Class<T> getEntityClass()

    /**
     * Update a single value property of a domain.
     * Its the controller method for {@link UpdateDomainPropertyService#updateProperty(Class, Long, String, String)}.
     * The domain class is fetch about the callback {@link #getEntityClass()}. The other values are provided from {@link UpdateDomainPropertyCommand}.
     *
     * @param cmd command object for the GUI parameters, see  {@link UpdateDomainPropertyCommand}
     * @return a JSON providing information about success or failure.
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    JSON updateField(UpdateDomainPropertyCommand cmd) {
        return checkErrorAndCallMethodWithExtendedMessagesAndJsonRendering(cmd) {
            updateDomainPropertyService.updateProperty(entityClass, cmd.entityId, cmd.property, cmd.value)
            return [:]
        }
    }

    /**
     * Update a multi value property of a domain.
     * Its the controller method for {@link UpdateDomainPropertyService#updateProperties(Class, Long, String, List)}.
     * The domain class is fetch about the callback {@link #getEntityClass()}. The other values are provided from {@link UpdateDomainPropertiesCommand}.
     *
     * @param cmd command object for the GUI parameters, see  {@link UpdateDomainPropertiesCommand}
     * @return a JSON providing information about success or failure.
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    JSON updateMultiField(UpdateDomainPropertiesCommand cmd) {
        return checkErrorAndCallMethodWithExtendedMessagesAndJsonRendering(cmd) {
            updateDomainPropertyService.updateProperties(entityClass, cmd.entityId, cmd.property, cmd.value)
            return [:]
        }
    }
}

@ToString(includeNames = true)
abstract class AbstractUpdateDomainCommand implements Validateable {
    Long entityId
    String property
}

@ToString(includeNames = true, includeSuperProperties = true)
class UpdateDomainPropertyCommand extends AbstractUpdateDomainCommand {
    String value
}

@ToString(includeNames = true, includeSuperProperties = true)
class UpdateDomainPropertiesCommand extends AbstractUpdateDomainCommand {
    List<String> value
}
