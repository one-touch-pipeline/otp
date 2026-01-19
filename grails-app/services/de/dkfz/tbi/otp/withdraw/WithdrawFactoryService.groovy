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
package de.dkfz.tbi.otp.withdraw

import groovy.transform.Synchronized
import org.hibernate.Hibernate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.utils.Entity

/**
 * WithdrawFactoryService is a helper service that provides the correct ProcessingWithdrawService
 * for a given domain entity class. It maintains a mapping between entity classes and their corresponding
 * withdraw services, allowing for easy retrieval of the appropriate service for processing withdrawals.
 */
@PreAuthorize("hasRole('ROLE_OPERATOR')")
class WithdrawFactoryService {

    /**
     * List of all available ProcessingWithdrawService instances.
     * These services are injected and used to build the entity-to-service mapping.
     */
    @Autowired
    List<ProcessingWithdrawService<? extends Entity, ? extends Entity>> withdrawServices

    /**
     * Internal mapping from entity class to its corresponding ProcessingWithdrawService.
     * Built during initialization and used for fast lookup.
     */
    private Map<Class<? extends Entity>, ProcessingWithdrawService<? extends Entity, ? extends Entity>> mappedEntity

    /**
     * Initializes the mappedEntity by collecting supported entity classes from each withdraw service
     * and associating them with their respective service.
     *
     * Since the method calls secured methods, it requires an authentication object itself, which is not available during bean creation.
     * Therefore it is initialized manually by the findProcessingWithdrawService method on its first use.
     */
    @Synchronized
    private void initMap() {
        if (mappedEntity) {
            return
        }
        mappedEntity = (withdrawServices.collectEntries { ProcessingWithdrawService<? extends Entity, ? extends Entity> withdrawService ->
            withdrawService.supportedClasses.collectEntries {
                [(it): withdrawService]
            }
        }.asImmutable() as Map<Class<? extends Entity>, ProcessingWithdrawService<? extends Entity, ? extends Entity>>)
    }

    /**
     * Finds and returns the ProcessingWithdrawService for the given entity instance.
     * Uses Hibernate to get the actual class of the entity for accurate mapping.
     *
     * @param entity The domain entity instance for which to find the withdraw service.
     * @return The corresponding ProcessingWithdrawService, or null if not found.
     */
    ProcessingWithdrawService findProcessingWithdrawService(Entity entity) {
        if (!mappedEntity) {
            initMap()
        }
        return mappedEntity[Hibernate.getClass(entity)]
    }
}
