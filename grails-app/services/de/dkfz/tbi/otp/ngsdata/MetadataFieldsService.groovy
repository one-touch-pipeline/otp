package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.*
import org.springframework.security.access.prepost.*

abstract class MetadataFieldsService<T> {

    T findByNameOrImportAlias(String nameOrImportAlias, Map properties = [:]) {
        assert nameOrImportAlias: "the input 'nameOrImportAlias' is null"

        return findByName(nameOrImportAlias, properties) ?: findByImportAlias(nameOrImportAlias, properties)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void addNewAlias(String name, String importAlias) {
        assert name: "the input name must not be null"
        assert importAlias: "the input importAlias must not be null"
        T tFromImportAlias = findByNameOrImportAlias(importAlias)
        assert !tFromImportAlias: "importAlias ${importAlias} already exists for ${tFromImportAlias.name}"

        getTAndAddAlias(name, importAlias)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    T create(String name, Map properties = [:], List<String> importAliases = []) {
        assert name: "the input name '${name}' must not be null"
        checkProperties(properties)
        checkNameAndAliases(name, properties, importAliases)
        T t = clazz.newInstance([name: name, importAlias: importAliases] + properties)
        assert t.save(flush: true, failOnError: true)
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

    protected T findByImportAlias(String importAlias, Map properties = [:]) {
        return CollectionUtils.atMostOneElement(clazz.list().findAll {
            it.importAlias*.toLowerCase()?.contains(importAlias.toLowerCase())
        })
    }

    abstract protected Class getClazz()
}
