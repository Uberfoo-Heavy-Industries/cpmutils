package net.uberfoo.z80.cpm22.filesystem.test;

import net.uberfoo.z80.cpm22.filesystem.DiskParameterBlock;

public class TestDiskParameterBlocks {

    public static final DiskParameterBlock Z80RB_DPB = new DiskParameterBlock(
            512,
            5,
            31,
            1,
            2047,
            511,
            240,
            0,
            0,
            0
    );

    public static final DiskParameterBlock Z80RB_MOD_ALCT_DPB = new DiskParameterBlock(
            512,
            5,
            31,
            1,
            2047,
            511,
            0xC0,
            0xC0,
            0,
            0
    );

}
