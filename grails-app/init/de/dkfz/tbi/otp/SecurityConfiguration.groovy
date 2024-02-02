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

import org.springframework.beans.factory.annotation.Autowired
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
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.ldap.DefaultSpringSecurityContextSource
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler
import org.springframework.security.oauth2.client.registration.*
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.oidc.*
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.*
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter
import org.springframework.web.reactive.function.client.WebClient

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.security.user.UserService
import de.dkfz.tbi.otp.security.user.identityProvider.*
import de.dkfz.tbi.otp.utils.SessionUtils
import de.dkfz.tbi.otp.workflowExecution.wes.WeskitAuthService

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

    @Autowired
    KeycloakService keycloakService

    @Autowired
    LdapService ldapService

    @Autowired
    SecurityService securityService

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
        return new DefaultSpringSecurityContextSource("${configService.ldapServer}/${configService.ldapSearchBase}")
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
        return { web -> web.ignoring().antMatchers("/assets/**") }
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
    IdentityProvider identityProvider() {
        if (configService.oidcEnabled) {
            return keycloakService
        }
        return ldapService
    }

    @Bean
    @SuppressWarnings('Indentation')
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf { csrf ->
                    csrf.disable()
                }
                .oauth2Client(withDefaults())
                .authorizeRequests { authorize ->
                    if (configService.consoleEnabled) {
                        authorize.mvcMatchers(
                                "/console/**",
                                "/static/console*/**",
                        ).access("hasRole('ROLE_ADMIN')")
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
                                    "/auth/**",
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
                                    "/grails-errorhandler/**",
                                    "/error/**",
                                    "/webjars/chart.js/**",
                                    "/webjars/chartjs-plugin-datalabels/**",
                                    "/webjars/datatables-plugins/**",
                                    "/webjars/datatables/**",
                                    "/webjars/jquery/**",
                                    "/webjars/datatables.net-scroller/**",
                                    "/webjars/datatables-buttons/**",
                            ).permitAll()
                            .anyRequest().fullyAuthenticated()
                }

        if (configService.oidcEnabled) {
            http
                    .oauth2Login(withDefaults())
                    .logout { logout ->
                        logout.logoutSuccessHandler(oidcLogoutSuccessHandler())
                    }
        } else {
            http
                    .formLogin { formLogin ->
                        formLogin
                                .loginPage("/").permitAll()
                                .loginProcessingUrl("/authenticate").permitAll()
                                .successHandler(ldapLoginSuccessHandler)
                                .failureHandler(ldapLoginFailureHandler)
                    }
                    .exceptionHandling { exceptionHandling ->
                        exceptionHandling
                                .accessDeniedPage("/error/error403")
                                .authenticationEntryPoint(new ParameterAuthenticationEntryPoint("/auth/login"))
                    }
                    .anonymous { withDefaults() }
                    .logout { logout ->
                        logout
                                .logoutSuccessUrl("/")
                                .invalidateHttpSession(true)
                                .clearAuthentication(true)
                    }
        }

        return http.build()
    }

    @Bean
    GrantedAuthoritiesMapper userAuthoritiesMapper(UserService userService) {
        return { authorities ->
            Set<GrantedAuthority> mappedAuthorities = [] as HashSet

            authorities.forEach { authority ->
                if (OidcUserAuthority.isInstance(authority)) {
                    OidcUserAuthority oidcUserAuthority = (OidcUserAuthority)authority

                    OidcIdToken idToken = oidcUserAuthority.idToken

                    SessionUtils.withNewSession {
                        User user = userService.findOrCreateUserWithLdapData(idToken.preferredUsername)

                        user.authorities.each { Role role ->
                                mappedAuthorities.add(new SimpleGrantedAuthority(role.authority))
                        }

                        if (keycloakService.isUserDeactivated(user)) {
                            throw new DisabledException("User is disabled.")
                        }
                    }
                }
            }

            return mappedAuthorities
        }
    }

    @Bean
    ReactiveClientRegistrationRepository clientRegistrations() {
        List<ClientRegistration> clientRegistrations = [
                keycloakApiClientRegistration(),
                wesClientRegistration(),
        ]

        return new InMemoryReactiveClientRegistrationRepository(clientRegistrations)
    }

    @Bean
    ClientRegistrationRepository loginClientRegistrationRepository() {
        return new InMemoryClientRegistrationRepository(keycloakLoginClientRegistration())
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

    private ClientRegistration keycloakLoginClientRegistration() {
        return ClientRegistration
                .withRegistrationId("keycloakLogin")
                .clientId(configService.oidcClientId)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(configService.oidcRedirectUri)
                .scope("openid", "profile", "email")
                .authorizationUri("${configService.keycloakServer}/realms/${configService.keycloakRealm}/protocol/openid-connect/auth")
                .tokenUri("${configService.keycloakServer}/realms/${configService.keycloakRealm}/protocol/openid-connect/token")
                .userInfoUri("${configService.keycloakServer}/realms/${configService.keycloakRealm}/protocol/openid-connect/userinfo")
                .jwkSetUri("${configService.keycloakServer}/realms/${configService.keycloakRealm}/protocol/openid-connect/certs")
                .issuerUri("${configService.keycloakServer}/realms/${configService.keycloakRealm}")
                .userNameAttributeName(IdTokenClaimNames.SUB)
                .build()
    }

    private ClientRegistration keycloakApiClientRegistration() {
        return ClientRegistration
                .withRegistrationId(KeycloakService.CLIENT_REGISTRATION_ID)
                .tokenUri("${configService.keycloakServer}/realms/${configService.keycloakRealm}/protocol/openid-connect/token")
                .clientId(configService.keycloakClientId)
                .clientSecret(configService.keycloakClientSecret)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .build()
    }

    private ClientRegistration wesClientRegistration() {
        return ClientRegistration
                .withRegistrationId(WeskitAuthService.CLIENT_REGISTRATION_ID)
                .tokenUri(configService.wesAuthTokenUri)
                .clientId(configService.wesAuthClientId)
                .clientSecret(configService.wesAuthClientSecret)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .build()
    }

    AuthenticationSuccessHandler getLdapLoginSuccessHandler() {
        AuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler()
        successHandler.defaultTargetUrl = "/home/index"
        successHandler.targetUrlParameter = ParameterAuthenticationEntryPoint.TARGET_PARAM_NAME
        return successHandler
    }

    AuthenticationFailureHandler getLdapLoginFailureHandler() {
        AuthenticationFailureHandler failureHandler = new SimpleUrlAuthenticationFailureHandler() {
            @Override
            void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                         AuthenticationException exception) throws IOException, ServletException {
                String[] username = request.parameterMap.get("username")
                if (username) {
                    request.session.setAttribute(AuthController.LAST_USERNAME_KEY, username.first())
                }
                String[] target = request.parameterMap.get("target")
                if (target) {
                    request.session.setAttribute(AuthController.LAST_TARGET_KEY, target.first())
                }
                super.onAuthenticationFailure(request, response, exception)
            }
        }
        failureHandler.defaultFailureUrl = "/auth/authfail"
        return failureHandler
    }

    private LogoutSuccessHandler oidcLogoutSuccessHandler() {
        OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler =
                new OidcClientInitiatedLogoutSuccessHandler(loginClientRegistrationRepository())
        oidcLogoutSuccessHandler.postLogoutRedirectUri = "{baseUrl}"
        return oidcLogoutSuccessHandler
    }
}
