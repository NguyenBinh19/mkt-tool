package com.example.extracttool.playwright;

/** A browser tab could not commit or render a Facebook navigation. */
public class FacebookBrowserNavigationException extends RuntimeException {
    public FacebookBrowserNavigationException(String message, Throwable cause) {
        super(message, cause);
    }
}
