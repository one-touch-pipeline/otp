/*
 * Copyright 2011-2019 The OTP authors
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
package de.dkfz.tbi.otp.administration

import grails.gorm.transactions.Transactional
import groovy.transform.Immutable
import org.springframework.beans.factory.InitializingBean
import org.springframework.ldap.core.AttributesMapper
import org.springframework.ldap.core.LdapTemplate
import org.springframework.ldap.core.support.LdapContextSource
import org.springframework.ldap.query.ContainerCriteria

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.security.User

import javax.naming.NamingException
import javax.naming.directory.Attributes
import java.util.regex.Matcher

import static org.springframework.ldap.query.LdapQueryBuilder.query

@SuppressWarnings(["ExplicitCallToAndMethod", "ExplicitCallToOrMethod"])
@Transactional
class LdapService implements InitializingBean {

    ConfigService configService
    private LdapTemplate ldapTemplate

    @Override
    void afterPropertiesSet() {
        LdapContextSource ldapContextSource = new LdapContextSource()
        ldapTemplate = new LdapTemplate(ldapContextSource)

        ldapContextSource.setUrl(configService.getLdapServer())
        ldapContextSource.setBase(configService.getLdapSearchBase())
        ldapContextSource.setUserDn(configService.getLdapManagerDistinguishedName())
        ldapContextSource.setPassword(configService.getLdapManagerPassword())
        ldapContextSource.afterPropertiesSet()

        ldapTemplate.setIgnorePartialResultException(true)
    }

    LdapUserDetails getLdapUserDetailsByUsername(String username) {
        if (username == null) {
            return null
        }
        return ldapTemplate.search(
                query().where(LdapKey.OBJECT_CATEGORY).is(LdapKey.USER)
                        .and(configService.ldapSearchAttribute).is(username),
                new LdapUserDetailsAttributesMapper())[0]
    }

    List<LdapUserDetails> getListOfLdapUserDetailsByUsernameOrMailOrRealName(String searchString, int countLimit = 0) {
        if (searchString == null) {
            return []
        }
        String sanitizedSearchString = searchString.trim().replaceAll(" +", " ")

        String wildcardedSearch = "*${sanitizedSearchString}*"
        ContainerCriteria dynamicQuery = query()
                .where(configService.ldapSearchAttribute).like(wildcardedSearch)
                .or(LdapKey.MAIL).like(wildcardedSearch)
                .or(LdapKey.GIVEN_NAME).like(wildcardedSearch)
                .or(LdapKey.SURNAME).like(wildcardedSearch)

        if (sanitizedSearchString.contains(" ")) {
            String[] splitSearch = sanitizedSearchString.split(" ", 2)
            dynamicQuery = query()
                    .where(LdapKey.GIVEN_NAME).like("*${splitSearch[0]}*")
                    .and(LdapKey.SURNAME).like("*${splitSearch[1]}*")
        }

        return ldapTemplate.search(
                query().countLimit(countLimit)
                        .attributes(configService.ldapSearchAttribute, LdapKey.MAIL, LdapKey.GIVEN_NAME, LdapKey.SURNAME, LdapKey.DEPARTMENT)
                        .where(LdapKey.OBJECT_CATEGORY).is(LdapKey.PERSON)
                        .and(LdapKey.MAIL).isPresent()
                        .and(dynamicQuery),
                new LdapUserDetailsAttributesMapper())
    }

    String getDistinguishedNameOfGroupByGroupName(String groupName) {
        if (groupName == null) {
            return ""
        }
        return ldapTemplate.search(
                query().where(LdapKey.OBJECT_CATEGORY).is(LdapKey.GROUP)
                        .and(configService.ldapSearchAttribute).is(groupName),
                new DistinguishedNameAttributesMapper())[0]
    }

    List<String> getGroupMembersByDistinguishedName(String distinguishedName) {
        if (distinguishedName == null) {
            return []
        }
        // nested group memberships are not resolved
        return ldapTemplate.search(
                query().where(LdapKey.OBJECT_CATEGORY).is(LdapKey.USER)
                        .and(LdapKey.MEMBER_OF).is(distinguishedName),
                new UsernameAttributesMapper())
    }

    List<String> getGroupsOfUserByUsername(String username) {
        if (username == null) {
            return []
        }
        return ldapTemplate.search(
                query().where(LdapKey.OBJECT_CATEGORY).is(LdapKey.USER)
                        .and(configService.ldapSearchAttribute).is(username),
                new MemberOfAttributesMapper())[0]
    }

    boolean existsInLdap(User user) {
        ContainerCriteria query = query()
                .attributes(configService.ldapSearchAttribute)
                .where(LdapKey.OBJECT_CATEGORY).is(LdapKey.USER)
                .and(configService.ldapSearchAttribute).is(user.username)
        return ldapTemplate.search(query, new DistinguishedNameAttributesMapper()).size() >= 1
    }
}

class LdapUserDetailsAttributesMapper implements AttributesMapper<LdapUserDetails> {
    @Override
    LdapUserDetails mapFromAttributes(Attributes a) throws NamingException {
        List<String> memberOfList = a.get(LdapKey.MEMBER_OF)?.getAll()?.collect {
            Matcher matcher = it =~ /CN=(?<group>[^,]*),.*/
            if (matcher.matches() && matcher.group("group")) {
                return matcher.group("group")
            }
        }
        long accountExpires = (a.get(LdapKey.ACCOUNT_EXPIRES)?.get()?.toString()?.toLong()) ?: 0
        boolean deactivated = convertAdTimestampToUnixTimestampInMs(accountExpires) < new Date().getTime()
        String givenName = a.get(LdapKey.GIVEN_NAME)?.get()
        String sn = a.get(LdapKey.SURNAME)?.get()
        boolean realNameCreatable = givenName && sn
        return new LdapUserDetails([
                username         : a.get(ConfigService.getInstance().getLdapSearchAttribute())?.get()?.toString(),
                realName         : realNameCreatable ? "${givenName} ${sn}" : null,
                mail             : a.get(LdapKey.MAIL)?.get()?.toString(),
                department       : a.get(LdapKey.DEPARTMENT)?.get()?.toString(),
                thumbnailPhoto   : a.get(LdapKey.THUMBNAIL_PHOTO)?.get() as byte[],
                deactivated      : deactivated,
                memberOfGroupList: memberOfList,
        ])
    }

    private static long convertAdTimestampToUnixTimestampInMs(long windowsEpoch) {
        // http://meinit.nl/convert-active-directory-lastlogon-time-to-unix-readable-time
        // in milliseconds because Date.getTime() also returns milliseconds
        return ((windowsEpoch / 10000000) - 11644473600) * 1000
    }
}

class DistinguishedNameAttributesMapper implements AttributesMapper<String> {
    @Override
    String mapFromAttributes(Attributes a) throws NamingException {
        return a.get(LdapKey.DISTINGUISHED_NAME)?.get()?.toString()
    }
}

class UsernameAttributesMapper implements AttributesMapper<String> {
    @Override
    String mapFromAttributes(Attributes a) throws NamingException {
        return a.get(ConfigService.getInstance().getLdapSearchAttribute())?.get()?.toString()
    }
}

class MemberOfAttributesMapper implements AttributesMapper<List<String>> {
    @Override
    List<String> mapFromAttributes(Attributes a) throws NamingException {
        return a.get(LdapKey.MEMBER_OF)?.getAll()?.collect {
            Matcher matcher = it =~ /CN=(?<group>[^,]*),.*/
            if (matcher.matches() && matcher.group("group")) {
                return matcher.group("group")
            }
        }
    }
}

@Immutable
class LdapUserDetails {
    String username
    String realName
    String mail
    String department
    byte[] thumbnailPhoto
    boolean deactivated
    List<String> memberOfGroupList
}
