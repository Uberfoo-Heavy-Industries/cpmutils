package net.uberfoo.cpm.filesystem;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PartitionTable {

    private static final byte[] MAGIC = { 0x25, 0x01 };
    public static final int HEADER_SIZE = MAGIC.length + Integer.BYTES;

    private final List<PartitionTableEntry> entries;

    public PartitionTable() {
        entries = new LinkedList<>();
    }

    /**
     * Constructs a PartitionTable from a buffer containing the disk image.
     *
     * @param buffer The disk image
     * @throws IOException If there are errors reading the buffer or the contents are invalid
     * @throws ClassNotFoundException If buffer contents are incorrect classes
     */
    public PartitionTable(ByteBuffer buffer) throws IOException, ClassNotFoundException {
        var magic = new byte[2];
        buffer.get(buffer.limit() - HEADER_SIZE, magic, 0, MAGIC.length);
        if (!Arrays.equals(magic, MAGIC)) {
            throw new InvalidObjectException("Not a valid partition table. Magic = " + Arrays.toString(magic));
        }

        var size = buffer.getInt(buffer.limit() - Integer.BYTES);
        var arr = new byte[size];
        buffer.get(buffer.limit() - size - HEADER_SIZE, arr);
        var bais = new ByteArrayInputStream(arr);
        var in = new ObjectInputStream(bais);

        entries = (List<PartitionTableEntry>)in.readObject();
    }

    /**
     * Encodes this table into disk format. The encoded format
     * consists of an arbitrarily sizes list of serialized
     * PartitionTableEntry objects. Followed by the magic
     * constant and the size of the serialized objects as a
     * 4 byte integer.
     *
     * @return A ByteBuffer containing the encoded form
     * @throws IOException
     */
    public ByteBuffer encode() throws IOException {
        var baos = new ByteArrayOutputStream();
        var out = new ObjectOutputStream(baos);

        out.writeObject(entries);
        var size = baos.size();

        ByteBuffer buffer = ByteBuffer.allocate(size + HEADER_SIZE);
        buffer.put(baos.toByteArray());
        buffer.put(MAGIC);
        buffer.putInt(size);

        baos.close();

        return buffer.rewind();
    }

    public List<PartitionTableEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public boolean add(PartitionTableEntry partitionTableEntry) {
        return entries.add(partitionTableEntry);
    }

    public int size() {
        return entries.size();
    }

    public int diskSize() {
        return (int)entries.stream()
                .map(PartitionTableEntry::diskParameterBlock)
                .map(DiskParameterBlock::getFilesystemSize)
                .collect(Collectors.summarizingInt(x -> x)).getSum();
    }

    public PartitionTableEntry get(int index) {
        return entries.get(index);
    }

    public Stream<PartitionTableEntry> stream() {
        return entries.stream();
    }
}
