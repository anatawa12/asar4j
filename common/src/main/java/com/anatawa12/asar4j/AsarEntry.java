package com.anatawa12.asar4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * <h3>The normalized name</h3>
 * <p>
 * this library uses normalized name for identify each entries.
 * A normalized name can't ends with '/', can't have '\' (back slash),
 * must starts with '/', and each component can't be '.' or '..'.
 * </p>
 */
public class AsarEntry implements Cloneable {
    private final String name;
    private boolean executable;
    private int size = -1;
    long offset = -1;
    Object owner;

    /**
     * new a file entry.
     *
     * @param name the full name of new entry.
     */
    public AsarEntry(String name) {
        this.name = checkName(normalizeName(name));
    }

    public AsarEntry(AsarEntry entry) {
        this.name = entry.name;
        this.executable = entry.executable;
        this.size = entry.size;
        this.offset = entry.offset;
        this.owner = null;
    }

    /**
     * check is the name normalized.
     * normalized name must be matched with this regexp: {@code (/[^\\/]+)*}
     *
     * @param name the name may not normalized.
     * @return true if the name is normalized. false if not.
     */
    // package-private for testing
    static boolean isNormalized(String name) {
        if (name.length() == 0) return true;
        if (name.charAt(0) != '/') return false;
        // 0: normal
        // 1: single . after slash
        // 2: double . after slash
        // 3: after slash
        int stat = 3;
        for (int i = 1; i < name.length(); i++) {
            switch (name.charAt(i)) {
                case '\\':
                    return false;
                case '/':
                    if (stat != 0) return false;
                    stat = 3;
                    break;
                case '.':
                    if (stat == 3) stat = 1;
                    else if (stat == 1) stat = 2;
                    else stat = 0;
                    break;
                default:
                    stat = 0;
                    break;
            }
        }
        return stat == 0;
    }

    static List<String> components(String name) {
        List<String> result = new ArrayList<>();
        int lastSlash = -1;
        int i = 0;
        for (; i < name.length(); i++) {
            if ("\\/".indexOf(name.charAt(i)) != -1) {
                if (i != lastSlash + 1)
                    result.add(name.substring(lastSlash + 1, i));
                lastSlash = i;
            }
        }
        if (i != lastSlash + 1)
            result.add(name.substring(lastSlash + 1, i));
        return result;
    }

    /**
     * Normalizes the name. this method
     * <ul>
     *     <li>replaces '\' to '.'</li>
     *     <li>replaces multiple '/' to single '/'</li>
     *     <li>removes suffix '/'</li>
     *     <li>adds prefix '/' (for non-root path)</li>
     *     <li>throws exception if '.' or '..' is existing as a directory/file name.</li>
     * </ul>
     *
     * @param name the name of entry to be normalized.
     * @return the normalized from of {@code name}
     * @throws IllegalArgumentException if the name can't be normalized
     */
    public static String normalizeName(String name) {
        if (isNormalized(name)) return name;
        List<String> parts = components(name);
        if (parts.isEmpty()) return "";
        StringBuilder result = new StringBuilder();
        Iterator<String> iterator = parts.iterator();
        do {
            String component = iterator.next();
            if (".".equals(component) || "..".equals(component))
                throw new IllegalArgumentException("path have . or ..");
            result.append('/');
            result.append(component);
        } while (iterator.hasNext());
        return result.toString();
    }

    private static String checkName(String normalized) {
        if (normalized.contains("/./") || normalized.endsWith("/.")
                || normalized.contains("/../") || normalized.endsWith("/.."))
            throw new IllegalArgumentException("'.' or '..' cannot be a part of path component");
        return normalized;
    }

    /**
     * Returns the full name of this entry.
     *
     * @return the full name of this entry
     */
    public final String getName() {
        return name;
    }

    /**
     * Returns the basename of this entry.
     *
     * @return the basename of this entry.
     * @throws IllegalStateException if this entry is root directory entry.
     */
    public final String getBasename() {
        if (name.isEmpty())
            throw new IllegalStateException("root directory entry doesn't have basename");
        return name.substring(name.lastIndexOf('/') + 1);
    }

    /**
     * type of this entry.
     *
     * @return type of this entry.
     */
    public AsarEntryType getType() {
        return AsarEntryType.FILE;
    }

    /**
     * sets size of this file in unsigned 32-bit integer.
     * this will be ignored for LINKs and DIRECTORYs.
     *
     * @return size of this file or -1 if not specified.
     */
    public final int getSize() {
        return size;
    }

    /**
     * sets size of this file in unsigned 32-bit integer.
     * this will be ignored for LINKs and DIRECTORYs.
     *
     * @param size size of this file or -1 for reset.
     */
    public final void setSize(int size) {
        if (size != -1 && size < 0)
            throw new IllegalArgumentException("negative size");
        if (owner != null)
            throw new IllegalStateException("immutable entry");
        this.size = size;
    }

    /**
     * is this file executable.
     *
     * @return is this file executable
     * @throws IllegalStateException if this is not a FILE
     */
    public boolean isExecutable() {
        return executable;
    }

    /**
     * sets is this file executable.
     *
     * @param executable is this file executable
     * @throws IllegalStateException if this is not a FILE
     */
    public void setExecutable(boolean executable) {
        if (owner != null)
            throw new IllegalStateException("immutable entry");
        this.executable = executable;
    }

    // type specific

    // LINK

    /**
     * @return relative path to link target.
     * @throws IllegalStateException if this is not a LINK
     */
    public String getLinkTarget() throws IllegalStateException {
        throw new IllegalStateException("this is not LINK, is " + getType());
    }

    //DIRECTORY

    /**
     * @return all entries in this directory
     * @throws IllegalStateException if this is not a LINK
     */
    public Collection<AsarEntry> getChildren() throws IllegalStateException {
        throw new IllegalStateException("this is not DIRECTORY, is " + getType());
    }

    /**
     * gets child entry with basename.
     *
     * @param name the name of entry.
     * @return the child entry or null if not found.
     * @throws IllegalStateException if this is not a DIRECTORY.
     */
    public AsarEntry getChild(String name) throws IllegalStateException {
        throw new IllegalStateException("this is not DIRECTORY, is " + getType());
    }

    /**
     * clones this instance. you may need to override this.
     *
     * @return cloned instance of this. the type of this and returned value must be same.
     */
    @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
    @Override
    protected AsarEntry clone() {
        try {
            AsarEntry entry = (AsarEntry) super.clone();
            entry.owner = null;
            return entry;
        } catch (CloneNotSupportedException | ClassCastException e) {
            throw new AssertionError(e);
        }
    }
}
