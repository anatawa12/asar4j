package com.anatawa12.asar4j;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class AsarFile implements Closeable {
    private SeekableByteChannel file;
    private AsarDirectoryEntry rootEntry;
    private long initialOffset;
    private int count;

    public AsarFile(SeekableByteChannel file) throws IOException {
        this.file = Objects.requireNonNull(file);
        readHeader();
    }

    public AsarFile(Path path) throws IOException {
        this(Files.newByteChannel(path, StandardOpenOption.READ));
    }

    public AsarFile(File file) throws IOException {
        this(file.toPath());
    }

    public AsarFile(String name) throws IOException {
        this(Paths.get(name));
    }

    private void readHeader() throws IOException {
        /*
         * header format
         *
         *  0                   4 bytes
         * +---------------------+
         * | u32 size: integer 4 |
         * +---------------------+
         * |     header size     |
         * +---------------------+
         * |   header size - 4   |
         * +---------------------+
         * |      json size      |
         * +---------------------+
         *  0     json size
         * +===============+
         * |  header json  |
         * +===============+
         */
        ByteBuffer sizeFields = ByteBuffer.allocate(8);
        sizeFields.order(ByteOrder.LITTLE_ENDIAN);
        if (file.read(sizeFields) != 8)
            throw new AsarException("invalid header: unable to read size field");
        sizeFields.rewind();
        if (sizeFields.getInt() != 4)
            throw new AsarException("invalid header: invalid size of size field");
        long headerSize = sizeFields.getInt() & 0xFFFF_FFFFL;
        sizeFields.flip();
        if (file.read(sizeFields) != 8)
            throw new AsarException("invalid header: unable to read header");
        sizeFields.rewind();
        if ((sizeFields.getInt() & 0xFFFF_FFFFL) != headerSize - 4)
            throw new AsarException("invalid header: invalid size of json");
        long jsonSize = sizeFields.getInt() & 0xFFFF_FFFFL;

        JsonReader reader = new JsonReader(Channels.newReader(
                new SLROSByteChannel(file, 16, jsonSize),
                StandardCharsets.UTF_8.newDecoder(),
                -1));
        AsarEntry entry = readEntry(reader, "", this);
        if (!(entry instanceof AsarDirectoryEntry))
            throw new AsarException("invalid header: header is not directory");
        rootEntry = (AsarDirectoryEntry) entry;

        // + 8: size field
        initialOffset = headerSize + 8;

        // this is safe because size of files is in unsigned 32bit and each entry uses two or more bytes.
        count = (int) StreamSupport.stream(iterable.spliterator(), false).count();
    }

    static AsarEntry readEntry(JsonReader reader, String name, AsarFile owner) throws IOException {
        Map<String, AsarEntry> files = null;
        String link = null;
        // 0x0001: is executable
        // 0x0002: is offset set
        // 0x0004: is size set
        int flags = 0;
        long offset = 0;
        int size = 0;

        reader.read('{');
        while (reader.cur() != '}') {
            switch (reader.readKey()) {
                case "files":
                    files = readFiles(reader, name, owner);
                    break;
                case "link":
                    link = reader.readString();
                    break;
                case "executable":
                    flags &= ~0x0001;
                    flags |= reader.readBoolean() ? 1 : 0;
                    break;
                case "offset":
                    try {
                        offset = Long.parseLong(reader.readString());
                    } catch (NumberFormatException e) {
                        throw new IOException("invalid offset", e);
                    }
                    flags |= 0x0002;
                    break;
                case "size": {
                    double number = reader.readNumber();
                    // use ! for erase NaN
                    if (!(0 <= number && number <= Integer.MAX_VALUE))
                        throw new IOException("invalid size: out of range" + number);
                    size = (int) number;
                    flags |= 0x0004;
                    break;
                }
                case "unpacked":
                    if (reader.readBoolean()) {
                        reader.skipObjectBodyRemain();
                        reader.read('}');
                        return null;
                    }
                    break;
                default:
                    reader.skipValue();
                    break;
            }
            reader.readOpt(',');
        }
        reader.read('}');
        AsarEntry entry;
        if (files != null) {
            entry = new AsarDirectoryEntry(name, Collections.unmodifiableMap(files));
        } else if (link != null) {
            entry = new AsarLinkEntry(name, link);
        } else if ((flags & 0x0006) == 0x0006) {
            entry = new AsarEntry(name);
            entry.setExecutable((flags & 0x0001) != 0);
            entry.offset = offset;
            entry.setSize(size);
        } else {
            throw new IOException("invalid header: unknown entry");
        }
        entry.owner = owner;
        return entry;
    }

    private static Map<String, AsarEntry> readFiles(JsonReader reader, String base, AsarFile owner) throws IOException {
        Map<String, AsarEntry> entries;
        reader.read('{');
        // entries map
        entries = new HashMap<>();
        while (reader.cur() != '}') {
            String childName = reader.readKey();
            AsarEntry childEntry = readEntry(reader, base + "/" + childName, owner);
            if (childEntry != null)
                entries.put(childName, childEntry);
            reader.readOpt(',');
        }
        reader.read('}');
        return entries;
    }

    /**
     * Returns the asar file/directory entry for the specified name, or null if not found.
     *
     * @param name the name of entry
     * @return the asar file/directory entry, or null if not found.
     */
    public AsarEntry getEntry(String name) {
        return getEntry(name, true);
    }

    /**
     * Returns the asar entry for the specified name, or null if not found
     *
     * @param name            the name of entry.
     * @param resolveLastLink if the name targets a link, if true, resolves the link. if false, returns link entry.
     * @return the asar entry, or null if not found.
     */
    public AsarEntry getEntry(String name, boolean resolveLastLink) {
        eraseClose();
        List<String> components = AsarEntry.components(name);
        for (String component : components) {
            if (component.equals(".") || component.equals(".."))
                throw new IllegalArgumentException("path have . or ..");
        }
        return resolveEntry(components, resolveLastLink, rootEntry);
    }

    static AsarEntry resolveEntry(
            List<String> components,
            boolean resolveLastLink,
            AsarEntry rootEntry
    ) {
        // fast path
        if (components.isEmpty()) return rootEntry;

        LinkedList<AsarEntry> entries = new LinkedList<>();
        entries.add(rootEntry);
        ListIterator<String> iter = components.listIterator();
        while (iter.hasNext()) {
            String component = iter.next();
            if (component.isEmpty()) continue; // '//' is ignored
            AsarEntry cur = entries.peekLast();
            if (cur == null) return null;
            if (cur.getType() != AsarEntryType.DIRECTORY) return null;

            switch (component) {
                case "..":
                    entries.removeLast();
                    break;
                case ".":
                    break;
                default:
                    AsarEntry child = cur.getChild(component);
                    if (child == null) return null;
                    switch (child.getType()) {
                        case DIRECTORY:
                        case FILE:
                            entries.add(child);
                            break;
                        case LINK:
                            if (resolveLastLink || iter.hasNext()) {
                                // run link
                                int added = 0;
                                for (String s1 : child.getLinkTarget().split("/")) {
                                    iter.add(s1);
                                    added++;
                                }
                                for (int i = 0; i < added; i++) {
                                    iter.previous();
                                }
                            } else {
                                return child;
                            }
                            break;
                        default:
                            throw new AssertionError();
                    }
                    break;
            }
        }
        return entries.peekLast();
    }

    /**
     * Returns an input stream for reading the contents of the specified asar file entry.
     *
     * @param entry the asar file entry
     * @return the input stream for reading the contents of the specified asar file entry.
     */
    public InputStream getInputStream(AsarEntry entry) {
        return Channels.newInputStream(getChannel(entry));
    }

    /**
     * Returns a channel for reading the contents of the specified asar file entry.
     *
     * @param entry the asar file entry
     * @return the channel for reading the contents of the specified asar file entry.
     */
    public SeekableByteChannel getChannel(AsarEntry entry) {
        eraseClose();
        if (entry == null) throw new NullPointerException("entry");
        if (entry.getType() != AsarEntryType.FILE) throw new IllegalArgumentException("the entry is not a file");
        if (entry.owner != this) throw new IllegalArgumentException("the entry is not of this file");
        return new SLROSByteChannel(
                file, initialOffset + entry.offset, entry.getSize());
    }

    public Enumeration<? extends AsarEntry> entries() {
        eraseClose();
        return new Entries(rootEntry);
    }

    private final Iterable<? extends AsarEntry> iterable = () -> new Entries(rootEntry);

    public Iterable<? extends AsarEntry> iterable() {
        eraseClose();
        return iterable;
    }

    public Iterator<? extends AsarEntry> iterator() {
        eraseClose();
        return new Entries(rootEntry);
    }

    public Stream<? extends AsarEntry> stream() {
        return StreamSupport.stream(Spliterators.spliterator(iterator(), count,
                Spliterator.DISTINCT | Spliterator.SIZED | Spliterator.NONNULL | Spliterator.IMMUTABLE),
                false);
    }

    public int size() {
        return count;
    }

    @Override
    public void close() throws IOException {
        file.close();
        file = null;
    }

    private void eraseClose() {
        if (file == null || !file.isOpen())
            throw new IllegalStateException("asar file or parent file closed");
    }

    private static class Entries implements Enumeration<AsarEntry>, Iterator<AsarEntry> {
        final Queue<AsarEntry> willBeProceed;

        private Entries(AsarEntry first) {
            this.willBeProceed = new LinkedList<>();
            willBeProceed.add(first);
        }

        @Override
        public boolean hasMoreElements() {
            return !willBeProceed.isEmpty();
        }

        @Override
        public AsarEntry nextElement() {
            AsarEntry entry = willBeProceed.remove();
            if (entry.getType() == AsarEntryType.DIRECTORY) {
                willBeProceed.addAll(entry.getChildren());
            }
            return entry;
        }

        @Override
        public boolean hasNext() {
            return hasMoreElements();
        }

        @Override
        public AsarEntry next() {
            return nextElement();
        }
    }
}
