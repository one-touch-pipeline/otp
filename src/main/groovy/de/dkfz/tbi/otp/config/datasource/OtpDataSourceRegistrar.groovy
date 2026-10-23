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
package de.dkfz.tbi.otp.config.datasource

import groovy.util.logging.Slf4j
import org.springframework.beans.BeansException
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor

/**
 * Swaps GORM's {@code dataSourceConnectionSourceFactory} bean for {@link OtpDataSourceConnectionSourceFactory} (or a
 * subclass of it), so that the groovy web console uses its own, restricted pool.
 */
@Slf4j
class OtpDataSourceRegistrar implements BeanDefinitionRegistryPostProcessor {

    static final String FACTORY_BEAN_NAME = "dataSourceConnectionSourceFactory"

    Class<? extends OtpDataSourceConnectionSourceFactory> factoryClass = OtpDataSourceConnectionSourceFactory

    @Override
    void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        if (!registry.containsBeanDefinition(FACTORY_BEAN_NAME)) {
            throw new IllegalStateException("Could not find bean '${FACTORY_BEAN_NAME}' to install the console data source; " +
                    "this should be unreachable when GORM is configured normally.")
        }
        registry.getBeanDefinition(FACTORY_BEAN_NAME).beanClassName = factoryClass.name
        log.debug("Replaced '${FACTORY_BEAN_NAME}' with ${factoryClass.simpleName}")
    }

    @Override
    void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // nothing to do
    }
}
