package com.anatawa12.asar4j;

class AsarLinkEntry extends AsarEntry {
    private final String linkTarget;

    AsarLinkEntry(String name, String link) {
        super(name);
        linkTarget = link;
    }

    @Override
    public AsarEntryType getType() {
        return AsarEntryType.LINK;
    }

    @Override
    public String getLinkTarget() {
        return linkTarget;
    }

    @Override
    public boolean isExecutable() {
        throw new IllegalStateException("this is not FILE, but LINK");
    }

    @Override
    public void setExecutable(boolean executable) {
        throw new IllegalStateException("this is not FILE, but LINK");
    }
}
