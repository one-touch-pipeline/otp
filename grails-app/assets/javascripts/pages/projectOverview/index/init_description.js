$.otp.initialiseSpecificOverview = {


    /**
     * Boolean shows if descriptionContent is collapsed or not
     */
    descriptionCollapsed: true,

    /**
     *  Tells if textarea is clickable at the moment to collapse/expand the description content
     */
    textareaClickable: true,

    /**
     * If a click on descriptionRow is performed the descriptionContent
     * is collapsed/expanded in the specificOverview HTML.
     * Will only expand/collapse if the click is not on
     * either buttons or edit textareas called.
     */
    toggleDescription: function () {

        $("#descriptionRow").click(function (e) {

            if ($.otp.initialiseSpecificOverview.validateToggle(e)) {

                if ($.otp.initialiseSpecificOverview.descriptionCollapsed == false) {

                    $.otp.initialiseSpecificOverview.collapseDescription();

                } else {

                    $.otp.initialiseSpecificOverview.expandDescription();
                }
            }

        });
    },

    /**
     * Expands the Description and changes the Arrow direction in the description header.
     *
     */
    expandDescription: function () {
        $("#descriptionContent").children().css('height', 'auto');
        $.otp.initialiseSpecificOverview.descriptionCollapsed = false;
        $("#descriptionHeader").html('Description ↑');
    },

    /**
     * Collapse the Description and changes the Arrow direction in the description header.
     */
    collapseDescription: function () {
        $.otp.initialiseSpecificOverview.descriptionCollapsed = true;
        $("#descriptionContent").children().css('height', '3em');
        $("#descriptionHeader").html('Description ↓');
    },



    /**
     * Validates if the edit Button or the editing textarea is clicked,
     * so no collapse will be initiated.
     * @param e
     * @returns {boolean} true or false to tell if a collapse or expand have to be made
     */
    validateToggle: function (e) {
        if (!$(e.target).is("button")) {
            if ($.otp.initialiseSpecificOverview.textareaClickable) {
                return true;
            }
        } else {
            $.otp.initialiseSpecificOverview.toggleTextareaClickDisable();
            $.otp.initialiseSpecificOverview.expandDescription();
            return false;
        }

    },


    /**
     * Toggle the boolean that tells if the textarea is clickable at the moment
     * This is important to ensure that no collapse is made while editing the text.
     */
    toggleTextareaClickDisable: function () {
        $.otp.initialiseSpecificOverview.textareaClickable = !$.otp.initialiseSpecificOverview.textareaClickable;
    }
};
