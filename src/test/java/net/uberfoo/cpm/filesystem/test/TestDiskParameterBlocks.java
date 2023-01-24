package net.uberfoo.cpm.filesystem.test;

import static net.uberfoo.cpm.filesystem.DiskParameterBlock.createSkewTab;

import net.uberfoo.cpm.filesystem.DiskParameterBlock;

public class TestDiskParameterBlocks {

    public static final DiskParameterBlock Z80RB_DPB = new DiskParameterBlock(
            512,
            128,
            5,
            31,
            1,
            2047,
            511,
            240,
            0,
            0,
            0,
            new int[0]
    );

    public static final DiskParameterBlock Z80RB_BOOT_DPB = new DiskParameterBlock(
            512,
            128,
            5,
            31,
            1,
            2047,
            511,
            240,
            0,
            0,
            1,
            new int[0]
    );

    public static final DiskParameterBlock Z80RB_MOD_ALCT_DPB = new DiskParameterBlock(
            128,
            512,
            5,
            31,
            1,
            2047,
            511,
            0xC0,
            0xC0,
            0,
            0,
            new int[0]
    );


    public static final DiskParameterBlock OSBORNE_1_DPB = new DiskParameterBlock(
            256,
            20,
            4,
            15,
            1,
            45,
            63,
            0x80,
            0x00,
            0,
            3,
            createSkewTab(2, 10)
    );

}
