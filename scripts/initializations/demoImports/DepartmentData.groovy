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
import de.dkfz.tbi.otp.security.user.*
import de.dkfz.tbi.otp.utils.CollectionUtils

/**
 * Script to create example Data for Departments with department heads.
 */

DepartmentService departmentService = ctx.departmentService
DeputyRelationService deputyRelationService = ctx.deputyRelationService

User otp = CollectionUtils.exactlyOneElement(User.findAllByUsername('otp'))
User dave = CollectionUtils.exactlyOneElement(User.findAllByUsername('dave')) //has one Deputy
User goofy = CollectionUtils.exactlyOneElement(User.findAllByUsername('goofy')) //has multiple Deputies
User ralf = CollectionUtils.exactlyOneElement(User.findAllByUsername('ralf')) //has same Deps as goofy but one more

User mufasa = CollectionUtils.exactlyOneElement(User.findAllByUsername('mufasa')) //Deputy of dave
User sarabi = CollectionUtils.exactlyOneElement(User.findAllByUsername('sarabi')) //Deputy of goofy and ralf
User simba = CollectionUtils.exactlyOneElement(User.findAllByUsername('simba')) //Deputy of goofy and ralf
User nala = CollectionUtils.exactlyOneElement(User.findAllByUsername('nala')) //Deputy of ralf


departmentService.createDepartment([ouNumber       : '123',
                                        costCenter     : 'No PI no Deputy',
                                        departmentHeads: [],
] as DepartmentCommand)
departmentService.createDepartment([ouNumber       : '1234',
                                        costCenter     : 'One PI no Deputy',
                                        departmentHeads: [otp],
] as DepartmentCommand)
departmentService.createDepartment([ouNumber       : '12345',
                                        costCenter     : 'Two PIs one with one without Deputy',
                                        departmentHeads: [otp, dave],
] as DepartmentCommand)
departmentService.createDepartment([ouNumber       : '123456',
                                        costCenter     : 'Two PIs both with Deputies',
                                        departmentHeads: [dave, goofy],
] as DepartmentCommand)
departmentService.createDepartment([ouNumber       : '1234567',
                                        costCenter     : 'Two PIs both with overlapping Deputies',
                                        departmentHeads: [goofy, ralf],
] as DepartmentCommand)

deputyRelationService.grantDepartmentDeputyRights(dave, mufasa.username)
deputyRelationService.grantDepartmentDeputyRights(goofy, sarabi.username)
deputyRelationService.grantDepartmentDeputyRights(ralf, sarabi.username)
deputyRelationService.grantDepartmentDeputyRights(goofy, simba.username)
deputyRelationService.grantDepartmentDeputyRights(ralf, simba.username)
deputyRelationService.grantDepartmentDeputyRights(ralf, nala.username)