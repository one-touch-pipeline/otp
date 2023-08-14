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
package de.dkfz.tbi.otp.security.user.identityProvider

import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import groovy.transform.ToString
import org.apache.commons.lang.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.user.identityProvider.data.IdpUserDetails
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.util.ldap.UserAccountControl

import java.nio.charset.StandardCharsets

@Transactional
class KeycloakService implements IdentityProvider {

    static final String CLIENT_REGISTRATION_ID = "keycloak"

    @Autowired
    GrailsApplication grailsApplication

    @Autowired
    ConfigService configService

    @Autowired
    WebClient webClient

    @Override
    IdpUserDetails getIdpUserDetailsByUsername(String username) {
        KeycloakUser user = fetchKeycloakUserByExactUsername(username)
        return user ? castKeycloakUserIntoIdpUserDetails(user) : null
    }

    @Override
    List<IdpUserDetails> getIdpUserDetailsByUserList(List<User> otpUsers) {
        String requestUrl = "$apiBaseUrl/${configService.keycloakRealm}/users?max=1000000"
        List<KeycloakUser> keycloakUsers = this.<KeycloakUser>getFromKeycloakApi(requestUrl, keycloakUserTypeReference)
        List<String> otpUsernames = otpUsers*.username
        keycloakUsers.removeAll { !otpUsernames.contains(it.username) }
        return this.castKeycloakUsersIntoIdpUserDetails(keycloakUsers)
    }

    @Override
    List<IdpUserDetails> getListOfIdpUserDetailsBySearchString(String searchString) {
        String requestUrl = "$apiBaseUrl/${configService.keycloakRealm}/users?search=$searchString"
        List<KeycloakUser> keycloakUsers = this.<KeycloakUser>getFromKeycloakApi(requestUrl, keycloakUserTypeReference)
        return this.castKeycloakUsersIntoIdpUserDetails(keycloakUsers)
    }

    private String getGroupIdByGroupName(String groupName) {
        if (groupName == null) {
            return ""
        }

        String requestUrl = "$apiBaseUrl/${configService.keycloakRealm}/groups?search=$groupName"
        List<KeycloakGroup> keycloakGroups = this.<KeycloakGroup>getFromKeycloakApi(requestUrl, keycloakGroupTypeReference)
        return keycloakGroups ? CollectionUtils.atMostOneElement(keycloakGroups).id : ""
    }

    private List<String> getGroupMembersByGroupId(String groupId) {
        if (!groupId) {
            return []
        }

        String requestUrl = "$apiBaseUrl/${configService.keycloakRealm}/groups/$groupId/members"
        List<KeycloakUser> keycloakUsersInGroup = this.<KeycloakUser>getFromKeycloakApi(requestUrl, keycloakUserTypeReference)
        return keycloakUsersInGroup*.username
    }

    @Override
    List<String> getGroupMembersByGroupName(String groupName) {
        String groupId = getGroupIdByGroupName(groupName)
        return new ArrayList<String>(getGroupMembersByGroupId(groupId))
    }

    @Override
    List<String> getGroupsOfUser(User user) {
        return new ArrayList<String>(getIdpUserDetailsByUsername(user.username).memberOfGroupList ?: [])
    }

    @Override
    boolean exists(User user) {
        if (!user.username) {
            return false
        }
        try {
            fetchKeycloakUserByExactUsername(user.username)
            return true
        } catch (NoSuchElementException e) {
            return false
        }
    }

    @Override
    Map<String, String> getAllUserAttributes(User user) {
        KeycloakUser keycloakUser = fetchKeycloakUserByExactUsername(user.username)

        if (!keycloakUser.attributes) {
            return [:]
        }

        return keycloakUser.attributes.properties + keycloakUser.properties.findAll { it.key != "attributes" } as Map<String, String>
    }

    @Override
    boolean isUserDeactivated(User user) {
        if (!user.username) {
            return true
        }

        if (!user.enabled) {
            return true
        }

        return getIdpUserDetailsByUsername(user.username).deactivated || isUserDeactivatedInFederatedLdap(user)
    }

    @Override
    boolean isUserInIdpAndActivated(User user) {
        return !isUserDeactivated(user)
    }

    @Override
    Integer getUserAccountControlOfUser(User user) {
        if (!user) {
            return null
        }

        KeycloakUser keycloakUser = fetchKeycloakUserByExactUsername(user.username)
        return keycloakUser.attributes && keycloakUser.attributes.userAccountControl ? keycloakUser.attributes.userAccountControl.first() : null
    }

    @Override
    Map<UserAccountControl, Boolean> getAllUserAccountControlFlagsOfUser(User user) {
        Integer value = getUserAccountControlOfUser(user)
        if (value == null) {
            return [:]
        }
        return UserAccountControl.values().collectEntries { UserAccountControl field ->
            [(field): UserAccountControl.isSet(field, value)]
        }
    }

    @Override
    String getLogoutUri() {
        String serverUrl = grailsApplication.config.getProperty("grails.serverURL")
        String postLogoutRedirectUrl = URLEncoder.encode("${serverUrl}/logout", StandardCharsets.UTF_8.toString())

        return "${configService.keycloakServer}/realms/${configService.keycloakRealm}/protocol/openid-connect/logout" +
                "?post_logout_redirect_uri=${postLogoutRedirectUrl}" +
                "&client_id=${configService.oidcClientId}"
    }

    private String getApiBaseUrl() {
        return "${configService.keycloakServer}/admin/realms"
    }

    private KeycloakUser fetchKeycloakUserByExactUsername(String username) {
        String requestUrl = "$apiBaseUrl/${configService.keycloakRealm}/users?username=$username&exact=true"
        List<KeycloakUser> users = this.<KeycloakUser>getFromKeycloakApi(requestUrl, keycloakUserTypeReference)
        return users.size() > 0 ? users.first() : null
    }

    @CompileDynamic
    private <T> List<T> getFromKeycloakApi(String requestUrl, ParameterizedTypeReference typeRef) {
        return webClient.get()
                .uri(requestUrl)
                .attributes(ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId(CLIENT_REGISTRATION_ID))
                .retrieve()
                .bodyToMono(typeRef)
                .block() as List<T>
    }

    private ParameterizedTypeReference getKeycloakUserTypeReference() {
        new ParameterizedTypeReference<List<KeycloakUser>>() { }
    }

    private ParameterizedTypeReference getKeycloakGroupTypeReference() {
        new ParameterizedTypeReference<List<KeycloakGroup>>() { }
    }

    private List<IdpUserDetails> castKeycloakUsersIntoIdpUserDetails(List<KeycloakUser> keycloakUsers) {
        return keycloakUsers.collect { castKeycloakUserIntoIdpUserDetails(it) }
    }

    private IdpUserDetails castKeycloakUserIntoIdpUserDetails(KeycloakUser keycloakUser) {
        String realName = keycloakUser.firstName && keycloakUser.lastName ? "${keycloakUser.firstName} ${keycloakUser.lastName}" : keycloakUser.username
        String department = keycloakUser.attributes && keycloakUser.attributes.department ? keycloakUser.attributes.department.first() : ""
        String thumbnailPhoto = keycloakUser.attributes && keycloakUser.attributes.thumbnailPhoto ? keycloakUser.attributes.thumbnailPhoto.first() : ""
        List<String> memberOfGroupList = keycloakUser.attributes && keycloakUser.attributes.memberOf ? keycloakUser.attributes.memberOf : []

        return new IdpUserDetails([
                username         : keycloakUser.username,
                realName         : realName,
                mail             : keycloakUser.email,
                department       : department,
                thumbnailPhoto   : thumbnailPhoto,
                deactivated      : !keycloakUser.enabled,
                memberOfGroupList: memberOfGroupList,
        ])
    }

    private boolean isUserDeactivatedInFederatedLdap(User user) {
        Integer uaControl = getUserAccountControlOfUser(user)
        return uaControl ? UserAccountControl.isSet(UserAccountControl.ACCOUNTDISABLE, uaControl) : false
    }
}

@ToString
class KeycloakUser {
    String id
    String username
    String firstName
    String lastName
    String email
    boolean enabled
    long createdTimestamp
    boolean totp
    boolean emailVerified
    String federationLink
    KeycloakUserAttributes attributes
    List disableableCredentialTypes
    List requiredActions
    int notBefore
    Object access
}

@ToString
class KeycloakGroup {
    String id
    String name
    String path
    List<KeycloakGroup> subGroups
}

@ToString
@SuppressWarnings("PropertyName")
class KeycloakUserAttributes {
    List<String> LDAP_ENTRY_DN
    List<String> department
    List<String> LDAP_ID
    List<String> modifyTimestamp
    List<String> createTimestamp
    List<String> thumbnailPhoto
    List<String> memberOf
    List<Integer> userAccountControl

    void setMemberOf(List<String> memberOf) {
        this.memberOf = memberOf.collect { StringUtils.substringBetween(it, "CN=", ",OU") }
    }
}
