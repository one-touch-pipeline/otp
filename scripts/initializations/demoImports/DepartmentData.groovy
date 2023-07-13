/*
 * Copyright 2011-2023 The OTP authors
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

import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.user.DepartmentCommand
import de.dkfz.tbi.otp.security.user.DepartmentService
import de.dkfz.tbi.otp.utils.CollectionUtils

/**
 * Script to create example Data for Departments with department heads.
 */

DepartmentService departmentService = ctx.departmentService

User otp = CollectionUtils.exactlyOneElement(User.findAllByUsername('otp'))
User dave = CollectionUtils.exactlyOneElement(User.findAllByUsername('dave'))
departmentService.createDepartment([ouNumber       : '123',
                                    costCenter     : ' cost_center',
                                    departmentHeads: [otp, dave],
] as DepartmentCommand)