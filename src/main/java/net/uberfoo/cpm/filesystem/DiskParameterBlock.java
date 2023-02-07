package net.uberfoo.cpm.filesystem;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

/**
 * Represents the parameters of a CP/M disk.
 *
 * @param recordsPerTack Number of sectors on each track.
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
        int sectorSize,
        int recordsPerTack,
        int blockShiftFactor,
        int blockMask,
        int extentMask,
        int storageSize,
        int numDirectoryEntries,
        int directoryAllocationBitmap1,
        int directoryAllocationBitmap2,
        int checkVectorSize,
        int offset,
        int[] skewTab
) implements Serializable {

    public DiskParameterBlock(CpmPartitionTableEntry tableEntry) {
        this(
                tableEntry.sectorSize(),
                tableEntry.recordsPerTrack(),
                tableEntry.blockShiftFactor(),
                tableEntry.blockMask(),
                tableEntry.extentMask(),
                tableEntry.storageSize(),
                tableEntry.numDirectoryEntries(),
                tableEntry.directoryAllocationBitmap1(),
                tableEntry.directoryAllocationBitmap2(),
                tableEntry.checkVectorSize(),
                tableEntry.trackOffset(),
                new int[tableEntry.skewTabLength()]
        );

        for (int i = 0; i < tableEntry.skewTabLength(); i++)
            skewTab[i] = tableEntry.skewTab(i);
    }

    /**
     * Translates a logical sector into a physical sector.
     *
     * @param sector Sector number of track
     *
     * @return Physical sector number
     */
    public int translateSector(int sector) {
        if (skewTab.length == 0) return sector;
        return skewTab[sector];
    }

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
        return (recordsPerTack * 128) * offset;
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
     * Computes the number of records in a block according to the parameters.
     *
     * @return A number of records
     */
    public int getBlockSectorCount() {
        return (int)(getBlockSize() / sectorSize);
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
    public int getFilesystemSize() {
        return (storageSize + 1) * getBlockSize();
    }

    /**
     * Computes the number of tracks on the disk.
     *
     * @return The number of tracks
     */
    public int getTrackCount() {
        return getFilesystemSize() / (128 * recordsPerTack);
    }

    /**
     * Computes the number of physical sectors on each track.
     *
     * @return Number of sectors
     */
    public int getSectorsPerTrack() {
        return (recordsPerTack * 128) / sectorSize;
    }

    /**
     * Gets the size of a CP/M filesystem record.
     *
     * @return A size in bytes
     */
    public static int getRecordSize() { return 128; }

    /**
     * Computes a skew table for a given skew number and number of sectors.
     *
     * @param skew The skew factor
     * @param sectorsPerTack Number of sectors
     * @return A sector lookup table
     */
    public static int[] createSkewTab(int skew, int sectorsPerTack) {
        var skewTab = new int[sectorsPerTack];
        int k;
        for (int i = 0, j = 0; i < sectorsPerTack; ++i, j = (j + skew) % sectorsPerTack) {
            while (true) {
                for (k = 0; k < i && skewTab[k] != j; ++k) ;
                if (k < i) j = (j + 1) % sectorsPerTack;
                else break;
            }
            skewTab[i] = j;
        }
        return skewTab;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiskParameterBlock that = (DiskParameterBlock) o;
        return sectorSize == that.sectorSize && recordsPerTack == that.recordsPerTack && blockShiftFactor == that.blockShiftFactor && blockMask == that.blockMask && extentMask == that.extentMask && storageSize == that.storageSize && numDirectoryEntries == that.numDirectoryEntries && directoryAllocationBitmap1 == that.directoryAllocationBitmap1 && directoryAllocationBitmap2 == that.directoryAllocationBitmap2 && checkVectorSize == that.checkVectorSize && offset == that.offset && Arrays.equals(skewTab, that.skewTab);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(sectorSize, recordsPerTack, blockShiftFactor, blockMask, extentMask, storageSize, numDirectoryEntries, directoryAllocationBitmap1, directoryAllocationBitmap2, checkVectorSize, offset);
        result = 31 * result + Arrays.hashCode(skewTab);
        return result;
    }
}
