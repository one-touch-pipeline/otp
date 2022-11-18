/*
 * Copyright 2011-2022 The OTP authors
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

import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler
import org.springframework.security.access.hierarchicalroles.RoleHierarchy
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl
import org.springframework.security.authentication.*
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.ldap.DefaultSpringSecurityContextSource
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.*
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter

import de.dkfz.tbi.otp.security.*

import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import static org.springframework.security.config.Customizer.withDefaults

@Configuration
@EnableWebSecurity
@EnableMethodSecurity()
class SecurityConfiguration {

    @Bean
    static MethodSecurityExpressionHandler securityExpressionHandler(ProjectPermissionEvaluator projectPermissionEvaluator) {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler()
        expressionHandler.permissionEvaluator = projectPermissionEvaluator
        expressionHandler.roleHierarchy = roleHierarchy()
        return expressionHandler
    }

    @Bean
    CustomRequestDataValueProcessor requestDataValueProcessor() {
        return new CustomRequestDataValueProcessor()
    }

    @Bean
    AuthenticationTrustResolverImpl authenticationTrustResolver() {
        return new AuthenticationTrustResolverImpl()
    }

    @Bean
    DefaultSpringSecurityContextSource contextSource() {
        return new DefaultSpringSecurityContextSource("ldap://localhost:389/dc=otpldap,dc=dev")
    }

    @Bean
    AuthenticationManager ldapAuthenticationManager(HttpSecurity http, LdapDaoAuthenticationProvider ldapDaoAuthenticationProvider) {
        AuthenticationManagerBuilder authenticationManagerBuilder = http.getSharedObject(AuthenticationManagerBuilder)
        authenticationManagerBuilder.authenticationProvider(ldapDaoAuthenticationProvider)
        return authenticationManagerBuilder.build()
    }

    @Bean
    static RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl r = new RoleHierarchyImpl()
        r.hierarchy = '''
            ROLE_ADMIN > ROLE_OPERATOR
            ROLE_OPERATOR > ROLE_TEST_PI
            ROLE_OPERATOR > ROLE_TEST_BIOINFORMATICAN
            ROLE_OPERATOR > ROLE_TEST_SUBMITTER
            ROLE_ADMIN > ROLE_SWITCH_USER
        '''
        return r
    }

    @Bean
    WebSecurityCustomizer webSecurityCustomizer() {
        // ignoring doesn't set a security context, use permitAll instead for regular pages
        return (web) -> web.ignoring().antMatchers(
                "/assets/**",
        )
    }

    @Bean
    SwitchUserFilter switchUserFilter(UserDetailsService userDetailsService) {
        SwitchUserFilter filter = new SwitchUserFilter()
        filter.userDetailsService = userDetailsService
        filter.switchUserUrl = "/impersonate"
        filter.switchFailureUrl = "/"
        filter.targetUrl = "/"
        return filter
    }

    @Bean
    // for DicomAuditSecurityEventListener
    AuthenticationEventPublisher authenticationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        return new DefaultAuthenticationEventPublisher(applicationEventPublisher)
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        AuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler()
        successHandler.defaultTargetUrl = "/home/index"
        successHandler.targetUrlParameter = ParameterAuthenticationEntryPoint.TARGET_PARAM_NAME
        AuthenticationFailureHandler failureHandler = new SimpleUrlAuthenticationFailureHandler() {
            @Override
            void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                         AuthenticationException exception) throws IOException, ServletException {
                String[] username = request.parameterMap.get("username")
                if (username) {
                    request.session.setAttribute(LoginController.LAST_USERNAME_KEY, username.first())
                }
                String[] target = request.parameterMap.get("target")
                if (target) {
                    request.session.setAttribute(LoginController.LAST_TARGET_KEY, target.first())
                }
                super.onAuthenticationFailure(request, response, exception)
            }
        }
        failureHandler.defaultFailureUrl = "/login/authfail"

        http
                .csrf { csrf ->
                    csrf.disable()
                }
                .exceptionHandling { exceptionHandling ->
                    exceptionHandling
                            .accessDeniedPage("/error/error403")
                            .authenticationEntryPoint(new ParameterAuthenticationEntryPoint("/login"))
                }
                .anonymous { withDefaults() }
                .formLogin { formLogin ->
                    formLogin
                            .loginPage("/login").permitAll()
                            .loginProcessingUrl("/authenticate").permitAll()
                            .successHandler(successHandler)
                            .failureHandler(failureHandler)
                }
                .logout { logout ->
                    logout
                            .logoutSuccessUrl("/")
                            .invalidateHttpSession(true)
                            .clearAuthentication(true)
                }
        return http.build()
    }
}
