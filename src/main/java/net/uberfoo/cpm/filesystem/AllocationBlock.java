package net.uberfoo.cpm.filesystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.stream.Stream;

public class AllocationBlock extends DiskBlock {

    private static final Logger LOG = LoggerFactory.getLogger(AllocationBlock.class);

    static final int ENTRY_SIZE = 32;

    private final AllocationTableEntry[] allocationTable;

    private final int numEntries;

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

    public AllocationTableEntry[] getAllocationTable() {
        return allocationTable;
    }

    public Stream<Integer> getUsedEntries() {
        return Arrays.stream(allocationTable)
                .filter(x -> x.getStat() != 0xE5)
                .map(AllocationTableEntry::getIndex);
    }

    public Stream<Integer> getUnusedEntries() {
        return Arrays.stream(allocationTable)
                .filter(x -> x.getStat() == 0xE5)
                .map(AllocationTableEntry::getIndex);
    }
}
