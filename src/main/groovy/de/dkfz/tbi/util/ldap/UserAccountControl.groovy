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
package de.dkfz.tbi.util.ldap

import groovy.transform.TupleConstructor

/**
 * Names and associated masks for the LDAP value userAccountControl
 *
 * see: https://support.microsoft.com/de-de/help/305144/how-to-use-useraccountcontrol-to-manipulate-user-account-properties
 */
@TupleConstructor
enum UserAccountControl {
    SCRIPT                         (0b00000000000000000000000000000001),
    ACCOUNTDISABLE                 (0b00000000000000000000000000000010),
    // UNUSED                      (0b00000000000000000000000000000100),
    HOMEDIR_REQUIRED               (0b00000000000000000000000000001000),
    LOCKOUT                        (0b00000000000000000000000000010000),
    PASSWD_NOTREQD                 (0b00000000000000000000000000100000),
    PASSWD_CANT_CHANGE             (0b00000000000000000000000001000000),
    ENCRYPTED_TEXT_PWD_ALLOWED     (0b00000000000000000000000010000000),
    TEMP_DUPLICATE_ACCOUNT         (0b00000000000000000000000100000000),
    NORMAL_ACCOUNT                 (0b00000000000000000000001000000000),
    // UNUSED                      (0b00000000000000000000010000000000),
    INTERDOMAIN_TRUST_ACCOUNT      (0b00000000000000000000100000000000),
    WORKSTATION_TRUST_ACCOUNT      (0b00000000000000000001000000000000),
    SERVER_TRUST_ACCOUNT           (0b00000000000000000010000000000000),
    // UNUSED                      (0b00000000000000000100000000000000),
    // UNUSED                      (0b00000000000000001000000000000000),
    DONT_EXPIRE_PASSWORD           (0b00000000000000010000000000000000),
    MNS_LOGON_ACCOUNT              (0b00000000000000100000000000000000),
    SMARTCARD_REQUIRED             (0b00000000000001000000000000000000),
    TRUSTED_FOR_DELEGATION         (0b00000000000010000000000000000000),
    NOT_DELEGATED                  (0b00000000000100000000000000000000),
    USE_DES_KEY_ONLY               (0b00000000001000000000000000000000),
    DONT_REQ_PREAUTH               (0b00000000010000000000000000000000),
    PASSWORD_EXPIRED               (0b00000000100000000000000000000000),
    TRUSTED_TO_AUTH_FOR_DELEGATION (0b00000001000000000000000000000000),
    // UNUSED                      (0b00000010000000000000000000000000),
    PARTIAL_SECRETS_ACCOUNT        (0b00000100000000000000000000000000),
    // UNUSED                      (0b00001000000000000000000000000000),
    // UNUSED                      (0b00010000000000000000000000000000),
    // UNUSED                      (0b00100000000000000000000000000000),
    // UNUSED                      (0b01000000000000000000000000000000),
    // UNUSED                      (0b10000000000000000000000000000000),

    int bitMask

    static boolean isSet(UserAccountControl field, int value) {
        return (field.bitMask & value) == field.bitMask
    }
}
