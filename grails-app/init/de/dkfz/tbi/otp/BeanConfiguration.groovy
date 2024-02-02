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
package de.dkfz.tbi.otp

import grails.databinding.converters.ValueConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.LocaleResolver
import org.springframework.web.servlet.i18n.FixedLocaleResolver

import de.dkfz.tbi.otp.utils.NumberConverter

@Configuration
class BeanConfiguration {
    // don't use the default locale specific value converter
    @Bean
    ValueConverter defaultGrailsShortConverter() {
        return new NumberConverter(targetType: Short)
    }

    @Bean
    @SuppressWarnings("ConfusingMethodName") // primitive type is lowercase
    ValueConverter defaultGrailsshortConverter() {
        return new NumberConverter(targetType: Short.TYPE)
    }

    @Bean
    ValueConverter defaultGrailsIntegerConverter() {
        return new NumberConverter(targetType: Integer)
    }

    @Bean
    @SuppressWarnings("ConfusingMethodName") // primitive type is lowercase
    ValueConverter defaultGrailsintegerConverter() {
        return new NumberConverter(targetType: Integer.TYPE)
    }

    @Bean
    ValueConverter defaultGrailsLongConverter() {
        return new NumberConverter(targetType: Long)
    }

    @Bean
    @SuppressWarnings("ConfusingMethodName") // primitive type is lowercase
    ValueConverter defaultGrailslongConverter() {
        return new NumberConverter(targetType: Long.TYPE)
    }

    @Bean
    ValueConverter defaultGrailsFloatConverter() {
        return new NumberConverter(targetType: Float)
    }

    @Bean
    @SuppressWarnings("ConfusingMethodName") // primitive type is lowercase
    ValueConverter defaultGrailsfloatConverter() {
        return new NumberConverter(targetType: Float.TYPE)
    }

    @Bean
    ValueConverter defaultGrailsDoubleConverter() {
        return new NumberConverter(targetType: Double)
    }

    @Bean
    ValueConverter defaultGrailsConverter() {
        return new NumberConverter(targetType: Double.TYPE)
    }

    @Bean
    ValueConverter defaultGrailsBigIntegerConverter() {
        return new NumberConverter(targetType: BigInteger)
    }

    @Bean
    ValueConverter defaultGrailsBigDecimalConverter() {
        return new NumberConverter(targetType: BigDecimal)
    }

    // only use English (prevents translations included in plugins being used)
    @Bean
    LocaleResolver localeResolver() {
        return new FixedLocaleResolver(Locale.ENGLISH)
    }
}
