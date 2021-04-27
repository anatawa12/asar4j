package com.anatawa12.asar4j;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class AsarOutputStream extends OutputStream {
    private ByteArrayOutputStream bodyBuffer = new ByteArrayOutputStream();
    private final AsarDirectoryEntry root = new AsarDirectoryEntry("", new HashMap<>());
    private AsarEntry currentEntry = null;

    private final OutputStream out;

    /**
     * Creates a new asar output stream
     *
     * @param out the actual output stream
     */
    public AsarOutputStream(OutputStream out) {
        this.out = out;
    }

    // non-file operation

    /**
     * adds link entry. if {@code target} starts with slash or backslash, the target path
     * must be absolute path from root of this asar file. if not, the target path must be relative
     * path from enclosing directory of the link.
     *
     * @param link   the full name of new link.
     * @param target the full or relative name of the target of the new link.
     */
    public void addLink(String link, String target) throws IOException {
        String linkPath = AsarEntry.normalizeName(link);
        final List<String> linkComponents = AsarEntry.components(linkPath);
        final List<String> targetComponents = resolveRelativeOrAbsolute(linkComponents, target);
        int minLen = Math.min(linkComponents.size(), targetComponents.size());
        int differentAt;
        {
            int i = 0;
            for (; i < minLen; i++) {
                if (!linkComponents.get(i).equals(targetComponents.get(i)))
                    break;
            }
            differentAt = i;
        }
        // differentAt is now the first element which is different between two

        // if link is targeting inside of link, it should be error.
        if (differentAt == linkComponents.size())
            throw new AsarException("invalid link: link to inside link");
        // link to parent of link should be allowed.

        assert differentAt < linkComponents.size();
        // count of '..'
        // for example
        // link:   /a/b/c
        // target: /a/b/d/f
        // differentAt: 2
        // goParentCount = 0 = 3 - 1 - 2
        int goParentCount = linkComponents.size() - 1 - differentAt;

        StringBuilder relativeBuilder = new StringBuilder();
        boolean first = true;
        for (int i = 0; i < goParentCount; i++) {
            if (!first) relativeBuilder.append('/');
            first = false;
            relativeBuilder.append("..");
        }

        for (String s : targetComponents.subList(differentAt, targetComponents.size())) {
            if (!first) relativeBuilder.append('/');
            first = false;
            relativeBuilder.append(s);
        }
        addEntry(new AsarLinkEntry(linkPath, relativeBuilder.toString()), false);
    }

    private static List<String> resolveRelativeOrAbsolute(List<String> relativeFrom, String path) {
        // if starts with '/' or '\', it's absolute
        if (path.charAt(0) == '/' || path.charAt(0) == '\\')
            return AsarEntry.components(AsarEntry.normalizeName(path));
        // relative.
        List<String> relative = AsarEntry.components(path);
        LinkedList<String> absolute = new LinkedList<>(relativeFrom);
        // remove name of link
        absolute.removeLast();
        absolute.addAll(relative);
        ListIterator<String> iterator = absolute.listIterator();
        while (iterator.hasNext()) {
            switch (iterator.next()) {
                case ".":
                    // . : no effect, just remove '.'
                    iterator.remove();
                    break;
                case "..":
                    // .. : go parent, remove '..' and one previous.
                    iterator.remove();
                    if (!iterator.hasPrevious())
                        throw new IllegalArgumentException("path root reached with '..'");
                    iterator.previous();
                    iterator.remove();
                    break;
            }
        }
        return absolute;
    }

    /**
     * Adds a directory at the specified path. if directory is exists, no operation
     *
     * @param path the path to directory will be created.
     */
    public void addDirectory(String path) throws IOException {
        addEntry(new AsarDirectoryEntry(path, new HashMap<>()), true);
    }

    // file operation

    /**
     * Begins writing a new asar file entry and positions the stram to the start of entry data.
     * Closes the current entry if still active.
     *
     * @param entry the asar entry to be written.
     */
    public void putNextEntry(AsarEntry entry) throws IOException {
        ensureClose();
        if (entry.getType() != AsarEntryType.FILE)
            throw new IllegalArgumentException("the entry is not a file.");
        entry = new AsarEntry(entry);
        if (currentEntry != null)
            closeEntry();
        assert currentEntry == null;

        addEntry(entry, false);

        currentEntry = entry;
        entry.offset = bodyBuffer.size();
    }

    private void addEntry(AsarEntry adding, boolean allowSameType) throws AsarException {
        List<String> components = AsarEntry.components(adding.getName());
        if (components.isEmpty())
            throw new AsarException("entry name duplicated: '' (root entry)");

        AsarDirectoryEntry current = root;
        ListIterator<String> iterator = components.listIterator();
        String component;
        // because components is not empty.
        assert iterator.hasNext();
        while (true) {
            component = iterator.next();
            if (!iterator.hasNext()) break;
            AsarEntry entry = current.getChild(component);
            if (entry != null && current.getType() != AsarEntryType.DIRECTORY)
                throw new AsarException("directory not found: '" + name(components, iterator) + "' (root entry)");
            if (entry == null)
                current.addChild(
                        entry = new AsarDirectoryEntry(name(components, iterator), new HashMap<>()));
            current = (AsarDirectoryEntry) entry;
        }

        if (current.getChild(component) != null &&
                (!allowSameType || current.getChild(component).getType() != adding.getType()))
            throw new AsarException("entry name duplicated: '" + adding.getName() + "' (root entry)");
        current.addChild(adding);
    }

    private String name(List<String> components, ListIterator<String> iterator) {
        StringBuilder builder = new StringBuilder();
        int until = iterator.nextIndex();
        for (int i = 0; i < until; i++) {
            builder.append('/');
            builder.append(components.get(i));
        }
        return builder.toString();
    }

    /**
     * Closes the current asar entry and positions the stream for writing the next entry.
     */
    public void closeEntry() throws IOException {
        if (currentEntry == null) return;
        int currentOffset = bodyBuffer.size();
        // currentEntry.offset must be equals to or less than currentOffset.
        int size = (int) (currentOffset - currentEntry.offset);
        if (currentEntry.getSize() == -1) {
            currentEntry.setSize(size);
        } else {
            if (currentEntry.getSize() != size)
                throw new AsarException("invalid entry size (expected " +
                        currentEntry.getSize() + " but got " + size + " bytes)");
        }
        currentEntry = null;
    }

    /**
     * Writes an byte to the current asar entry data.
     * Current implementation just puts the data to buffer.
     *
     * @param b the data to be written
     */
    @Override
    public void write(int b) throws IOException {
        ensureEntry();
        bodyBuffer.write(b);
    }

    /**
     * Writes an byte to the current asar entry data.
     * Current implementation just puts the data to buffer.
     *
     * @param b   the data to be written
     * @param off the start offset in the data
     * @param len the number of bytes that are written
     */
    @SuppressWarnings("NullableProblems")
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        ensureEntry();
        bodyBuffer.write(b, off, len);
    }

    /**
     * Writes an byte to the current asar entry data.
     * Current implementation just puts the data to buffer.
     *
     * @param b the data to be written
     */
    @SuppressWarnings("NullableProblems")
    @Override
    public void write(byte[] b) throws IOException {
        ensureEntry();
        bodyBuffer.write(b);
    }

    /**
     * Finishes writing the contents of the asar stream without closing underlying stream.
     */
    public void finish() throws IOException {
        closeEntry();
        // for erasure concurrent operation
        ByteArrayOutputStream bodyBuffer = this.bodyBuffer;
        this.bodyBuffer = null;

        // header
        {
            ByteArrayOutputStream header = new ByteArrayOutputStream();
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(header, StandardCharsets.UTF_8))) {
                writeToJson(new JsonWriter(writer), root);
            }

            ByteBuffer buffer = ByteBuffer.allocate(4 * 4);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            // size of u32: 4
            buffer.putInt(4);
            // size of header pickle: json in u8 + 8
            buffer.putInt(header.size() + 8);
            // size of pickle excluding this size field: json in u8 + 4
            buffer.putInt(header.size() + 4);
            // size of json in u8
            buffer.putInt(header.size());

            out.write(buffer.array());
            header.writeTo(out);
        }

        // body
        bodyBuffer.writeTo(out);
    }

    private void ensureEntry() throws IOException {
        if (bodyBuffer == null)
            throw new AsarException("this stream already finished to writing");
        if (currentEntry == null)
            throw new AsarException("no current asar file entry");
    }

    private void ensureClose() throws IOException {
        if (bodyBuffer == null)
            throw new AsarException("this stream already finished to writing");
    }

    private void writeToJson(JsonWriter writer, AsarEntry entry) throws IOException {
        switch (entry.getType()) {
            case DIRECTORY: {
                writer.write("{\"files\":{");
                Iterator<AsarEntry> iter = entry.getChildren().iterator();
                if (iter.hasNext()) {
                    while (true) {
                        AsarEntry child = iter.next();
                        writer.writeKey(child.getBasename());
                        writeToJson(writer, child);
                        if (!iter.hasNext()) break;
                        writer.write(',');
                    }
                }
                writer.write("}}");
                break;
            }
            case FILE:
                writer.write("{\"offset\":");
                writer.write(Long.toString(entry.offset));
                writer.write(",\"size\":");
                writer.write(Integer.toString(entry.getSize()));
                if (entry.isExecutable())
                    writer.write(",\"executable\":true}");
                else
                    writer.write('}');
                break;
            case LINK:
                writer.write("{\"link\":");
                writer.writeString(entry.getLinkTarget());
                writer.write('}');
                break;
        }
    }
}
