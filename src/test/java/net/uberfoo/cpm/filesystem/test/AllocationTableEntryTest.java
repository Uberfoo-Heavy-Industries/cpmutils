package net.uberfoo.cpm.filesystem.test;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import net.uberfoo.cpm.filesystem.AllocationTableEntry;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

public class AllocationTableEntryTest {

    private static final byte[] BIGTREK_ENTRY_1 = {
            (byte)0x00, (byte)0x42, (byte)0x49, (byte)0x47, (byte)0x54, (byte)0x52,
            (byte)0x45, (byte)0x4B, (byte)0x20, (byte)0x42, (byte)0x41, (byte)0x53,
            (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x80, (byte)0x09, (byte)0x00,
            (byte)0x0A, (byte)0x00, (byte)0x0B, (byte)0x00, (byte)0x0C, (byte)0x00,
            (byte)0x0D, (byte)0x00, (byte)0x0E, (byte)0x00, (byte)0x0F, (byte)0x00,
            (byte)0x10, (byte)0x00
    };

    private static final byte[] BIGTREK_ENTRY_2 = {
            (byte)0x00, (byte)0x42, (byte)0x49, (byte)0x47, (byte)0x54, (byte)0x52,
            (byte)0x45, (byte)0x4B, (byte)0x20, (byte)0x42, (byte)0x41, (byte)0x53,
            (byte)0x02, (byte)0x00, (byte)0x00, (byte)0x38, (byte)0x11, (byte)0x00,
            (byte)0x12, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0x00
    };

    private static final byte[] TESTTXT_ENTRY_1 = {
            (byte)0x1f, (byte)0x54, (byte)0x45, (byte)0x53, (byte)0x54, (byte)0x20,
            (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x54, (byte)0x58, (byte)0x54,
            (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x80, (byte)0x1C, (byte)0x01,
            (byte)0x1D, (byte)0x01, (byte)0x1E, (byte)0x01, (byte)0x1F, (byte)0x01,
            (byte)0x20, (byte)0x01, (byte)0x21, (byte)0x01, (byte)0x22, (byte)0x01,
            (byte)0x23, (byte)0x01
    };

    private static final byte[] TESTTXT_ENTRY_2 = {
            (byte)0x1f, (byte)0x54, (byte)0x45, (byte)0x53, (byte)0x54, (byte)0x20,
            (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x54, (byte)0x58, (byte)0x54,
            (byte)0x03, (byte)0x00, (byte)0x01, (byte)0x80, (byte)0xA4, (byte)0x01,
            (byte)0xA5, (byte)0x01, (byte)0xA6, (byte)0x01, (byte)0xA7, (byte)0x01,
            (byte)0xA8, (byte)0x01, (byte)0xA9, (byte)0x01, (byte)0xAA, (byte)0x01,
            (byte)0xAB, (byte)0x01
    };

    /* C:\Users\james\workspace\z80Utils\cpmdisk.img (1/8/2023 12:46:59 PM)
   StartOffset(h): 00001120, EndOffset(h): 0000113F, Length(h): 00000020 */

    private static final byte[] TESTTXT_ENTRY_3 = {
            (byte)0x1f, (byte)0x54, (byte)0x45, (byte)0x53, (byte)0x54, (byte)0x20,
            (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x54, (byte)0x58, (byte)0x54,
            (byte)0x04, (byte)0x00, (byte)0x04, (byte)0x28, (byte)0x2C, (byte)0x03,
            (byte)0x2D, (byte)0x03, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0x00
    };

    @Test
    public void testDecodeEntry1() throws Exception {
        var entry = new AllocationTableEntry(4, 10, BIGTREK_ENTRY_1, TestDiskParameterBlocks.Z80RB_DPB);

        assertThat(entry.getIndex(), equalTo(10));
        assertThat(entry.getAllocBlockPointer(), equalTo(4L));
        assertThat(entry.getExtent(), equalTo(0));
        assertThat(entry.getStat(), equalTo(0));
        assertThat(entry.getFilename(), equalTo("BIGTREK"));
        assertThat(entry.getExtension(), equalTo("BAS"));
        assertThat(entry.getFullFilename(), equalTo("BIGTREK.BAS"));
        assertThat(entry.getRecordCount(), equalTo(256));
        assertThat("flags should be empty", entry.getFlags().isEmpty());
        assertThat(entry.getBc(), equalTo(0));
        assertThat(entry.getBlockPointers(), hasSize(8));
        assertThat(entry.getBlockPointers(), contains(0x0009L, 0x000aL, 0x000bL, 0x000cL, 0x000dL, 0x000eL, 0x000fL, 0x0010L));

    }

    @Test
    public void testDecodeEntry2() throws Exception {
        var entry = new AllocationTableEntry(4, 11, BIGTREK_ENTRY_2, TestDiskParameterBlocks.Z80RB_DPB);

        assertThat(entry.getIndex(), equalTo(11));
        assertThat(entry.getAllocBlockPointer(), equalTo(4L));
        assertThat(entry.getExtent(), equalTo(1));
        assertThat(entry.getStat(), equalTo(0));
        assertThat(entry.getFilename(), equalTo("BIGTREK"));
        assertThat(entry.getExtension(), equalTo("BAS"));
        assertThat(entry.getFullFilename(), equalTo("BIGTREK.BAS"));
        assertThat(entry.getRecordCount(), equalTo(56));
        assertThat("flags should be empty", entry.getFlags().isEmpty());
        assertThat(entry.getBc(), equalTo(0));
        assertThat(entry.getBlockPointers(), hasSize(8));
        assertThat(entry.getBlockPointers(), contains(0x0011L, 0x0012L, 0x0000L, 0x0000L, 0x0000L, 0x0000L, 0x0000L, 0x0000L));

    }

    @Test
    public void testDecodeEntry3() throws Exception {
        var entry = new AllocationTableEntry(0, 0, TESTTXT_ENTRY_1, TestDiskParameterBlocks.Z80RB_DPB);

        assertThat(entry.getIndex(), equalTo(0));
        assertThat(entry.getAllocBlockPointer(), equalTo(0L));
        assertThat(entry.getExtent(), equalTo(0));
        assertThat(entry.getStat(), equalTo(31));
        assertThat(entry.getFilename(), equalTo("TEST"));
        assertThat(entry.getExtension(), equalTo("TXT"));
        assertThat(entry.getFullFilename(), equalTo("TEST.TXT"));
        assertThat(entry.getRecordCount(), equalTo(256));
        assertThat("flags should be empty", entry.getFlags().isEmpty());
        assertThat(entry.getBc(), equalTo(0));
        assertThat(entry.getBlockPointers(), hasSize(8));
        assertThat(entry.getBlockPointers(), contains(0x011cL, 0x011dL, 0x011eL, 0x011fL, 0x0120L, 0x0121L, 0x0122L, 0x0123L));

    }

    @Test
    public void testDecodeEntry4() throws Exception {
        var entry = new AllocationTableEntry(512, 256, TESTTXT_ENTRY_2, TestDiskParameterBlocks.Z80RB_DPB);

        assertThat(entry.getIndex(), equalTo(256));
        assertThat(entry.getAllocBlockPointer(), equalTo(512L));
        assertThat(entry.getExtent(), equalTo(17));
        assertThat(entry.getStat(), equalTo(31));
        assertThat(entry.getFilename(), equalTo("TEST"));
        assertThat(entry.getExtension(), equalTo("TXT"));
        assertThat(entry.getFullFilename(), equalTo("TEST.TXT"));
        assertThat(entry.getRecordCount(), equalTo(256));
        assertThat("flags should be empty", entry.getFlags().isEmpty());
        assertThat(entry.getBc(), equalTo(0));
        assertThat(entry.getBlockPointers(), hasSize(8));
        assertThat(entry.getBlockPointers(), contains(0x01a4L, 0x01a5L, 0x01a6L, 0x01a7L, 0x01a8L, 0x01a9L, 0x01aaL, 0x01abL));

    }

    @Test
    public void testDecodeEntry5() throws Exception {
        var entry = new AllocationTableEntry(1025, 513, TESTTXT_ENTRY_3, TestDiskParameterBlocks.Z80RB_DPB);

        assertThat(entry.getIndex(), equalTo(513));
        assertThat(entry.getAllocBlockPointer(), equalTo(1025L));
        assertThat(entry.getExtent(), equalTo(66));
        assertThat(entry.getStat(), equalTo(31));
        assertThat(entry.getFilename(), equalTo("TEST"));
        assertThat(entry.getExtension(), equalTo("TXT"));
        assertThat(entry.getFullFilename(), equalTo("TEST.TXT"));
        assertThat(entry.getRecordCount(), equalTo(40));
        assertThat("flags should be empty", entry.getFlags().isEmpty());
        assertThat(entry.getBc(), equalTo(0));
        assertThat(entry.getBlockPointers(), hasSize(8));
        assertThat(entry.getBlockPointers(), contains(0x032cL, 0x032dL, 0x0000L, 0x0000L, 0x0000L, 0x0000L, 0x0000L, 0x0000L));

    }

    @Test
    public void testEncodeFromDecodes() throws Exception {
        var buffer = ByteBuffer.allocate((int)TestDiskParameterBlocks.Z80RB_DPB.getFilesystemSize());
        var entry = new AllocationTableEntry(0, 0, BIGTREK_ENTRY_1, TestDiskParameterBlocks.Z80RB_DPB);
        entry.writeEntry(buffer);

        var bufferSlice = buffer.slice(0, 32);
        assertThat(bufferSlice, equalTo(ByteBuffer.wrap(BIGTREK_ENTRY_1)));

        buffer.clear();
        entry = new AllocationTableEntry(0, 2, BIGTREK_ENTRY_2, TestDiskParameterBlocks.Z80RB_DPB);
        entry.writeEntry(buffer);

        bufferSlice = buffer.slice(2 * 32, 32);
        assertThat(bufferSlice, equalTo(ByteBuffer.wrap(BIGTREK_ENTRY_2)));

        buffer.clear();
        entry = new AllocationTableEntry(0, 16, TESTTXT_ENTRY_1, TestDiskParameterBlocks.Z80RB_DPB);
        entry.writeEntry(buffer);

        bufferSlice = buffer.slice(16 * 32, 32);
        assertThat(bufferSlice, equalTo(ByteBuffer.wrap(TESTTXT_ENTRY_1)));

        buffer.clear();
        entry = new AllocationTableEntry(4, 0, TESTTXT_ENTRY_2, TestDiskParameterBlocks.Z80RB_DPB);
        entry.writeEntry(buffer);

        bufferSlice = buffer.slice(TestDiskParameterBlocks.Z80RB_DPB.getBlockSize() * 4, 32);
        assertThat(bufferSlice, equalTo(ByteBuffer.wrap(TESTTXT_ENTRY_2)));

        buffer.clear();
        entry = new AllocationTableEntry(3, 33, TESTTXT_ENTRY_3, TestDiskParameterBlocks.Z80RB_DPB);
        entry.writeEntry(buffer);

        bufferSlice = buffer.slice((TestDiskParameterBlocks.Z80RB_DPB.getBlockSize() * 3) + (33 * 32), 32);
        assertThat(bufferSlice, equalTo(ByteBuffer.wrap(TESTTXT_ENTRY_3)));
    }
}
