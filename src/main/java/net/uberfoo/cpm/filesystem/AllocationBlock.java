package net.uberfoo.cpm.filesystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Represents a single block of the file allocation directory.
 */
public class AllocationBlock extends DiskBlock {

    private static final Logger LOG = LoggerFactory.getLogger(AllocationBlock.class);

    static final int ENTRY_SIZE = 32;

    private final AllocationTableEntry[] allocationTable;

    private final int numEntries;

    /**
     * Creates a new AllocationBlock from the supplied buffer and parameters
     *
     * @param index Index of the block
     * @param block The block as a byte buffer
     * @param dpb The disk parameters
     */
    public AllocationBlock(long index, ByteBuffer block, DiskParameterBlock dpb) {
        super(index, dpb);
        numEntries = dpb.getBlockSize() / ENTRY_SIZE;
        this.allocationTable = new AllocationTableEntry[numEntries];
        LOG.trace(" --- Allocation table #{}", index);

        // Iterate over the entries in the allocation block
        for (int i = 0; i < numEntries; i++) {
            var entryBytes = new byte[ENTRY_SIZE];
            block.get(entryBytes);

            allocationTable[i] = new AllocationTableEntry(index, i, entryBytes, dpb);
            LOG.trace(allocationTable[i].toString());
        }
    }

    /**
     * Gets the entire file allocation table contained in this block.
     *
     * @return The entire file allocation table
     */
    public AllocationTableEntry[] getAllocationTable() {
        return allocationTable;
    }

    /**
     * Gets all the unused entries in the allocation table represented by this block.
     *
     * @return All used file allocation table entries
     */
    public Stream<Integer> getUsedEntries() {
        return Arrays.stream(allocationTable)
                .filter(x -> x.getStat() != 0xE5)
                .map(AllocationTableEntry::getIndex);
    }

    /**
     * Get all the used entries in the allocation table represented by this block.
     *
     * @return All unused file allocation table entries.
     */
    public Stream<Integer> getUnusedEntries() {
        return Arrays.stream(allocationTable)
                .filter(x -> x.getStat() == 0xE5)
                .map(AllocationTableEntry::getIndex);
    }
}
