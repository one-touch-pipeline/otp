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
package de.dkfz.tbi.otp.utils

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.codehaus.groovy.runtime.StringGroovyMethods
import org.grails.datastore.gorm.GormEntityApi
import org.springframework.security.access.prepost.PreAuthorize

import java.lang.reflect.ParameterizedType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Generic Service for converting values for update properties of an domain.
 *
 * Values given via the gui are Strings, but for setting they need often to convert to another type.
 * Since those code are very similar, this service provide therefore a generic solution.
 *
 * Based of domain class and property it determines the class and do the transformation.
 *
 * There usually the asType method of groovy is used, except for:
 * - Boolean: Groovy handle all not empty Strings as true, but here we want, that only the String 'true' is
 *   interpreted as true, all others should be false.
 * - LocalDate: class not part of the types handled per default. Therefore it is added: It use the
 *   format {@link DateTimeFormatter#ISO_LOCAL_DATE} for parsing
 *
 * All other classes are forwarded to {@link StringGroovyMethods#asType}
 *
 * It can also handle sets or list, were each string value is converted to the requested generic type of the collection, which itself are converted to the
 * type of the property. Empty values are removed from the list.
 */
@CompileStatic
@Transactional
class UpdateDomainPropertyService {

    /**
     * Update a single value property of a domain with a single value provided as string.
     * For more details see class description.
     *
     * @param clazz the domain class of the object to update
     * @param entityId the database id of the object to update
     * @param property the property to be update
     * @param value the new value as string, which is converted to the type of the property.
     * @return the updated object
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    GormEntityApi updateProperty(Class<GormEntityApi> clazz, Long entityId, String property, String value) {
        return handleUpdate(clazz, entityId, property) { MetaProperty metaProperty ->
            assert !Collection.isAssignableFrom(metaProperty.type): "the property ${property} of type ${metaProperty.type} implement Collection, " +
                    "which is not supported by this method"
            return value ? convertType(value, metaProperty.type) : null
        }
    }

    /**
     * Update a collection property of a domain with a list of values provided as a list of strings.
     * each value is converted to the generic type of the property and the list itself to the type of the property.
     * For more details see class description.
     *
     * @param clazz the domain class of the object to update
     * @param entityId the database id of the object to update
     * @param property the property to be update
     * @param values the new values as a list of strings.
     * @return the updated object
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    GormEntityApi updateProperties(Class<GormEntityApi> clazz, Long entityId, String property, List<String> values) {
        return handleUpdate(clazz, entityId, property) { MetaProperty metaProperty ->
            assert Collection.isAssignableFrom(metaProperty.type): "the property ${property} of type ${metaProperty.type} does not implement Collection, " +
                    "which is required by this method"

            Class genericType = getGenericTypeOfCollection(metaProperty)

            List<String> trimmedValues = values*.trim().findAll()
            List<Object> cleanedAndConvertedList = trimmedValues.collect { String value ->
                convertType(value, genericType)
            }

            return cleanedAndConvertedList.asType(metaProperty.type)
        }
    }

    private GormEntityApi handleUpdate(Class<GormEntityApi> clazz, Long entityId, String property, Closure getValue) {
        assert clazz: 'class not given'
        assert entityId: 'entityId not given'
        assert property: 'property name not given'
        assert GormEntityApi.isAssignableFrom(clazz): 'no subclass of GormEntityApi'

        GormEntityApi entity = load(clazz, entityId)
        assert entity: "No entity found for class '${clazz}' and id '${entityId}'"
        assert entity.hasProperty(property): "'${entity}' has no property with name '${property}'"

        MetaProperty metaProperty = getMetaProperty(entity, property)

        entity[property] = getValue(metaProperty)
        entity.save(flush: true)
        return entity
    }

    @CompileDynamic
    private GormEntityApi load(Class<GormEntityApi> clazz, Long entityId) {
        return clazz.get(entityId)
    }

    private MetaProperty getMetaProperty(GormEntityApi definition, String property) {
        return definition.metaClass.getMetaProperty(property)
    }

    private Class getGenericTypeOfCollection(MetaProperty property) {
        MetaBeanProperty metaBeanProperty = (MetaBeanProperty) property
        ParameterizedType parameterizedType = (ParameterizedType) metaBeanProperty.field.field.genericType
        return parameterizedType.actualTypeArguments[0]
    }

    private <T> T convertType(String value, Class<T> clazz) {
        if (clazz == LocalDate) {
            return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE) as T
        }
        if (clazz == Boolean || clazz == boolean) {
            return Boolean.parseBoolean(value) as T
        }
        return value.asType(clazz)
    }
}
