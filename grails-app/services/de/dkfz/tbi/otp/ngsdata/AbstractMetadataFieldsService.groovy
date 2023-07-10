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

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.MetadataField

@CompileDynamic
@Transactional
abstract class AbstractMetadataFieldsService<T extends MetadataField> {

    // explicit protected is required, see: https://stackoverflow.com/a/20472740/6921511
    protected final static String MULTILINE_JOIN_STRING = ";\n"

    T findByNameOrImportAlias(String nameOrImportAlias, Map properties = [:]) {
        assert nameOrImportAlias: "the input 'nameOrImportAlias' is null"

        return findByName(nameOrImportAlias, properties) ?: findByImportAlias(nameOrImportAlias, properties)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void addNewAlias(Long id, String importAlias) {
        assert id: "the input ID must not be null"
        assert importAlias: "the input importAlias must not be null"

        T instance = clazz.get(id)
        assert instance: "could not get an instance of type ${clazz} with ID ${id}"

        T tFromImportAlias = findByNameOrImportAlias(importAlias)
        assert !tFromImportAlias: "importAlias ${importAlias} already exists for ${tFromImportAlias.name}"

        getTAndAddAlias(instance.name, importAlias)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void changeLegacyState(T domainObject, boolean legacy) {
        domainObject.legacy = legacy
        assert domainObject.save(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    T create(String name, Map properties = [:], List<String> importAliases = []) {
        assert name: "the input name '${name}' must not be null"
        checkProperties(properties)
        checkNameAndAliases(name, properties, importAliases)
        T t = clazz.newInstance([name: name, importAlias: importAliases] + properties)
        assert t.save(flush: true)
        return t
    }

    protected void checkProperties(Map properties) {
        assert !properties: "this class doesn't support additional properties, if you wish to use them this has to be done in a dedicated sub-class"
    }

    protected void checkNameAndAliases(String name, Map properties, List<String> importAliases) {
        assert !findByNameOrImportAlias(name, properties): "The ${clazz} '${name}' exists already"
        importAliases.each {
            assert !findByNameOrImportAlias(it, properties): "The ${clazz} importAlias '${it}' exists already"
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    protected void getTAndAddAlias(String name, String importAlias) {
        T t = findByNameOrImportAlias(name)
        assert t: "No ${clazz} with name or importAlias ${name} exists"
        assert !t.importAlias.contains(importAlias): "the importAlias was already created"
        t.importAlias.add(importAlias)
        assert t.save(flush: true)
    }

    protected T findByName(String name, Map properties = [:]) {
        checkProperties(properties)
        return CollectionUtils.atMostOneElement(clazz.findAllByNameIlike(name))
    }

    @SuppressWarnings("UnusedMethodParameter")
    protected T findByImportAlias(String importAlias, Map properties = [:]) {
        return CollectionUtils.atMostOneElement(clazz.list().findAll {
            it.importAlias*.toLowerCase()?.contains(importAlias.toLowerCase())
        })
    }

    abstract protected Class getClazz()
}
