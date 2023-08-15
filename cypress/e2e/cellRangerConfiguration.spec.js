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

describe('Check cell ranger configuration page', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAsOperator();
    });

    it('should visit the index page', () => {
      cy.visit('/cellRangerConfiguration/index');
    });

    it('should select cell ranger version submit it and afterwards invalidate it', () => {
      cy.visit('/cellRangerConfiguration/index');

      cy.get('#select2-programVersion-container').click();
      cy.get('li').contains('cellranger/4.0.0').click();

      cy.get('input#submit').click();
      cy.checkPage('/cellRangerConfiguration/index');

      cy.get('div.annotation-box').should('not.contain.text', 'Please configure the Cell Ranger version to use');
      cy.get('input[type=submit]#save').should('exist');
      cy.get('input[type=submit]#execute').should('exist');

      cy.get('#invalidateConfig').click();
      cy.checkPage('/cellRangerConfiguration/index');

      cy.get('div.annotation-box').should('contain.text', 'Please configure the Cell Ranger version to use');
      cy.get('input[type=submit]#save').should('not.exist');
      cy.get('input[type=submit]#execute').should('not.exist');
    });

    it('should select cell ranger version, then activate and deactivate automatic running', () => {
      cy.visit('/cellRangerConfiguration/index');

      cy.get('#select2-programVersion-container').click();
      cy.get('li').contains('cellranger/4.0.0').click();

      cy.get('input#submit').click();
      cy.checkPage('/cellRangerConfiguration/index');

      cy.get('#updateAutomaticExecution #enableAutoExec').click();
      cy.get('#updateAutomaticExecution #referenceGenomeIndex2').select('hg_GRCh38 CELL_RANGER 1.2.0', { force: true });
      cy.get('input[type=submit]#execute').click();

      cy.checkPage('/cellRangerConfiguration/index');
      cy.get('#updateAutomaticExecution #enableAutoExec').should('be.checked');

      cy.get('#updateAutomaticExecution #enableAutoExec').click();
      cy.get('input[type=submit]#execute').click();

      cy.checkPage('/cellRangerConfiguration/index');
      cy.get('#updateAutomaticExecution #enableAutoExec').should('not.be.checked');
    });
  });
});
