package net.uberfoo.cpm22.filesystem;

public class DiskBlock {

    protected final long index;
    protected final DiskParameterBlock dpb;

    protected final int blockSize;

    public DiskBlock(long index, DiskParameterBlock dpb) {
        this.index = index;
        this.dpb = dpb;
        this.blockSize = dpb.getBlockSize();
    }

    public DiskBlock(long index, DiskParameterBlock dpb, int blockSize) {
        this.index = index;
        this.dpb = dpb;
        this.blockSize = blockSize;
    }

    public long getIndex() {
        return index;
    }

    public int getBlockSize() {
        return blockSize;
    }
}
