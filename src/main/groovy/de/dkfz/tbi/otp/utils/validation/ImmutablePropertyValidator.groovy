/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.utils.validation

import org.codehaus.groovy.runtime.InvokerHelper
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.GormEntity
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable
import org.grails.orm.hibernate.HibernateDatastore
import org.hibernate.collection.spi.PersistentCollection

/**
 * Validator for properties which are fixed after the domain object is saved.
 *
 * Use it as custom validator of the property, e.g. {@code name validator: ImmutablePropertyValidator.validator('name')},
 * or call {@link #isChanged} from an own validator. The error code is {@link #CHANGED}, so the message is defined by
 * "domainClass.property.immutable".
 *
 * The check needs no Hibernate session, so it works for detached objects, too. Combine it with "updatable: false"
 * in the mapping, so that the database is protected also when the validation is skipped.
 */
class ImmutablePropertyValidator {

    static final String CHANGED = "immutable"

    /**
     * Returns whether the property of a saved domain object was changed. For an unsaved object, it is always false.
     *
     * A change is detected by:
     * <ul>
     * <li>the dirty tracking of the domain object: the setter was called with an unequal value, also for a detached object</li>
     * <li>a dirty Hibernate collection: an element was added or removed, also for a detached object</li>
     * <li>the state loaded by Hibernate: the collection was replaced by another one, even with the same elements</li>
     * <li>a detached Hibernate object, or an attached one without loaded state (e.g. loaded read-only), whose collection is
     * no Hibernate collection: it was replaced, even with the same elements, since Hibernate wraps the collections of a saved
     * object</li>
     * </ul>
     */
    @SuppressWarnings("Instanceof") // the runtime types of Hibernate and GORM determine how a change can be detected
    static boolean isChanged(GormEntity entity, String property) {
        if (entity.ident() == null) {
            return false
        }
        Object value = InvokerHelper.getProperty(entity, property)
        if (value instanceof PersistentCollection && ((PersistentCollection) value).isDirty()) {
            return true
        }
        if (entity instanceof DirtyCheckable && property in ((DirtyCheckable) entity).listDirtyPropertyNames()) {
            return true
        }
        if (entity.attached) {
            Object loadedValue = entity.getPersistentValue(property)
            if (loadedValue != null) {
                return loadedValue instanceof PersistentCollection && !loadedValue.is(value)
            }
        }
        return value instanceof Collection && !(value instanceof PersistentCollection) && isHibernate(entity)
    }

    /** false for the datastore of unit tests (DataTest), which does not wrap collections */
    @SuppressWarnings("Instanceof") // the type of the datastore determines whether collections are wrapped
    private static boolean isHibernate(GormEntity entity) {
        return GormEnhancer.findDatastore(entity.getClass()) instanceof HibernateDatastore
    }

    /**
     * Returns a custom validator for the given property, which rejects a change of a saved domain object with {@link #CHANGED}.
     */
    static Closure validator(String property) {
        return { Object value, GormEntity entity ->
            return isChanged(entity, property) ? CHANGED : true
        }
    }
}
