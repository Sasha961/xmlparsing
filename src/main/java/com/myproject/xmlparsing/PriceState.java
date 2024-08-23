package com.myproject.xmlparsing;

public enum PriceState {
    REGISTERED("registered"),
    AWAITS_CLASSIFICATION("awaits_classification"),
    IN_PROGRESS("in_progress"),
    ERROR("error"),
    ERROR_EMPTY("error_empty"),
    CHANGED("changed"),
    UNCHANGED("unchanged"),
    ERROR_95("error_95"),
    IN_PROGRESS_TRY_LATER("in_progress_try_later");

    public final String state;

    PriceState(String state) {
        this.state = state;
    }
}
