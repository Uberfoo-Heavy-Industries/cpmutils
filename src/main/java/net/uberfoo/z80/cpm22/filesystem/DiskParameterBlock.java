package net.uberfoo.z80.cpm22.filesystem;

public record DiskParameterBlock(

        int sectorsPerTack,
        int blockShiftFactor,
        int blockMask,
        int extentMask,
        int storageSize,
        int numDirectoryEntries,
        int directoryAllocationBitmap1,
        int directoryAllocationBitmap2,
        int checkVectorSize,
        int offset

) {
    public int getBlockSize() {
        return 1 << (blockShiftFactor + 7);
    }

    public int getOffsetBytes() {
        return (sectorsPerTack * 128) * offset;
    }

    public int getBlockRecordCount() {
        return (int)(getBlockSize() / 128);
    }

    public int getOffsetBlocks() {
        return  getOffsetBytes() / getBlockSize();
    }

    public long getFilesystemSize() {
        return (storageSize + 1) * getBlockSize();
    }

    public static final int getRecordSize() { return 128; }
}
