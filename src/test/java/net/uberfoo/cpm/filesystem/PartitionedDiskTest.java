package net.uberfoo.cpm.filesystem;

import net.uberfoo.cpm.filesystem.test.TestDiskParameterBlocks;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class PartitionedDiskTest {

    @Test
    public void testCreateDisk() throws Exception {
        var disk1 = new LabeledDisk("BOOT", makeDisk(TestDiskParameterBlocks.Z80RB_BOOT_DPB));
        var disk2 = new LabeledDisk("Drive B", makeDisk(TestDiskParameterBlocks.Z80RB_DPB));
        var disk3 = new LabeledDisk("Drive C", makeDisk(TestDiskParameterBlocks.Z80RB_DPB));
        var disk4 = new LabeledDisk("Drive D", makeDisk(TestDiskParameterBlocks.Z80RB_DPB));
        var disk5 = new LabeledDisk("Drive E", makeDisk(TestDiskParameterBlocks.Z80RB_DPB));

        var partitionedDisk = new PartitionedDisk();
        partitionedDisk.add(disk1);
        partitionedDisk.add(disk2);
        partitionedDisk.add(disk3);
        partitionedDisk.add(disk4);
        partitionedDisk.add(disk5);

        var buffer = partitionedDisk.createDisk();

        partitionedDisk = new PartitionedDisk(buffer);

        assertThat(partitionedDisk.getDisks().get(0).label(), is("BOOT"));
        assertThat(partitionedDisk.getDisks().get(1).label(), is("Drive B"));
        assertThat(partitionedDisk.getDisks().get(2).label(), is("Drive C"));
        assertThat(partitionedDisk.getDisks().get(3).label(), is("Drive D"));
        assertThat(partitionedDisk.getDisks().get(4).label(), is("Drive E"));

        assertThat(partitionedDisk.getDisks(), hasSize(5));
        assertThat(partitionedDisk.get(0).disk().getBuffer().limit(), is(TestDiskParameterBlocks.Z80RB_BOOT_DPB.getFilesystemSize()));

    }

    private static CpmDisk makeDisk(DiskParameterBlock dpb) throws IOException {
        return CpmDisk.makeFilesystem(dpb, ByteBuffer.allocate((int) dpb.getFilesystemSize()));
    }

    @Test
    public void testReadDisk() throws Exception {
        var url = ClassLoader.getSystemResource("test_part_disk.img");
        var buff = ByteBuffer.wrap(Files.readAllBytes(Path.of(url.toURI())));

        var partitionedDisk = new PartitionedDisk(buff);

        var disks = partitionedDisk.getDisks();
        assertThat(disks, hasSize(3));

        assertThat(disks.get(0).label(), is("BOOT"));
        assertThat(disks.get(1).label(), is("Floppy Drive"));
        assertThat(disks.get(2).label(), is("Drive C"));

        assertThat(disks.get(0).disk().getDpb(), equalTo(TestDiskParameterBlocks.Z80RB_BOOT_DPB));
        assertThat(disks.get(1).disk().getDpb(), equalTo(TestDiskParameterBlocks.OSBORNE_1_DPB));
        assertThat(disks.get(2).disk().getDpb(), equalTo(TestDiskParameterBlocks.Z80RB_DPB));

        assertThat(disks.get(0).disk().getBuffer().limit(), is(TestDiskParameterBlocks.Z80RB_BOOT_DPB.getFilesystemSize()));
        assertThat(disks.get(1).disk().getBuffer().limit(), is(TestDiskParameterBlocks.OSBORNE_1_DPB.getFilesystemSize()));
        assertThat(disks.get(2).disk().getBuffer().limit(), is(TestDiskParameterBlocks.Z80RB_DPB.getFilesystemSize()));
    }

}
