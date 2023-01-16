package net.uberfoo.cpm.filesystem;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Math.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileAlreadyExistsException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * Represents a single CP/M format disk.
 */
public class CpmDisk {

    private static final Logger LOG = LoggerFactory.getLogger(CpmDisk.class);

    /**
     * Represents a set of unique coordinates for a file allocation table entry.
     *
     * @param block Allocation block number of entry.
     * @param index Index of entry with the allocation block.
     */
    protected record EntryCoordinates(long block, int index) {}

    private final ByteBuffer buffer;
    private final DiskParameterBlock dpb;
    private List<AllocationBlock> allocationBlocks;

    /**
     * Creates a new disk with the supplied parameters
     * from the supplied file channel. This constructor
     * will map the file into memory.
     *
     * @param dpb The disk parameters.
     * @param channel The file channel of the disk image.
     * @throws IOException If there is a filesystem error.
     */
    public CpmDisk(DiskParameterBlock dpb, FileChannel channel) throws IOException {
        this(dpb, channel.map(FileChannel.MapMode.READ_WRITE, channel.position(), dpb.getFilesystemSize()));
    }

    /**
     * Creates a new disk with the supplied parameters
     * from the supplied byte buffer of a disk image.
     *
     * @param dpb The disk parameters.
     * @param buffer The buffer containing the disk image.
     * @throws IOException If there is a filesystem error.
     */
    public CpmDisk(DiskParameterBlock dpb, ByteBuffer buffer) throws IOException {
        LOG.info("Loading CP/M filesystem.");
        this.dpb = dpb;
        this.buffer = buffer;
        parseAllocationBlocks();
    }

    /**
     * Creates a new filesystem according to the supplied
     * disk parameters. The supplied byte buffer must be
     * positioned to the beginning of the location where
     * the filesystem will be written. The method will
     * fill the buffer with 0xE5 for block size bytes for
     * the number of blocks according to the disk parameters.
     *
     * @param dpb The disk parameters.
     * @param buffer The buffer to create the filesystem in.
     * @return A CpmDisk instance representing the new filesystem.
     * @throws IOException If an error occurs with the buffer.
     */
    public static CpmDisk makeFilesystem(DiskParameterBlock dpb, ByteBuffer buffer) throws IOException {
        LOG.info("Creating CP/M filesystem with disk parameter block: {}", dpb);
        int start = buffer.position();
        byte[] block = new byte[dpb.getBlockSize()];
        Arrays.fill(block, (byte)0xE5);
        IntStream.rangeClosed(0, dpb.storageSize())
                .forEach(i -> buffer.put(block));
        return new CpmDisk(dpb, buffer.slice(start, (int)dpb.getFilesystemSize()));
    }

    private void parseAllocationBlocks() throws IOException {
        allocationBlocks = new LinkedList<>();
        int dirMask = (dpb.directoryAllocationBitmap1() << 8) + dpb.directoryAllocationBitmap2();
        for(int i = 15 ; i >= 0 ; i--) {
            int bit = dirMask & (1 << i);
            if (bit > 0) {
                readAllocBlock(i);
            }
        }
    }

    /**
     * Refresh this disk by flushing to disk if this
     * is a memory mapped file. Then the allocation
     * table is reparsed from the buffer.
     *
     * @throws IOException If an error occurs with the buffer
     */
    public void refresh() throws IOException {
        if (buffer instanceof MappedByteBuffer mapped) {
            mapped.force();
        }
        parseAllocationBlocks(); // Re-read the directory blocks
    }

    /**
     * Gets a list of files on this disk.
     *
     * @return A list of files
     */
    public List<AllocationTableFile> getFiles() {
        return getFilesStream().toList();
    }

    /**
     * Gets a stream of all the files on this disk.
     *
     * @return A stream of files
     */
    public Stream<AllocationTableFile> getFilesStream() {
        return fileEntriesByName()
                .values().stream()
                .map(entries -> new AllocationTableFile(entries, dpb, buffer));
    }

    private void readAllocBlock(int i) throws IOException {
        int index = 15 - i;
        var buff = buffer.slice((dpb.getBlockSize() * index) + dpb.getOffsetBytes(), dpb.getBlockSize());
        allocationBlocks.add(new AllocationBlock(index, buff, dpb));
    }

    private Map<String, List<AllocationTableEntry>> fileEntriesByName() {
        return validEntries()
                .collect(Collectors.groupingBy(x -> x.getFullFilename()));
    }

    @Contract(pure = true)
    private static @NotNull Predicate<AllocationTableEntry> validEntriesOnly() {
        return x -> x.getStat() != 0xE5;
    }

    private Stream<AllocationTableEntry> allTables() {
        return allocationBlocks.stream()
                .flatMap(block -> Stream.of(block.getAllocationTable()));
    }

    /**
     * Gets a stream of the block points for every used block on this disk.
     *
     * @return A stream of block pointers
     */
    public Stream<Long> getUsedBlocks() {
        return validEntries()
                .flatMap(x -> x.getBlockPointers().stream());
    }

    private Stream<AllocationTableEntry> validEntries() {
        return allTables()
                .filter(validEntriesOnly());
    }

    /**
     * Gets a stream of block pointers for all unused
     * blocks on this disk.
     *
     * @return A stream of block pointers
     */
    public Stream<Long> getUnusedBlocks() {
        var usedBlocks = getUsedBlocks().toList();
        var usedAllocBlocks = allocationBlocks.stream()
                .map(x -> x.getIndex()).toList();
        return LongStream.rangeClosed(0, dpb.storageSize())
                .filter(x -> !usedBlocks.contains(x) && !usedAllocBlocks.contains(x))
                .boxed();
    }

    /**
     * Gets a stream of entry locations for all used
     * allocation entries on this disk.
     *
     * @return A stream of entry coordinates
     */
    public Stream<EntryCoordinates> getUsedEntries() {
        return allocationBlocks.stream()
                .flatMap(x -> x.getUsedEntries().map(y -> new EntryCoordinates(x.getIndex(), y)));
    }

    /**
     * Gets a stream of entry locations for all unused
     * allocation entries on this disk.
     *
     * @return A stream of entry coordinates
     */
    public Stream<EntryCoordinates> getUnusedEntries() {
        return allocationBlocks.stream()
                .flatMap(x -> x.getUnusedEntries().map(y -> new EntryCoordinates(x.getIndex(), y)));
    }

    /**
     * Gets the disk parameters for this disk.
     *
     * @return A disk parameter block
     */
    public DiskParameterBlock getDpb() {
        return dpb;
    }

    /**
     * Gets the size of the filesystem according
     * to the disk parameters.
     *
     * @return A size
     */
    public long size() {
        return dpb.getFilesystemSize();
    }

    /**
     * Looks for a file with the specified name
     * on this disk.
     *
     * @param filename The full name of the file.
     * @param stat The user number for the file.
     * @return An optional with the file if found.
     */
    public Optional<AllocationTableFile> findFile(@NotNull String filename, int stat) {
        return getFilesStream()
                .filter(x -> x.getFilename().equalsIgnoreCase(filename))
                .filter(x -> x.getStat() == stat)
                .findFirst();
    }

    /**
     * Creates a new file on this disk.
     *
     * @param filename The complete name of the new file
     * @param stat The user number of the new file
     * @param flags The flags for the new file
     * @param buffer The contents of the new file
     * @return A file entry
     * @throws IOException For general I/O errors
     * @throws FileAlreadyExistsException If this disk already contains a file with the supplied name.
     */
    public AllocationTableFile createFile(@NotNull String filename, int stat, @NotNull BitSet flags, @NotNull ByteBuffer buffer) throws IOException {
        if (findFile(filename, stat).isPresent()) {
            throw new FileAlreadyExistsException(stat + ": " + filename);
        }

        var size = buffer.capacity();
        var numBlocks = ceilDiv(size, dpb.getBlockSize());
        var numExtents = ceilDiv(numBlocks, 8);
        var block = ByteBuffer.allocate(dpb.getBlockSize());

        var blocks = getUnusedBlocks().limit(numBlocks).toList();

        int ex = 0;

        // Write the blocks to disk
        for (Long blockPtr : blocks) {
            block.clear();

            // Copy bytes until either block is full or there are no more bytes
            while (block.remaining() > 0 && buffer.hasRemaining()) {
                block.put(buffer.get());
            }

            // Fill the remaining buffer with 0's
            while (block.remaining() > 0) {
                block.put((byte) 0x00);
            }
            block.rewind();
            writeBlock(blockPtr, block);
        }

        var it = blocks.iterator();
        var entries = new ArrayList<AllocationTableEntry>(numExtents);

        for (var x : getUnusedEntries().limit(numExtents).toList()) {
            var entry = new AllocationTableEntry(x.block, x.index(), stat, ex++, filename, flags, dpb);
            IntStream.range(0, 8)
                    .forEachOrdered(i -> {
                        if (it.hasNext()) {
                            var next = it.next();
                            entry.addBlock(new DiskBlock(next, dpb, (!it.hasNext()) ? size % dpb.getBlockSize() : dpb.getBlockSize()));
                        }
                    });
            entries.add(entry);
        }

        // Write the file entries
        for (var entry : entries) {
            entry.writeEntry(this.buffer);
        }

        return new AllocationTableFile(entries, dpb, buffer);
    }

    /**
     * Deletes a file from this disk by marking its allocation table
     * entries with the delete stat (0xE5). The reminder of each entry
     * as well of the files block will remain untouched.
     *
     * @param filename The complete name of the file
     * @param stat The user number of the file
     * @throws IOException For general I/O errors
     * @throws FileNotFoundException If no file is found with the supplied name and user number.
     */
    public void deleteFile(@NotNull String filename, int stat) throws IOException {
        var file = getFilesStream()
                .filter(x -> x.getFilename().equalsIgnoreCase(filename))
                .filter(x -> x.getStat() == stat)
                .findFirst().orElseGet(() -> null);

        if (file == null) throw new FileNotFoundException(stat + ": " + filename);

        file.delete();
        refresh();
    }

    /**
     * Gets the list of allocation blocks on this disk.
     *
     * @return A list of blocks
     */
    public List<AllocationBlock> getAllocationBlocks() {
        return allocationBlocks;
    }

    private void writeBlock(long blockPointer, @NotNull ByteBuffer block) throws IOException {
        buffer.put((int) ((blockPointer * dpb.getBlockSize()) + dpb.getOffsetBytes()), block, 0, block.remaining());
    }
}
