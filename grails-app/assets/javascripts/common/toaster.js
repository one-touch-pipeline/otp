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

/**
 * This toaster is a bootstrap based lib which allows it to simply fire bootstrap toast messages.
 * It can be used to fire success, warning, error or info toasts.
 */
$.otp.toaster = {
  /**
   * Converts message arrays to a list of items and doesn't touch normal strings.
   * @param message - Message as string or array string to convert
   * @returns {*|string}: converted message
   */
  convertMessageArray(message) {
    if (Array.isArray(message)) {
      return `<ul>${message.map((msgElement) => `<li>${msgElement}</li>`).join('')}</ul>`;
    }
    return message;
  },
  /**
   * Fires an info toast message.
   *
   * @param title - title of the toast
   * @param message - the toasts message
   */
  // eslint-disable-next-line strict
  showInfoToast(title = 'Info', message = '') {
    this.showToast(title, $.otp.toaster.convertMessageArray(message), 30000, 'info');
  },
  /**
   * Fires a success toast message.
   *
   * @param title - title of the toast
   * @param message - the toasts message
   */
  // eslint-disable-next-line strict
  showSuccessToast(title = 'Success', message = 'Operation has been successful.') {
    this.showToast(title, $.otp.toaster.convertMessageArray(message), 15000, 'success');
  },
  /**
   * Fires a warning toast message.
   *
   * @param title - title of the toast
   * @param message - the toasts message
   */
  // eslint-disable-next-line strict
  showWarningToast(title = 'Warning', message = 'A warning occurred during the operation.') {
    this.showToast(title, $.otp.toaster.convertMessageArray(message), 30000, 'warning');
  },
  /**
   * Fires an error toast message.
   *
   * @param title - title of the toast
   * @param error - the toast's error message
   */
  // eslint-disable-next-line strict
  showErrorToast(title = 'Error', error = 'Unknown error. Please try again.') {
    this.showToast(title, $.otp.toaster.convertMessageArray(error), 60000, 'danger');
  },
  /**
   * Fires a toast message.
   *
   * @param title - title of the toast
   * @param message - the toasts message
   * @param displayTime - time in ms to show the toast
   * @param state - allowed states: info, success, warning, danger. default is info
   */
  // eslint-disable-next-line strict
  showToast(title, message, displayTime, state = 'info') {
    const stateProps = this.getStateProps(state);
    const date = new Date().toLocaleTimeString('en-US', {
      hour12: false,
      hour: 'numeric',
      minute: 'numeric'
    });

    const otpToast = `<div class="toast ${stateProps.toastClass}" data-bs-delay="${displayTime}"
                           role="alert" aria-live="assertive" aria-atomic="true" data-autohide="false">
                        <div class="toast-header justify-content-between">
                          <span class="title-wrapper">
                            <span class="otp-toast-icon-wrapper text-${state}">${stateProps.icon}</span>
                            <strong class="mr-auto">${title}</strong>
                            <small class="text-muted otp-toast-time-txt">${date}</small>
                          </span>
                          <button type="button" class="ml-2 btn btn-outline-secondary btn-sm mb-1 close" 
                                  data-bs-dismiss="toast" aria-label="Close">
                            <span aria-hidden="true">&times;</span>
                          </button>
                        </div>
                        <div class="toast-body">
                          <div class="alert alert-${state}" role="alert">${message}</div>
                        </div>
                      </div>`;

    this.getToastBox().append(otpToast);

    const toast = $('.toast');

    toast.on('hidden.bs.toast', function () {
      $(this).remove();
    });
    toast.toast('show');
  },
  /**
   * This method returns the wrapper for the toaster.
   * If it not exists in the DOM, it will be created then.
   *
   * @returns {*|Window.jQuery|HTMLElement}
   */
  getToastBox() {
    'use strict';

    const toastBoxId = 'otpToastBox';
    const toastBox = `<div id="${toastBoxId}" class="otpToastBox"></div>`;
    const getToastBoxFromDOM = () => $(`#${toastBoxId}`);

    if (getToastBoxFromDOM().length) {
      return getToastBoxFromDOM();
    }

    $('body').append(toastBox);
    return getToastBoxFromDOM();
  },
  /**
   * Convert the state into a css class name.
   *
   * @param state - info, success, warning, danger
   * @returns {string}: css class
   */
  getStateProps(state) {
    'use strict';

    switch (state) {
      case 'info':
        return {
          toastClass: 'otpInfoToast',
          icon: '<i class="bi bi-info-circle"></i>'
        };
      case 'success':
        return {
          toastClass: 'otpSuccessToast',
          icon: '<i class="bi bi-check-circle"></i>'
        };
      case 'warning':
        return {
          toastClass: 'otpWarningToast',
          icon: '<i class="bi bi-exclamation-circle"></i>'
        };
      case 'danger':
        return {
          toastClass: 'otpErrorToast',
          icon: '<i class="bi bi-bug"></i>'
        };
      default:
        return {
          toastClass: 'otpInfoToast',
          icon: '<i class="bi bi-info-circle"></i>'
        };
    }
  }
};
