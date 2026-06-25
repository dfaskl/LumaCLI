package com.lumacli.browser;

public interface BrowserConnector {
    String status();

    String connectDefault();

    String disconnect();
}
