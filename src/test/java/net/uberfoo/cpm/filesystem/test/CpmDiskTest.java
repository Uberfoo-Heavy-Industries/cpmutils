package net.uberfoo.cpm.filesystem.test;

import net.uberfoo.cpm.filesystem.CpmDisk;
import net.uberfoo.cpm.filesystem.AllocationTableFile;
import net.uberfoo.cpm.filesystem.DiskParameterBlock;
import org.hamcrest.MatcherAssert;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.BitSet;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static java.lang.Math.*;

public class CpmDiskTest {

    @Test
    public void testCreateRetrieveDeleteFile() throws Exception {
        var disk = makeZ80RBDisk();
        assertThat("File system is empty", disk.getFilesStream().count() == 0);

        try (var input = FileChannel.open(Path.of(ClassLoader.getSystemResource("sample-2mb-text-file.txt").toURI()), StandardOpenOption.READ)) {
            ByteBuffer inputBuffer = createFile("TEST.TXT", 0x00, disk, input);

            var fileEntry = disk.findFile("TEST.TXT", 0x00).orElseThrow();

            inputBuffer.rewind();

            assertFileIsCorrect(inputBuffer, fileEntry);
        }

        // Test can't find file under different user number
        MatcherAssert.assertThat("File is not present under different user number", disk.findFile("TEST.TXT", 0x01).isEmpty());

        disk.deleteFile("TEST.TXT", 0x00);
        MatcherAssert.assertThat("File is no longer present.", disk.findFile("TEST.TXT", 0x00).isEmpty());
        assertThat("File system is empty", disk.getFilesStream().count() == 0);
    }

    @Test
    public void testCreateRetrieveDeleteTinyFile() throws Exception {
        var disk = makeZ80RBDisk();
        assertThat("File system is empty", disk.getFilesStream().count() == 0);

        try (var input = FileChannel.open(Path.of(ClassLoader.getSystemResource("tinyfile.txt").toURI()), StandardOpenOption.READ)) {
            ByteBuffer inputBuffer = createFile("tiny", 0x05, disk, input);

            var fileEntry = disk.findFile("tiny", 0x05).orElseThrow();

            inputBuffer.rewind();

            assertFileIsCorrect(inputBuffer, fileEntry);
        }

        disk.deleteFile("tiny", 0x05);
        MatcherAssert.assertThat("File is no longer present.", disk.findFile("tiny", 0x05).isEmpty());
        assertThat("File system is empty", disk.getFilesStream().count() == 0);
    }

    @Test
    public void testFromGamesDisk() throws Exception {

        var diskBuffer = loadFilesystem("cpmdisk.img", TestDiskParameterBlocks.Z80RB_DPB);
        var disk = new CpmDisk(TestDiskParameterBlocks.Z80RB_DPB, diskBuffer);

        var fileBuffer = loadFile("tinyfile.txt");
        disk.createFile("tiny1",  0x00, new BitSet(11), fileBuffer);
        disk.refresh();
        fileBuffer.rewind();
        assertFileIsCorrect(fileBuffer, disk.findFile("tiny1", 0x00).orElseThrow());

        fileBuffer = loadFile("sample-2mb-text-file.txt");
        disk.createFile("large1",  0x00, new BitSet(11), fileBuffer);
        disk.refresh();
        fileBuffer.rewind();
        assertFileIsCorrect(fileBuffer, disk.findFile("large1", 0x00).orElseThrow());

        disk.deleteFile("tiny1", 0x00);
        assertThat("file is deleted", disk.findFile("tiny1", 0x00).isEmpty());

        fileBuffer = loadFile("BIGTREK.BAS");
        disk.createFile("BIGTREK2.BAS",  0x00, new BitSet(11), fileBuffer);
        disk.refresh();
        fileBuffer.rewind();
        assertFileIsCorrect(fileBuffer, disk.findFile("BIGTREK2.BAS", 0x00).orElseThrow());

    }

    private static ByteBuffer loadFile(String filename) throws Exception {
        try (FileChannel channel = FileChannel.open(Path.of(ClassLoader.getSystemResource(filename).toURI()))) {
            var buffer = ByteBuffer.allocate((int) channel.size());
            channel.read(buffer);
            return buffer.rewind();
        }
    }

    @NotNull
    private static ByteBuffer loadFilesystem(String filename, DiskParameterBlock dpb) throws Exception {
        try (FileChannel channel = FileChannel.open(Path.of(ClassLoader.getSystemResource(filename).toURI()))) {
            var buffer = ByteBuffer.allocate((int) dpb.getFilesystemSize());
            channel.read(buffer);
            return buffer.rewind();
        }
    }

    private static CpmDisk makeZ80RBDisk() throws IOException {
        return CpmDisk.makeFilesystem(TestDiskParameterBlocks.Z80RB_DPB, ByteBuffer.allocate((int) TestDiskParameterBlocks.Z80RB_DPB.getFilesystemSize()));
    }

    @NotNull
    private static ByteBuffer createFile(String name, int stat, CpmDisk disk, FileChannel input) throws IOException {
        var inputBuffer = ByteBuffer.allocate((int) input.size());
        input.read(inputBuffer);
        inputBuffer.rewind();

        disk.createFile(name, stat, new BitSet(11), inputBuffer);
        disk.refresh();
        return inputBuffer;
    }

    private static void assertFileIsCorrect(ByteBuffer inputBuffer, AllocationTableFile fileEntry) {
        var size = fileEntry.size();
        var inputSize = inputBuffer.remaining();

        // Size is same as input to the furthest 128 bytes
        assertThat(size, is(ceilDiv(inputSize, 128L) * 128));

        // Size difference is equal to the padding to the furthest 128 bytes
        assertThat(size - inputSize, equalTo((long)(128 - inputSize % 128) & 0x7F));

        // Assert that the contents minus the padding are identical
        assertThat(fileEntry.retrieveFileContents().slice(0, inputSize), equalTo(inputBuffer));

        byte[] padding = new byte[(int) (size - inputSize)];

        // Assert that file is padded with zeros
        assertThat(fileEntry.retrieveFileContents().slice(inputSize, (int)size - inputSize), equalTo(ByteBuffer.wrap(padding)));
    }
}
