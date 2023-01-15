package net.uberfoo.cpm.filesystem;

/**
 * Represents the parameters of a CP/M disk.
 *
 * @param sectorsPerTack Number of sectors on each track.
 * @param blockShiftFactor Block shift factor.
 * @param blockMask Block mask.
 * @param extentMask Mask to use on RC lower bit for extent high bits.
 * @param storageSize Size of the disk in number of blocks minus one.
 * @param numDirectoryEntries Maximum number of file allocation entries
 * @param directoryAllocationBitmap1 Mask of allocation directory locations.
 * @param directoryAllocationBitmap2 Mask of allocation directory locations.
 * @param checkVectorSize Check vector size. Usually 0 for fixed disks.
 * @param offset Number of tracks to skip at the beginning of the disk.
 */
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
    /**
     * Computes the size of a single block according to the parameters.
     *
     * @return A size in bytes
     */
    public int getBlockSize() {
        return 1 << (blockShiftFactor + 7);
    }

    /**
     * Computes the offset, in bytes, according to the parameters.
     *
     * @return An offset in bytes
     */
    public int getOffsetBytes() {
        return (sectorsPerTack * 128) * offset;
    }

    /**
     * Computes the number of records in a block according to the parameters.
     *
     * @return A number of records
     */
    public int getBlockRecordCount() {
        return (int)(getBlockSize() / 128);
    }

    /**
     * Computes the size of the disk offset in bytes according to the parameters.
     *
     * @return An offset in blocks
     */
    public int getOffsetBlocks() {
        return  getOffsetBytes() / getBlockSize();
    }

    /**
     * Computes the full size of the disk according to the parameters.
     *
     * @return A size in bytes
     */
    public long getFilesystemSize() {
        return (storageSize + 1) * getBlockSize();
    }

    /**
     * Gets the size of a CP/M filesystem record.
     *
     * @return A size in bytes
     */
    public static final int getRecordSize() { return 128; }
}
