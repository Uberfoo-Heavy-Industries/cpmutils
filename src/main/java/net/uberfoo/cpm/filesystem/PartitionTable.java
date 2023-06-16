package net.uberfoo.cpm.filesystem;

import com.google.flatbuffers.FlatBufferBuilder;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PartitionTable {

    private static final byte[] MAGIC = { 0x25, 0x01 };
    public static final int HEADER_SIZE = MAGIC.length + Integer.BYTES;

    private final List<PartitionTableEntry> entries;

    public PartitionTable() {
        entries = new LinkedList<>();
    }

    public PartitionTable(List<PartitionTableEntry> entries) {
        this.entries = entries;
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
        var slice = buffer.slice(buffer.limit() - size - HEADER_SIZE, size);

        var encodedTable = EncodedPartitionTable.getRootAsEncodedPartitionTable(slice);

        entries = new ArrayList<>(encodedTable.entriesLength());
        for (int i = 0; i < encodedTable.entriesLength(); i++) {
            var entry = encodedTable.entries(i);
            entries.add(new PartitionTableEntry((int)entry.offset(), entry.label(), new DiskParameterBlock(entry)));
        }
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
        FlatBufferBuilder builder = new FlatBufferBuilder(0);

        var entriesArr = new int[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.get(i);
            var label = builder.createString(entry.label());
            var dpb = entry.diskParameterBlock();
            var skewTab = CpmPartitionTableEntry.createSkewTabVector(builder, dpb.skewTab());
            var encodedEntry = CpmPartitionTableEntry.createCpmPartitionTableEntry(builder,
                    label,
                    entry.offset(),
                    dpb.sectorSize(),
                    dpb.recordsPerTack(),
                    dpb.blockShiftFactor(),
                    dpb.blockMask(),
                    dpb.extentMask(),
                    dpb.storageSize(),
                    dpb.numDirectoryEntries(),
                    dpb.directoryAllocationBitmap1(),
                    dpb.directoryAllocationBitmap2(),
                    dpb.checkVectorSize(),
                    dpb.offset(),
                    skewTab
                    );
            entriesArr[i] = encodedEntry;
        }
        var entriesVec = EncodedPartitionTable.createEntriesVector(builder, entriesArr);

        EncodedPartitionTable.startEncodedPartitionTable(builder);
        EncodedPartitionTable.addEntries(builder, entriesVec);
        var table = EncodedPartitionTable.endEncodedPartitionTable(builder);
        builder.finish(table);

        var tableBuffer = builder.sizedByteArray();
        var size = tableBuffer.length;
        ByteBuffer buffer = ByteBuffer.allocate(size + HEADER_SIZE);

        buffer.put(tableBuffer);
        buffer.put(MAGIC);
        buffer.putInt(size);

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
