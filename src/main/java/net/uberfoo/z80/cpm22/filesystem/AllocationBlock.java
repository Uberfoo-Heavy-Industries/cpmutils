package net.uberfoo.z80.cpm22.filesystem;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.stream.Stream;

public class AllocationBlock extends DiskBlock {

    static final int ENTRY_SIZE = 32;

    private final AllocationTableEntry[] allocationTable;

    private final int numEntries;

    public AllocationBlock(long index, ByteBuffer block, DiskParameterBlock dpb) {
        super(index, dpb);
        numEntries = dpb.getBlockSize() / ENTRY_SIZE;
        this.allocationTable = new AllocationTableEntry[numEntries];
        System.out.println(" --- Allocation table #" + index);

        // Iterate over the entries in the allocation block
        for (int i = 0; i < numEntries; i++) {
            var entryBytes = new byte[ENTRY_SIZE];
            block.get(entryBytes);

            allocationTable[i] = new AllocationTableEntry(index, i, entryBytes, dpb);
            System.out.println(allocationTable[i]);
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
