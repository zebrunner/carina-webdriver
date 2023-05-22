package com.zebrunner.carina.webdriver.proxy;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

/**
 * All patterns should be in python-style
 */
public final class FilterConditions {

    private FilterConditions() {
        // do nothing
    }

    /**
     * Not
     * 
     * @param condition {@link FilterCondition}
     * @return {@link FilterCondition}
     */
    public static FilterCondition not(FilterCondition condition) {
        return new FilterCondition(String.format("!(%s)", condition));
    }

    /**
     * And
     *
     * @param conditions several {@link FilterCondition}
     * @return {@link FilterCondition}
     */
    public static FilterCondition and(FilterCondition... conditions) {
        return new FilterCondition(String.format("(%s)", StringUtils.joinWith(" & ", conditions)));
    }

    /**
     * Or
     *
     * @param conditions several {@link FilterCondition}
     * @return {@link FilterCondition}
     */
    public static FilterCondition or(FilterCondition... conditions) {
        return new FilterCondition(String.format("(%s)", StringUtils.joinWith(" | ", conditions)));
    }

    /**
     * Group. Should not be used in usual cases.
     *
     * @param conditions several {@link FilterCondition}
     * @return {@link FilterCondition}
     */
    public static FilterCondition group(FilterCondition... conditions) {
        return new FilterCondition(String.format("(%s)", StringUtils.joinWith(" ", conditions)));
    }

    /**
     * Asset in response: CSS, JavaScript, images, fonts.
     * 
     * @return {@link FilterCondition}
     */
    public static FilterCondition assets() {
        return new FilterCondition("~a");
    }

    /**
     * All flows
     *
     * @return {@link FilterCondition}
     */
    public static FilterCondition allFlows() {
        return new FilterCondition("~all");
    }

    /**
     * Body
     * 
     * @param regex regex
     * @return {@link FilterCondition}
     */
    public static FilterCondition body(String regex) {
        validateRegex(regex);
        return new FilterCondition(String.format("~b %s", regex));
    }

    /**
     * Request body
     * 
     * @param regex regex
     * @return {@link FilterCondition}
     */
    public static FilterCondition requestBody(String regex) {
        validateRegex(regex);
        return new FilterCondition(String.format("~bq %s", regex));
    }

    /**
     * Response body
     * 
     * @param regex regex
     * @return {@link FilterCondition}
     */
    public static FilterCondition responseBody(String regex) {
        validateRegex(regex);
        return new FilterCondition(String.format("~bs %s", regex));
    }

    /**
     * HTTP response code
     * 
     * @param code response code
     * @return {@link FilterCondition}
     */
    public static FilterCondition responseCode(Integer code) {
        return new FilterCondition(String.format("~c %s", code));
    }

    /**
     * Flow comment
     * 
     * @param regex regex
     * @return {@link FilterCondition}
     */
    public static FilterCondition flowComment(String regex) {
        validateRegex(regex);
        return new FilterCondition(String.format("~comment %s", regex));
    }

    /**
     * Domain
     * 
     * @param regex regex
     * @return {@link FilterCondition}
     */
    public static FilterCondition domain(String regex) {
        validateRegex(regex);
        return new FilterCondition(String.format("~d %s", regex));
    }

    /**
     * DNS
     * 
     * @return {@link FilterCondition}
     */
    public static FilterCondition dnsFlows() {
        return new FilterCondition("~dns");
    }

    /**
     * Destination address
     * 
     * @param regex regex
     * @return {@link FilterCondition}
     */
    public static FilterCondition destinationAddress(String regex) {
        validateRegex(regex);
        return new FilterCondition(String.format("~dst %s", regex));
    }

    /**
     * Match error
     * 
     * @return {@link FilterCondition}
     */
    public static FilterCondition error() {
        return new FilterCondition("~e");
    }

    /**
     * Header condition. Header matching should be against a string of the form {@code "name: value"}.
     * 
     * @param regex for example, if
     *            request or response contains header like {@code cache-control: no-cache}, pattern could be like {@code ^cache-control\sno-cache$}.
     * @return {@link FilterCondition}
     */
    public static FilterCondition header(String regex) {
        validateRegex(regex);
        return new FilterCondition(String.format("~h %s", regex));
    }

    /**
     * Request header condition. Header matching should be against a string of the form {@code "name: value"}.
     * 
     * @param regex for example, if
     *            request contains header like {@code cache-control: no-cache}, pattern could be like {@code ^cache-control\sno-cache$}.
     * @return {@link FilterCondition}
     */
    public static FilterCondition requestHeader(String regex) {
        validateRegex(regex);
        return new FilterCondition(String.format("~hq %s", regex));
    }

    /**
     * Response header condition. Header matching should be against a string of the form {@code "name: value"}.
     * 
     * @param regex regex for example, if
     *            response contains header like {@code cache-control: no-cache}, pattern could be like {@code ^cache-control\sno-cache$}.
     * @return {@link FilterCondition}
     */
    public static FilterCondition responseHeader(String regex) {
        validateRegex(regex);
        return new FilterCondition(String.format("~hs %s", regex));
    }

    /**
     * HTTP flows
     * 
     * @return {@link FilterCondition}
     */
    public static FilterCondition httpFlows() {
        return new FilterCondition("~http");
    }

    /**
     * Method
     * 
     * @param regex regex
     * @return {@link FilterCondition}
     */
    public static FilterCondition method(String regex) {
        validateRegex(regex);
        return new FilterCondition(String.format("~m %s", regex));
    }

    /**
     * Marked
     * 
     * @return {@link FilterCondition}
     */
    public static FilterCondition marked() {
        return new FilterCondition("~marked");
    }

    /**
     * Match marked flows with specified marker
     * 
     * @param regex regex
     * @return {@link FilterCondition}
     */
    public static FilterCondition marked(String regex) {
        validateRegex(regex);
        return new FilterCondition(String.format("~marker %s", regex));
    }

    /**
     * Metadata
     * 
     * @param regex regex
     * @return {@link FilterCondition}
     */
    public static FilterCondition metadata(String regex) {
        validateRegex(regex);
        return new FilterCondition(String.format("~meta %s", regex));
    }

    /**
     * Match request with no response
     * 
     * @return {@link FilterCondition}
     */
    public static FilterCondition requestWithNoResponse() {
        return new FilterCondition("~q");
    }

    /**
     * Replayed
     * 
     * @return {@link FilterCondition}
     */
    public static FilterCondition replayed() {
        return new FilterCondition("~replay");
    }

    /**
     * Replayed client request
     * 
     * @return {@link FilterCondition}
     */
    public static FilterCondition replayedRequest() {
        return new FilterCondition("~replayq");
    }

    /**
     * Replayed server response
     * 
     * @return {@link FilterCondition}
     */
    public static FilterCondition replayedResponse() {
        return new FilterCondition("~replays");
    }

    /**
     * Response
     * 
     * @return {@link FilterCondition}
     */
    public static FilterCondition response() {
        return new FilterCondition("~s");
    }

    /**
     * Source address
     * 
     * @param regex regex
     * @return {@link FilterCondition}
     */
    public static FilterCondition sourceAddress(String regex) {
        validateRegex(regex);
        return new FilterCondition(String.format("~src %s", regex));
    }

    /**
     * Content-type header
     * 
     * @param regex regex
     * @return {@link FilterCondition}
     */
    public static FilterCondition contentTypeHeader(String regex) {
        validateRegex(regex);
        return new FilterCondition(String.format("~t %s", regex));
    }

    /**
     * TCP
     * 
     * @return {@link FilterCondition}
     */
    public static FilterCondition tcpFlows() {
        return new FilterCondition("~tcp");
    }

    /**
     * Request Content-Type header
     * 
     * @param regex regex
     * @return {@link FilterCondition}
     */
    public static FilterCondition requestContentTypeHeader(String regex) {
        validateRegex(regex);
        return new FilterCondition(String.format("~tq %s", regex));
    }

    /**
     * Response Content-Type header
     * 
     * @param regex regex
     * @return {@link FilterCondition}
     */
    public static FilterCondition responseContentTypeHeader(String regex) {
        validateRegex(regex);
        return new FilterCondition(String.format("~ts %s", regex));
    }

    /**
     * URL
     * 
     * @param regex regex
     * @return {@link FilterCondition}
     */
    public static FilterCondition url(String regex) {
        validateRegex(regex);
        return new FilterCondition(String.format("~u %s", regex));
    }

    /**
     * UDP
     * 
     * @return {@link FilterCondition}
     */
    public static FilterCondition udpFlows() {
        return new FilterCondition("~udp");
    }

    /**
     * WebSocket
     * 
     * @return {@link FilterCondition}
     */
    public static FilterCondition websocketFlows() {
        return new FilterCondition("~websocket");
    }

    private static void validateRegex(String regex) {
        Objects.requireNonNull(regex);
        if (StringUtils.isBlank(regex) || StringUtils.isEmpty(regex)) {
            throw new IllegalArgumentException("Regex should not be blank");
        }

        if (StringUtils.containsWhitespace(regex)) {
            throw new IllegalArgumentException(String.format("Regex '%s' should not contains whitespace. Use \\s instead", regex));
        }

        if (regex.contains("\"") || regex.contains("'")) {
            throw new IllegalArgumentException(String.format("Regex '%s' should not contains \" or '.", regex));
        }
    }

}
