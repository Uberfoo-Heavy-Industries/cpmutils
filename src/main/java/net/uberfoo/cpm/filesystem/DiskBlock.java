package net.uberfoo.cpm.filesystem;

/**
 * Interface for classes that represent a block on a disk.
 */
public class DiskBlock {

    /**
     * The location of this block on disk.
     */
    protected final long index;

    /**
     * The disk parameters.
     */
    protected final DiskParameterBlock dpb;

    /**
     * The size of this block.
     */
    protected final int blockSize;

    /**
     * Creates a new block with the supplied pointer and parameters.
     *
     * @param index The block pointer where this block is to be located.
     * @param dpb The disk parameters.
     */
    public DiskBlock(long index, DiskParameterBlock dpb) {
        this.index = index;
        this.dpb = dpb;
        this.blockSize = dpb.getBlockSize();
    }

    /**
     * Creates a new block with the supplied pointer and parameters.
     * Accepts an arbitrary block size.
     *
     * @param index The block pointer where this block is to be located.
     * @param dpb The disk parameters.
     * @param blockSize The size of the block
     */
    public DiskBlock(long index, DiskParameterBlock dpb, int blockSize) {
        this.index = index;
        this.dpb = dpb;
        this.blockSize = blockSize;
    }

    /**
     * Get the location on disk where this block resides.
     *
     * @return A block pointer
     */
    public long getIndex() {
        return index;
    }

    /**
     * Gets the size of this block.
     *
     * @return A size
     */
    public int getBlockSize() {
        return blockSize;
    }
}
