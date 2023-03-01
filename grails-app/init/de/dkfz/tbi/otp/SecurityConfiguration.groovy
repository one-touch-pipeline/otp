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

import org.springframework.beans.factory.annotation.Autowired
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
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.*
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.*
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter
import org.springframework.web.reactive.function.client.WebClient

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.security.user.identityProvider.*

import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import static org.springframework.security.config.Customizer.withDefaults

@Configuration
@EnableWebSecurity
@EnableMethodSecurity()
class SecurityConfiguration {

    @Autowired
    ConfigService configService

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

    @Autowired
    KeycloakService keycloakService

    @Autowired
    LdapService ldapService

    @Bean
    IdentityProvider identityProvider() {
        if (configService.oidcEnabled) {
            return keycloakService
        }
        return ldapService
    }

    @Bean
    @SuppressWarnings('Indentation')
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
                .authorizeRequests { authorize ->
                    if (configService.consoleEnabled) {
                        authorize.mvcMatchers(
                                "/console/**",
                                "/static/console*/**",
                        ).access("hasRole('ROLE_ADMIN') and @dicomAuditConsoleHandler.log()")
                    } else {
                        authorize.mvcMatchers(
                                "/console/**",
                                "/static/console*/**",
                        ).denyAll()
                    }
                    authorize
                            .mvcMatchers(
                                    "/adminSeed/**",
                                    "/plugins/**",
                            ).denyAll()
                            .mvcMatchers(
                                    "/impersonate",
                            ).hasRole("SWITCH_USER")
                            .mvcMatchers(
                                    "/",
                                    "/login/**",
                                    "/info/about",
                                    "/info/numbers",
                                    "/info/contact",
                                    "/info/imprint",
                                    "/info/partners",
                                    "/info/faq",
                                    "/info/newsBanner",
                                    "/statistic/projectCountPerDate",
                                    "/statistic/laneCountPerDate",
                                    "/privacyPolicy/**",
                                    "/metadataImport/autoImport",
                                    "/error",
                                    "/error/**",
                            ).permitAll()
                            .anyRequest().fullyAuthenticated()
                }
                .logout { logout ->
                    logout
                            .logoutSuccessUrl("/")
                            .invalidateHttpSession(true)
                            .clearAuthentication(true)
                }
                .oauth2Client(withDefaults())
        return http.build()
    }

    @Bean
    ReactiveClientRegistrationRepository clientRegistrations() {
        ClientRegistration keycloakRegistration = ClientRegistration
                .withRegistrationId(KeycloakService.CLIENT_REGISTRATION_ID)
                .tokenUri("${configService.keycloakServer}/realms/otp-dev/protocol/openid-connect/token")
                .clientId(configService.keycloakClientId)
                .clientSecret(configService.keycloakClientSecret)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .build()
        ClientRegistration wesRegistration = ClientRegistration
                .withRegistrationId("wes")
                .tokenUri(configService.wesAuthBaseUrl)
                .clientId(configService.wesAuthClientId)
                .clientSecret(configService.wesAuthClientSecret)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .build()
        return new InMemoryReactiveClientRegistrationRepository(keycloakRegistration, wesRegistration)
    }

    @Bean
    @SuppressWarnings("DoNotCreateServicesWithNew")
    WebClient webClient(ReactiveClientRegistrationRepository clientRegistrations) {
        InMemoryReactiveOAuth2AuthorizedClientService clientService = new InMemoryReactiveOAuth2AuthorizedClientService(clientRegistrations)
        AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager authorizedClientManager =
                new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(clientRegistrations, clientService)
        ServerOAuth2AuthorizedClientExchangeFilterFunction oauth = new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)

        return WebClient.builder()
                .filter(oauth)
                .build()
    }
}
