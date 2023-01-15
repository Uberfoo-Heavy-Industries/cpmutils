package net.uberfoo.cpm.filesystem;

import static java.lang.Math.*;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Represents a single entry on the file allocation table.
 * A file can be represented as one or more table entries.
 */
public class AllocationTableEntry {

    /**
     * Number of bytes in a single filesystem record.
     */
    public static final int RECORD_LEN = 128;

    private final int stat;
    private String filename;
    private String extension;
    private BitSet flags;
    private final int extent;
    private int recordCount;
    private int bc;
    private List<Long> blockPointers;

    private final int index;
    private final long allocBlockPointer;

    private final DiskParameterBlock dpb;

    /**
     * Creates a file allocation table entry based on a set of parameters.
     *
     * @param allocBlockPointer Block pointer to the allocation block containing this entry.
     * @param index Tables index containing this entry.
     * @param stat Stat value. Usually the user number.
     * @param extent The extent number for this entry.
     * @param filename The file name for this entry.
     * @param flags The flag bits for this entry.
     * @param dpb The disk parameters to use when constructing this entry.
     */
    public AllocationTableEntry(long allocBlockPointer, int index, int stat, int extent, @NotNull String filename, @NotNull BitSet flags, @NotNull DiskParameterBlock dpb) {
        var split = filename.split("\\.");
        this.filename = split[0];
        this.extension = split.length > 1 ? split[1].substring(0, 3) : "";

        this.stat = stat;
        this.flags = flags;
        this.allocBlockPointer = allocBlockPointer;
        this.index = index;
        this.extent = extent;
        this.dpb = dpb;
        blockPointers = new LinkedList<>();
    }

    /**
     * Creates a file allocation table entry based on the bytes of an entry.
     *
     * @param allocBlockPointer Block pointer to the allocation block containing this entry.
     * @param index Tables index containing this entry.
     * @param entryBytes The array of bytes representing the entry.
     * @param dpb The disk parameters to use when parsing this entry.
     */
    public AllocationTableEntry(long allocBlockPointer, int index, @NotNull byte[] entryBytes, @NotNull DiskParameterBlock dpb) {
        this.index = index;
        this.allocBlockPointer = allocBlockPointer;
        this.dpb = dpb;
        flags = new BitSet(11);

        stat = Byte.toUnsignedInt(entryBytes[0]);

        byte[] fileNameBytes = Arrays.copyOfRange(entryBytes, 1, 12);
        IntStream.range(0, 11)
                .forEachOrdered(j -> {
                    if ((fileNameBytes[j] & 128) > 0) {
                        flags.set(j);
                    }
                    fileNameBytes[j] &= 0x7F;
                });

        extension = new String(fileNameBytes).substring(8).trim();
        filename = new String(fileNameBytes).substring(0, 8).trim();

        int xl = Byte.toUnsignedInt(entryBytes[12]); // Extent, low byte
        bc = Byte.toUnsignedInt(entryBytes[13]);  // Byte count, unused in CP/M 2.2
        int xh = Byte.toUnsignedInt(entryBytes[14]); // Extent, high byte
        int rc = Byte.toUnsignedInt(entryBytes[15]); // Record count, low bits

        // Decode the record count and extent number
        recordCount = ((int)(xl & dpb.extentMask()) << 7) + rc;
        extent = (xh << ((9 - dpb.extentMask()) >> 1)) + (xl / (dpb.extentMask() + 1));

        byte[] blockPointerBytes = Arrays.copyOfRange(entryBytes, 16, 32);

        // TODO: Right now we just support disks greater than 256 blocks
        //  which means there are 8 block pointers per extent
        blockPointers = IntStream.range(0, 8)
                .mapToLong(x -> Byte.toUnsignedInt(blockPointerBytes[x * 2]) + (Byte.toUnsignedInt(blockPointerBytes[(x * 2) + 1]) << 8))
                .boxed().toList();

    }

    /**
     * Encodes the entry to bytes.
     *
     * @return Bytes of the entry.
     */
    protected byte[] encode() {
        byte[] bytes = new byte[AllocationBlock.ENTRY_SIZE];

        // Set stat value, used as user number
        bytes[0] = (byte)stat;

        // Copy file name
        var paddedName = String.format("%-8s", filename);
        IntStream.range(1, 9)
                .forEach(i -> bytes[i] = paddedName.getBytes()[i - 1]);

        // Copy file extension
        var paddedExt = String.format("%-3s", extension);
        IntStream.range(9, 12)
                .forEach(i -> bytes[i] = paddedExt.getBytes()[i - 9]);

        // Compute the extent low and high bytes
        bytes[12] = (byte)((extent * (dpb.extentMask() + 1))  & 0x1F); // Extent, low byte
        bytes[14] = (byte)(extent >> ((9 - dpb.extentMask()) / 2)); // Extent, high byte

        // Byte counter
        bytes[13] = 0x00; // 0 for CP/M 2.2, byte count of last record in CP/M 3

        // Compute lower bits of extent, low byte
        if (recordCount > 127) {
            bytes[12] |= ((recordCount - RECORD_LEN) >> 7) & dpb.extentMask();
        }

        // Calculate the low part of the record count
        // the cast to byte makes sure we only get the lower bits
        bytes[15] = (byte)(recordCount - (bytes[12] & dpb.extentMask()) * RECORD_LEN);

        // Copy in block pointers
        IntStream.range(0, blockPointers.size())
                .forEach(i -> {
                    // Low byte first
                    bytes[i * 2 + 16] = (byte)(blockPointers.get(i) & 0xFF);
                    // High byte next
                    bytes[i * 2 + 17] = (byte)((blockPointers.get(i) >> 8) & 0xFF);
                });

        // Process the flags if any
        flags.stream()
                .forEach(f -> {
                    // Set top bit for flag
                    bytes[f + 1] |= 0x80;
                });

        return bytes;
    }

    /**
     * Writes this entry to a disk image.
     *
     * @param buffer The ByteBuffer of the disk image to write to.
     * @throws IOException If a filesystem error occurs.
     */
    public void writeEntry(ByteBuffer buffer) throws IOException {
        buffer.put((int) (allocBlockPointer * dpb.getBlockSize() + AllocationBlock.ENTRY_SIZE * index), encode());
    }

    /**
     * Computes the complete file name including the extension if any.
     *
     * @return The full file name
     */
    public String getFullFilename() {
        return filename + (extension.isBlank() ? "" : "." + extension);
    }

    /**
     * Get the stat byte for this file. This usually represents the user number.
     *
     * @return The stat byte
     */
    public int getStat() {
        return stat;
    }

    /**
     * Gets the name portion of the file's name.
     *
     * @return The name.
     */
    public String getFilename() {
        return filename;
    }

    /**
     * Gets the extension portion of the file's name.
     *
     * @return The extention.
     */
    public String getExtension() {
        return extension;
    }

    /**
     * Gets the flag bits for the file entry.
     *
     * @return The flag bits
     */
    public BitSet getFlags() {
        return flags;
    }

    /**
     * Gets the extent number that indicate index in
     * the collection of entries of this entry.
     *
     * @return The extent number.
     */
    public int getExtent() {
        return extent;
    }

    /**
     * Gets the record count. This is the number of
     * 128 byte records that make up this entry.
     *
     * @return The count of records.
     */
    public int getRecordCount() {
        return recordCount;
    }

    /**
     * Gets the BC byte. Used only by CP/M 3 and above.
     *
     * @return The BC byte.
     */
    public int getBc() {
        return bc;
    }

    /**
     * Gets the list of block pointers for this entry.
     * There can be up to 8 block pointers in an entry.
     *
     * @return A list of block pointers
     */
    public List<Long> getBlockPointers() {
        return blockPointers;
    }

    /**
     * Gets the index of this entry within it's allocation table.
     *
     * @return The index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Gets the block pointer for this entry's allocation block.
     *
     * @return The block pointer
     */
    public long getAllocBlockPointer() {
        return allocBlockPointer;
    }

    @Override
    public String toString() {
        var hexValues = blockPointers.stream()
                .map(x -> String.format("0x%04x", x))
                .toArray();

        return String.format("Stat: 0x%02x, File name: %12s, bc: 0x%02x, extent: %2d, record count: %4d, flags: %s, blocks: %s", getStat(), getFullFilename(), getBc(), getExtent(), getRecordCount(), getFlags(), Arrays.toString(hexValues));
    }

    void addBlock(DiskBlock block) {
        recordCount = dpb.getBlockRecordCount() * (int)(blockPointers.stream().filter(x -> x != 0).count())
                + ceilDiv(block.getBlockSize(), RECORD_LEN);
        blockPointers.add(block.index);
    }


}
