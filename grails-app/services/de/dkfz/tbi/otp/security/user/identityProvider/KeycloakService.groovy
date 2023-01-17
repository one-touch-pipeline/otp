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

import grails.gorm.transactions.Transactional
import org.apache.commons.lang.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.*
import org.springframework.security.oauth2.client.OAuth2RestTemplate
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails
import org.springframework.security.oauth2.common.OAuth2AccessToken
import org.springframework.web.client.RestTemplate

import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.user.identityProvider.data.IdpUserDetails
import de.dkfz.tbi.util.ldap.UserAccountControl

@Transactional
class KeycloakService implements IdentityProvider {

    @Autowired
    ConfigService configService

    private final RestTemplate restTemplate = new RestTemplate()

    private OAuth2AccessToken accessToken

    @Override
    IdpUserDetails getIdpUserDetailsByUsername(String username) {
        return castKeycloakUserIntoIdpUserDetails(getKeycloakUserByExactUsername(username))
    }

    @Override
    List<IdpUserDetails> getIdpUserDetailsByUserList(List<User> otpUsers) {
        String requestUrl = "$apiBaseUrl/${configService.keycloakRealm}/users?max=1000000"
        HttpEntity<String> entity = new HttpEntity<>(null, authorizationHeader)
        ResponseEntity<List<KeycloakUser>> response = restTemplate.exchange(requestUrl, HttpMethod.GET, entity, KeycloakUser[].class)
        List<KeycloakUser> keycloakUsers = response.body
        List<String> otpUsernames = otpUsers*.username
        keycloakUsers.removeAll { !otpUsernames.contains(it.username) }
        return castKeycloakUsersIntoIdpUserDetails(keycloakUsers)
    }

    @Override
    List<IdpUserDetails> getListOfIdpUserDetailsBySearchString(String searchString) {
        String requestUrl = "$apiBaseUrl/${configService.keycloakRealm}/users?search=$searchString"
        HttpEntity<String> entity = new HttpEntity<>(null, authorizationHeader)
        ResponseEntity<List<KeycloakUser>> response = restTemplate.exchange(requestUrl, HttpMethod.GET, entity, KeycloakUser[].class)
        List<KeycloakUser> keycloakUsers = response.body
        return castKeycloakUsersIntoIdpUserDetails(keycloakUsers)
    }

    private String getGroupIdByGroupName(String groupName) {
        if (groupName == null) {
            return ""
        }

        String requestUrl = "$apiBaseUrl/${configService.keycloakRealm}/groups?search=$groupName"
        HttpEntity<String> entity = new HttpEntity<String>(null, authorizationHeader)
        ResponseEntity<List<KeycloakGroup>> response = restTemplate.exchange(requestUrl, HttpMethod.GET, entity, KeycloakGroup[].class)
        List<KeycloakGroup> keycloakGroups = response.body
        return keycloakGroups ? CollectionUtils.atMostOneElement(keycloakGroups).id : ""
    }

    private List<String> getGroupMembersByGroupId(String groupId) {
        if (!groupId) {
            return []
        }

        String requestUrl = "$apiBaseUrl/${configService.keycloakRealm}/groups/$groupId/members"
        HttpEntity<String> entity = new HttpEntity<String>(null, authorizationHeader)
        ResponseEntity<List<KeycloakUser>> response = restTemplate.exchange(requestUrl, HttpMethod.GET, entity, KeycloakUser[].class)
        List<KeycloakUser> keycloakUsersInGroup = response.body
        return keycloakUsersInGroup*.username
    }

    @Override
    List<String> getGroupMembersByGroupName(String groupName) {
        String groupId = getGroupIdByGroupName(groupName)
        return new ArrayList<String>(getGroupMembersByGroupId(groupId))
    }

    @Override
    List<String> getGroupsOfUser(User user) {
        return new ArrayList<String>(getIdpUserDetailsByUsername(user.username).memberOfGroupList)
    }

    @Override
    boolean exists(User user) {
        if (!user.username) {
            return false
        }
        try {
            getKeycloakUserByExactUsername(user.username)
            return true
        } catch (NoSuchElementException e) {
            return false
        }
    }

    @Override
    Map<String, String> getAllUserAttributes(User user) {
        KeycloakUser keycloakUser = getKeycloakUserByExactUsername(user.username)
        return keycloakUser.attributes.properties + keycloakUser.properties.findAll { it.key != "attributes" }
    }

    @Override
    boolean isUserDeactivated(User user) {
        if (!user.username) {
            return true
        }

        return getIdpUserDetailsByUsername(user.username).deactivated
    }

    @Override
    boolean isUserInIdpAndActivated(User user) {
        return !isUserDeactivated(user)
    }

    @Override
    Integer getUserAccountControlOfUser(User user) {
        return getKeycloakUserByExactUsername(user.username).attributes.userAccountControl.first()
    }

    @Override
    Map<UserAccountControl, Boolean> getAllUserAccountControlFlagsOfUser(User user) {
        // TODO: otp-1826
        return [:]
    }

    private String getApiBaseUrl() {
        return "${configService.keycloakServer}/admin/realms"
    }

    /**
     * Get a HTTP header containing a valid Bearer access token. It uses the cached token, if it is still valid. Otherwise
     * the method will request a new access token.
     *
     * @return HttpHeaders with Bearer token
     */
    private HttpHeaders getAuthorizationHeader() {
        if (!accessToken || accessToken.expired) {
            this.accessToken = new OAuth2RestTemplate(oAuthConfigDetails()).accessToken
        }

        HttpHeaders headers = new HttpHeaders()
        headers.set("Authorization", "Bearer " + accessToken.value)
        headers.contentType = MediaType.APPLICATION_JSON_UTF8

        return headers
    }

    /**
     * Generate an oAuth2 configuration which contains all required request parameters to fetch a
     * Bearer token from the otp authorization server.
     *
     * @return ClientCredentialsResourceDetails, which contains the oAuth2 configuration
     */
    private ClientCredentialsResourceDetails oAuthConfigDetails() {
        ClientCredentialsResourceDetails authConfig = new ClientCredentialsResourceDetails()
        authConfig.accessTokenUri = "${configService.keycloakServer}/realms/otp-dev/protocol/openid-connect/token"
        authConfig.clientId = configService.keycloakClientId
        authConfig.clientSecret = configService.keycloakClientSecret
        return authConfig
    }

    private KeycloakUser getKeycloakUserByExactUsername(String username) {
        String requestUrl = "$apiBaseUrl/${configService.keycloakRealm}/users?username=$username&exact=true"
        HttpEntity<String> entity = new HttpEntity<>(null, authorizationHeader)
        ResponseEntity<List<KeycloakUser>> response = restTemplate.exchange(requestUrl, HttpMethod.GET, entity, KeycloakUser[].class)
        KeycloakUser keycloakUser = response.body.first()
        return keycloakUser
    }

    private List<IdpUserDetails> castKeycloakUsersIntoIdpUserDetails(List<KeycloakUser> keycloakUsers) {
        return keycloakUsers.collect { castKeycloakUserIntoIdpUserDetails(it) }
    }

    private IdpUserDetails castKeycloakUserIntoIdpUserDetails(KeycloakUser keycloakUser) {
        return new IdpUserDetails([
                username: keycloakUser.username,
                realName: "$keycloakUser.firstName $keycloakUser.lastName",
                mail: keycloakUser.email,
                department: keycloakUser.attributes.department.first(),
                thumbnailPhoto: keycloakUser.attributes.thumbnailPhoto.first().bytes,
                deactivated: !keycloakUser.enabled,
                memberOfGroupList: keycloakUser.attributes.memberOf,
        ])
    }
}

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

class KeycloakGroup {
    String id
    String name
    String path
    List<KeycloakGroup> subGroups
}

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
