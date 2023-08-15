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

$(() => {
  'use strict';

  // Automatically check boxes with class set-for-ROLE and
  // auto check and disable boxes with class set-and-block-for-ROLE
  $('.project-role-select').on('change', (e) => {
    const roles = ['PI', 'BIOINFORMATICIAN', 'LEAD_BIOINFORMATICIAN', 'COORDINATOR'];
    const hiddenCheckboxPrefix = 'hidden-checkbox';

    const setForMap =
      roles.map((role) => ({
        role,
        cssClass: `.set-for-${role}`,
        boxes: $(e.target).parent().parent().parent()
          .find(`.set-for-${role}`)
      }));

    const setAndBlockMap = roles.map((role) => ({
      role,
      cssClass: `.set-and-block-for-${role}`,
      boxes: $(e.target).parent().parent().parent()
        .find(`.set-and-block-for-${role}`)
    }));

    // remove disabled and checked from checkboxes
    setAndBlockMap.forEach((map) => {
      map.boxes.prop('disabled', false);
      map.boxes.prop('checked', false);
    });

    // remove hidden checkboxes
    setAndBlockMap.forEach((map) => {
      map.boxes.each((index, box) => {
        const hiddenCheckbox = $(box).parent().find(`#${hiddenCheckboxPrefix}-${box.name}`);
        if (hiddenCheckbox) {
          hiddenCheckbox.remove();
        }
      });
    });

    $(e.target).find('option:selected').each(function () {
      setForMap.forEach((map) => {
        map.boxes.prop('checked', false);

        if (this.text === map.role) {
          map.boxes.prop('checked', true);
        }
      });

      setAndBlockMap.forEach((map) => {
        if (this.text === map.role) {
          map.boxes.prop('checked', true);
          map.boxes.prop('disabled', true);
          // create hidden element if it doesn't exist already
          map.boxes.each((index, box) => {
            let hiddenCheckbox = $(box).parent().find(`#${hiddenCheckboxPrefix}-${box.name}`);
            if (hiddenCheckbox.length === 0) {
              hiddenCheckbox = document.createElement('input');
              hiddenCheckbox.hidden = true;
              hiddenCheckbox.type = 'checkbox';
              hiddenCheckbox.name = box.name;
              hiddenCheckbox.checked = box.checked;
              hiddenCheckbox.id = `${hiddenCheckboxPrefix}-${box.name}`;
              box.parentNode.appendChild(hiddenCheckbox);
            }
          });
        }
      });
    });
  }).trigger('change');
});
