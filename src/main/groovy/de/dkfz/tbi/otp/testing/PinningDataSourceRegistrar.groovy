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
package de.dkfz.tbi.otp.testing

import groovy.util.logging.Slf4j
import org.springframework.beans.BeansException
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor

/**
 * Swaps GORM's {@code dataSourceConnectionSourceFactory} bean for {@link PinningDataSourceConnectionSourceFactory}, so
 * that the data source Hibernate (and the {@code dataSource} bean) uses is pinned for Cypress test isolation.
 *
 * <p>Only registered when {@code otp.testing.endpoints.enabled=true} (see {@code resources.groovy}); in production it is
 * never registered and GORM's normal factory is used unchanged.</p>
 */
@Slf4j
class PinningDataSourceRegistrar implements BeanDefinitionRegistryPostProcessor {

    static final String FACTORY_BEAN_NAME = "dataSourceConnectionSourceFactory"

    @Override
    void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        if (registry.containsBeanDefinition(FACTORY_BEAN_NAME)) {
            registry.getBeanDefinition(FACTORY_BEAN_NAME).beanClassName = PinningDataSourceConnectionSourceFactory.name
            log.warn("Testing endpoints are ENABLED: replaced '${FACTORY_BEAN_NAME}' with " +
                    "PinningDataSourceConnectionSourceFactory. This must never happen in production.")
        } else {
            throw new TestingEndpointsException("Could not find bean '${FACTORY_BEAN_NAME}' to install Cypress test " +
                    "isolation; the testing endpoints cannot work. This should be unreachable when GORM is configured " +
                    "normally, so fail fast rather than start up half-installed.")
        }
    }

    @Override
    void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // nothing to do
    }
}
