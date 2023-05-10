package com.zebrunner.carina.webdriver.gui;

import com.zebrunner.carina.utils.annotations.Internal;

// todo remove ACTION_NAME enum from the carina-utils module
@Internal
enum ACTION_NAME {
    CLICK("click"),
    CLICK_BY_JS("click_by_js"),
    CLICK_BY_ACTIONS("click_by_actions"),
    SUBMIT("submit"),
    DOUBLE_CLICK("double_click"),
    RIGHT_CLICK("right_click"),
    HOVER("hover"),
    SEND_KEYS("send_keys"),
    SEND_KEYS_CHAR_SEQUENCE("send_keys_char_sequence"),
    TYPE("type"),
    ATTACH_FILE("attach_file"),
    GET_TEXT("get_text"),
    GET_LOCATION("get_location"),
    GET_SIZE("get_size"),
    GET_ATTRIBUTE("get_attribute"),
    PAUSE("pause"),
    WAIT("wait"),
    CHECK("check"),
    UNCHECK("uncheck"),
    IS_CHECKED("is_checked"),
    SELECT("select"),
    SELECT_VALUES("select_values"),
    SELECT_BY_MATCHER("select_by_matcher"),
    SELECT_BY_PARTIAL_TEXT("select_by_partial_text"),
    SELECT_BY_INDEX("select_by_index"),
    GET_SELECTED_VALUE("get_selected_value"),
    GET_SELECTED_VALUES("get_selected_values"),
    CLEAR("clear"),
    GET_TAG_NAME("get_tag_name"),
    GET_RECT("get_rect"),
    GET_CSS_VALUE("get_css_value"),
    GET_DOM_PROPERTY("get_dom_property"),
    GET_SCREENSHOT("get_screenshot"),
    GET_DOM_ATTRIBUTE("get_dom_attribute"),
    GET_ARIA_ROLE("get_aria_role"),
    GET_ACCESSIBLE_NAME("get_accessible_name"),
    CAPTURE_SCREENSHOT("capture_screenshot"),
    GET_LOGS("get_logs"),;

    private final String key;

    private ACTION_NAME(String key) {
        this.key = key;
    }

    public String getKey() {
        return this.key;
    }
}
