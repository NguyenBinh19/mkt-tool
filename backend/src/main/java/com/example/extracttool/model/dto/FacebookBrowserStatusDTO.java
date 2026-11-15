package com.example.extracttool.model.dto;

public record FacebookBrowserStatusDTO(String browser, String authentication, boolean loginPageOpen,
                                       int maxConcurrentTabs, int activeTabs, String message) { }
