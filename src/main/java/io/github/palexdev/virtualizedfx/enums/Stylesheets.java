package io.github.palexdev.virtualizedfx.enums;

import io.github.palexdev.virtualizedfx.ResourceManager;

import java.net.URL;

public enum Stylesheets {
    VIRTUAL_SCROLL_PANE("VirtualScrollPane.css"),
    SCROLL_BAR("NFXScrollBar.css"),
    ;

    private final String path;

    Stylesheets(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public URL getURL() {
        return ResourceManager.getResource(path);
    }

    public String load() {
        return ResourceManager.loadResource(path);
    }
}
