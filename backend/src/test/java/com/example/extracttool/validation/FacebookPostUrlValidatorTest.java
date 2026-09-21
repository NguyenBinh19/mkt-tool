package com.example.extracttool.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FacebookPostUrlValidatorTest {
    private final FacebookPostUrlValidator validator = new FacebookPostUrlValidator();

    @Test
    void acceptsFacebookAndFbWatchHttpsUrls() {
        assertNull(validator.validate("https://www.facebook.com/example/posts/123"));
        assertNull(validator.validate("https://fb.watch/example/"));
    }

    @Test
    void rejectsLookalikesAndNonHttpsUrls() {
        assertNotNull(validator.validate("https://facebook.com.evil.example/post"));
        assertNotNull(validator.validate("http://facebook.com/example/posts/123"));
        assertNotNull(validator.validate("not a url"));
    }
}
