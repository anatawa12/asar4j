package com.anatawa12.asar4j;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class AsarDirectoryEntry extends AsarEntry {
    // if java.util.HashMap, this is mutable.
    // if not, this must be immutable.
    private Map<String, AsarEntry> entries;

    AsarDirectoryEntry(String name, Map<String, AsarEntry> files) {
        super(name);
        entries = files;
    }

    @Override
    public AsarEntryType getType() {
        return AsarEntryType.DIRECTORY;
    }

    @Override
    public Collection<AsarEntry> getChildren() throws IllegalStateException {
        return Collections.unmodifiableCollection(entries.values());
    }

    @Override
    public AsarEntry getChild(String name) throws IllegalStateException {
        return entries.get(name);
    }

    @Override
    public boolean isExecutable() {
        throw new IllegalStateException("this is not FILE, but DIRECTORY");
    }

    @Override
    public void setExecutable(boolean executable) {
        throw new IllegalStateException("this is not FILE, but DIRECTORY");
    }

    void addChild(AsarEntry child) {
        if (!child.getName().startsWith(getName())
                || child.getName().length() == getName().length()
                || child.getName().charAt(child.getName().length() - 1) != '/')
            throw new IllegalArgumentException("the entry is not descendants of this");
        String name = child.getName().substring(getName().length() + 1);
        if (name.contains("/"))
            throw new IllegalArgumentException("the entry is grandchildren");
        entries.put(name, child);
    }

    @Override
    protected AsarDirectoryEntry clone() {
        AsarDirectoryEntry entry = (AsarDirectoryEntry) super.clone();
        if (entries instanceof HashMap<?, ?>) {
            entry.entries = new HashMap<>(entries);
        }
        return entry;
    }
}
