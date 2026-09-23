/*
 * Copyright 2011-2026 The OTP authors
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

describe('Check processingOptions page', () => {
  'use strict';

  before(() => {
    cy.fixture('processingOptionValidators').as('validators');
  });

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAs('operator');
    });

    it('should show the processOption page with the options listed', function () {
      cy.visit('/processingOption/index');

      cy.contains('Processing Options').should('exist');

      for (const validator of Object.values(this.validators)) {
        validator.options.forEach((option) => {
          cy.contains(option).should('exist');
        });
      }
    });

    it('should validate all input field based options', function () {
      cy.visit('/processingOption/index');

      const selector = 'input[type="text"]';

      const toasterSuc = '#otpToastBox .otpSuccessToast';
      const toasterErr = '#otpToastBox .otpErrorToast';

      // random postfix to avoid duplication so the test become idempotent
      const postfix = (Math.random() * 100).toFixed(0);

      for (const [name, validator] of Object.entries(this.validators)) {
        const wrong = validator.test.wrong;
        const right = validator.test.right.replace(new RegExp(validator.test.regex), postfix);

        validator.options.forEach((option) => {
          cy.log('Testing option: ' + option + ' with validator ' + name + ' with wrong value: ' + wrong + ' and right value: ' + right);
          cy.contains(option).parent().within(() => {
            cy.get('i.bi-pencil').parent().click();
            cy.get(selector).clear().type(wrong);
            cy.get('i.bi-save').parent().click();
          });
          cy.get(toasterErr).should('exist').and('contain.text', 'Saving failed')
            .and('contain.text', option);
          cy.get(toasterErr).get('button.close').click();

          cy.contains(option).parent().within(() => {
            cy.get('i.bi-pencil').parent().click();
            cy.get(selector).clear().type(right);
            cy.get('i.bi-save').parent().click();
          });
          // check if toast popup is shown
          cy.get(toasterSuc).should('exist').and('contain.text', 'successfully been saved')
            .and('contain.text', option);
          // close the toast popup
          cy.get(toasterSuc).get('button.close').click();
        });
      }
    });
  });

  context('when user is normal user', () => {
    beforeEach(() => {
      cy.loginAs('user');
    });

    it('should not be able to visit the page', () => {
      cy.checkAccessDenied('/processingOption/index');
    });
  });
});
