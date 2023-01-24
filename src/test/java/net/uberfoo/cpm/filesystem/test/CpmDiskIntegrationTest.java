package net.uberfoo.cpm.filesystem.test;

import net.uberfoo.cpm.filesystem.CpmDisk;
import net.uberfoo.cpm.filesystem.DiskParameterBlock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.BitSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class CpmDiskIntegrationTest {

    static Logger logger = LogManager.getLogger(CpmDiskIntegrationTest.class);
    @Test
    public void testZ80RetroDiskImage() throws Exception {
        logger.info("Z80RetroDiskImage");
        try (FileChannel channel = FileChannel.open(Path.of(ClassLoader.getSystemResource("cpmdisk.img").toURI()),
                StandardOpenOption.WRITE, StandardOpenOption.READ)) {
            var fileBuffer = ByteBuffer.allocate((int) channel.size());
            channel.read(fileBuffer);
            fileBuffer.rewind();

            var input = FileChannel.open(Path.of(ClassLoader.getSystemResource("sample-2mb-text-file.txt").toURI()), StandardOpenOption.READ);

            var inputBuffer = ByteBuffer.allocate((int)input.size());
            input.read(inputBuffer);
            inputBuffer.rewind();

            var dpb = TestDiskParameterBlocks.Z80RB_DPB;
            CpmDisk disk = new CpmDisk(dpb, fileBuffer);


            disk.createFile("TEST.TXT", 0x00, new BitSet(11), inputBuffer);

            disk.refresh();

            var fileEntry = disk.findFile("TEST.TXT", 0x00).orElseThrow();

            var size = fileEntry.size();
            assertThat(size, is(2167808L));

            var f = fileEntry.retrieveFileContents();
            var slice = f.slice(0, inputBuffer.remaining());
            assertThat(slice.mismatch(inputBuffer), equalTo(-1));
        }

    }
}
