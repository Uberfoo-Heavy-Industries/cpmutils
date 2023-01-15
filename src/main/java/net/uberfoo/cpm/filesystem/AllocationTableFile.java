package net.uberfoo.cpm.filesystem;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.IntStream;

/**
 * Represents an entire file composed of one or more allocation table entries.
 */
public class AllocationTableFile {

    private static final Logger LOG = LoggerFactory.getLogger(AllocationTableFile.class);

    private final int stat;
    private final String filename;
    private final BitSet flags;
    private int recordCount;
    private int byteCount;

    private final List<Long> blockPointers;

    private final List<CpmDisk.EntryCoordinates> allocationIndexes;

    private final DiskParameterBlock diskParameterBlock;

    private final ByteBuffer buffer;

    AllocationTableFile(int stat, @NotNull String filename, @NotNull BitSet flags,
                               @NotNull DiskParameterBlock diskParameterBlock, @NotNull ByteBuffer buffer) {
        this.stat = stat;
        this.filename = filename;
        this.flags = flags;
        this.diskParameterBlock = diskParameterBlock;
        this.buffer = buffer;
        blockPointers = new LinkedList<>();
        allocationIndexes = new LinkedList<>();
    }

    AllocationTableFile(@NotNull List<AllocationTableEntry> tableEntries,
                        @NotNull DiskParameterBlock diskParameterBlock,
                        @NotNull ByteBuffer buffer) {
        this.diskParameterBlock = diskParameterBlock;
        this.buffer = buffer;
        blockPointers = new ArrayList<>(tableEntries.size() * 8);
        allocationIndexes = new ArrayList<>(tableEntries.size());

        var list = tableEntries.stream()
                // Ensure entries are in order by extent number
                .sorted(Comparator.comparingInt(AllocationTableEntry::getExtent)).toList();

        var first = list.stream().findFirst().orElseThrow();

        filename = first.getFullFilename();
        stat = first.getStat();
        flags = first.getFlags();

        list.forEach(x -> {
            processEntry(x);
        });
    }

    /**
     * Retrieves the contents of this file from the disk.
     *
     * @return A byte buffer containing the contents.
     */
    public ByteBuffer retrieveFileContents() {
        int size = recordCount * DiskParameterBlock.getRecordSize();
        var buff = ByteBuffer.allocate(size);

        IntStream.range(0, blockPointers.size())
                .mapToObj(i -> {
                    int blockSize = diskParameterBlock.getBlockSize();
                    if (i == blockPointers.size() - 1)
                        blockSize = size % blockSize;

                    LOG.trace("allocating Block #{}: {}bytes", i, blockSize);

                    ByteBuffer block = ByteBuffer.allocate(blockSize);
                    return buffer.slice((int) (blockPointers.get(i).longValue() * diskParameterBlock.getBlockSize()), blockSize);
                })
                .forEachOrdered(buff::put);

        return buff;
    }

    void delete(ByteBuffer buffer) throws IOException {
        for (CpmDisk.EntryCoordinates x : allocationIndexes) {
            // Write unused byte to start of entry
            buffer.put((int) ((x.block() * diskParameterBlock.getBlockSize()) + diskParameterBlock.getOffsetBytes()
                    + (AllocationBlock.ENTRY_SIZE * x.index())), (byte)0xE5);
        }
    }

    private void processEntry(@NotNull AllocationTableEntry x) {
        recordCount += x.getRecordCount();
        byteCount = x.getBc();
        allocationIndexes.add(new CpmDisk.EntryCoordinates(x.getAllocBlockPointer(), x.getIndex()));
        blockPointers.addAll(x.getBlockPointers().stream().filter(p -> p != 0x00).toList());
    }

    /**
     * Gets the stat byte of the file. This typically contains
     * the user number.
     *
     * @return Stat byte
     */
    public int getStat() {
        return stat;
    }

    /**
     * Returns the complete name of the file.
     *
     * @return The file name
     */
    public String getFilename() {
        return filename;
    }

    /**
     * Gets the set of flag bits for this file.
     *
     * @return A set of flags
     */
    public BitSet getFlags() {
        return flags;
    }

    /**
     * Gets the count of 128 byte records in this file.
     *
     * @return The count
     */
    public int getRecordCount() {
        return recordCount;
    }

    /**
     * Gets the BC byte which is only used in CP/M 3 and above.
     *
     * @return The byte count byte
     */
    public int getByteCount() {
        return byteCount;
    }

    /**
     * Computes the size of the file based on count of records.
     *
     * @return The size of this file.
     */
    public long size() {
        return recordCount * DiskParameterBlock.getRecordSize();
    }

    /**
     * Gets the list of all block pointers for this file. The
     * block pointers will be in proper order. The list is
     * immutable.
     *
     * @return A list of block pointers.
     */
    public List<Long> getBlockPointers() {
        return Collections.unmodifiableList(blockPointers);
    }

    List<CpmDisk.EntryCoordinates> getAllocationIndexes() {
        return allocationIndexes;
    }

    @Override
    public String toString() {
        var hexValues = blockPointers.stream()
                .map(x -> String.format("0x%04x", x))
                .toArray();

        return String.format("Stat: 0x%02x, File name: %12s, record count: %6d, flags: %s, blocks: %s", stat, filename, recordCount, flags, Arrays.toString(hexValues));
    }
}
