%{--
  - Copyright 2011-2023 The OTP authors
  -
  - Permission is hereby granted, free of charge, to any person obtaining a copy
  - of this software and associated documentation files (the "Software"), to deal
  - in the Software without restriction, including without limitation the rights
  - to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  - copies of the Software, and to permit persons to whom the Software is
  - furnished to do so, subject to the following conditions:
  -
  - The above copyright notice and this permission notice shall be included in all
  - copies or substantial portions of the Software.
  -
  - THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  - IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  - FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  - AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  - LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  - OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  - SOFTWARE.
  --}%

<div class="card">
    <div class="card-header pt-1 pb-1 pointer">
        <g:hiddenField name="piUsers[${index}].projectRequestUser.id" value="${piUser?.projectRequestUser?.id}"/>

        <div class="row align-items-center">
            <a class="col-sm-9" id="user-form-title-${index}" data-bs-toggle="collapse" data-target="#collapse-${index}" aria-expanded="true"
               aria-controls="collapse-${index}">
                New User
            </a>

            <div class="clone-remove-container col-sm-3">
                <button class="clone-remove-2 btn btn-danger user-delete-button" type="button">
                    <i class="bi bi-trash"></i>
                </button>
            </div>
        </div>
    </div>

    <div id="collapse-${index}" class="collapse show" aria-labelledby="heading-${index}" data-parent="#accordion2">
        <g:render template="/templates/piUserFormItem"
                  model="[index                     : index, piUser: piUser, availableRoles: availableRoles, checkboxes: ['otpAccess', 'fileAccess', 'manageUsers'], accordionItem: true,
                          departmentPiFeatureEnabled: departmentPiFeatureEnabled]"/>
    </div>
</div>
