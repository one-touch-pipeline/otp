/*jslint browser: true */
/*global $ */

OTP = {
    contextPath:    $("head meta[name=contextPath]").attr("content"),
    controllerName: $("head meta[name=controllerName]").attr("content"),
    actionName:     $("head meta[name=actionName]").attr("content"),

    /**
     * Helper method to extend the given link by a further component.
     * Ensures that there is exactly one slash between the link and the further
     * component.
     * @param link The original link
     * @param component The new component to add
     * @returns link + '/' + component
     */
    addLinkComponent: function (link, component) {
        "use strict";
        if (component === undefined || !component) {
            return link;
        }
        if (!isNaN(link)) {
            link = link.toString();
        }
        if (!isNaN(component)) {
            component = component.toString();
        }
        if (link.charAt(link.length - 1) !== "/" && component.charAt(0) !== "/") {
            link += "/";
        } else if (link.charAt(link.length - 1) === "/" && component.charAt(0) === "/") {
            component = component.substring(1);
        }
        link += component;
        return link;
    },
    /**
     * Creates an URL from the passed in options in the same way as the Grails
     * createLink tag. The options is an object with the following attributes:
     * <ul>
     * <li>controller</li>
     * <li>action</li>
     * <li>id</li>
     * <li>parameters</li>
     * </ul>
     *
     * All elements are optional. Parameters is an object which gets serialized
     * into key/value pairs for the query part of the URL.
     *
     * If all elements are provided the following link structure is generated:
     * <strong>/applicationContextPath/controller/action/id?key1=value1&key2=value2</strong>
     * @param options The URL parts to construct the link from
     * @returns an URL to be used in e.g. href element of an a-attribute
     */
    createLink: function (options) {
        "use strict";
        var link, parameter, counter;
        link = OTP.contextPath;
        if (options === undefined || !options) {
            return link;
        }
        link = OTP.addLinkComponent(link, options.controller);
        link = OTP.addLinkComponent(link, options.action);
        link = OTP.addLinkComponent(link, options.id);
        if (options.parameters !== undefined && options.parameters && Object.keys(options.parameters).length > 0) {
            link += "?";
            counter = 0;
            for (parameter in options.parameters) {
                if (options.parameters.hasOwnProperty(parameter)) {
                    if (counter > 0) {
                        link += "&";
                    }
                    link += parameter + "=" + options.parameters[parameter];
                    counter += 1;
                }
            }
        }
        return link;
    },
    /**
     * Creates the HTML markup for an a element from the passed in options.
     * For the actual link (href) the same attributes in options are supported
     * as in {@link OTP.createLink}. In addition the following attributes in
     * options are supported:
     * <ul>
     * <li>title</li>
     * <li>text</li>
     * <li>target</li>
     * </ul>
     *
     * Title is used for the title attribute of the a-attribute and text is used
     * for the innerHTML element of the a-attribute
     * @param options The options defining the hyperlink
     * @returns {String} Markup for HTML element a
     */
    createLinkMarkup: function (options) {
        "use strict";
        var link, text, title, target;
        link = '<a href="' + OTP.createLink(options) + '"';
        text = "";
        if (options !== undefined && options) {
            if (options.text !== undefined && options.text) {
                text = options.text;
            }
            if (options.title !== undefined && options.title) {
                title = options.title;
            }
            if (options.target !== undefined && options.target) {
                target = options.target;
            }
        }
        if (title !== undefined) {
            link += ' title="' + title + '"';
        }
        if (target !== undefined) {
            link += ' target="' + target + '"';
        }
        return link + '>' + text + '</a>';
    },

    /**
     * Show a message
     * @param message Message to be shown
     * @param type Message type, optional, can either be success, info, warning or danger; default is info
     */
    showMessage: function (message, type) {
        "use strict";
        if (!message) {
            return;
        }
        type = typeof type !== 'undefined' ? type : "info";
        var alert = '.js-alert-' + type;
        var element = $(alert + " .js-alert-message");
        var currentValue = element.html();
        if (currentValue !== "") {
            currentValue += "<br>";
        }
        $(element).html(currentValue + message);
        $(alert).removeClass("hidden");
        $(alert + ' .close').click(function() {
            $(element).text("");
        });
    },

    /**
     * Return a function with the name given as string
     * @param functionName Name of the function
     * @returns {*} the function if it exists
     */
    getFunctionFromString: function(functionName) {
        var scope = window;
        var scopeSplit = functionName.split('.');
        for (var i = 0; i < scopeSplit.length - 1; i++) {
            scope = scope[scopeSplit[i]];
            if (scope === undefined) {
                return;
            }
        }
        return scope[scopeSplit[scopeSplit.length - 1]];
    }
};

$(function() {
    // enable bootstrap tooltips
    $('[title]').tooltip();

    // show/hide scroll to top button
    $(window).scroll(function() {
        if ($(this).scrollTop() > 100) {
            // 768 => http://getbootstrap.com/css/#grid-options
            if ($(window).width() >= 768) {
                $('.js-scroll-to-top').fadeIn();
            }
        } else {
            $('.js-scroll-to-top').fadeOut();
        }
    });

    // click event to scroll to top
    $('.js-scroll-to-top').click(function() {
        $('html, body').animate({
            scrollTop: 0
        }, 500);
        return false;
    });

    // project menu - prevent closing the menu or navigating when clicking on menu headers
    $('#js-project-list').find('li.dropdown-submenu > a').on('click', function(e) {
        e.preventDefault();
        // The event won't be propagated up to the document node and therefore delegated events won't be fired
        e.stopPropagation();
    });

    // project menu - search
    $('#js-project-input').on('input', function (e) {
        var inputAsArray = $(e.target).val().replace(/([a-z])([A-Z])/g, '$1 $2').toLowerCase().split(/[ _-]/);

        $('#js-project-list').find('li').each(function () {
            var li = $(this);

            var projectName = $('a', li).text().replace(/[ _-]/, "").toLowerCase();

            var projectMatches = true;
            for (var i = 0; i < inputAsArray.length; i++) {
                if (projectName.indexOf(inputAsArray[i]) === -1) {
                    projectMatches = false;
                    break;
                }
            }

            if (projectMatches) {
                li.removeClass("hidden");
            } else {
                li.addClass("hidden");
            }
        })
    });
});
