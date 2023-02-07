package net.uberfoo.cpm.filesystem.test;

import net.uberfoo.cpm.filesystem.PartitionTable;
import net.uberfoo.cpm.filesystem.PartitionTableEntry;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class PartitionTableTest {

    @Test
    public void testTableWriteThenRead() throws Exception {
        var table = new PartitionTable();

        for (int i = 0; i < 3; i++) {
            var entry = new PartitionTableEntry(0x800000 * i, Character.toString('A' + i), TestDiskParameterBlocks.Z80RB_DPB);
            table.add(entry);
        }

        var tableBuffer = table.encode();

        table = new PartitionTable(tableBuffer);

        assertThat(table.size(), is(3));

        table.stream().forEach(x -> System.out.println(x));
    }

    @Test
    public void testDiskSize() throws Exception {

        var table = new PartitionTable();

        for (int i = 0; i < 3; i++) {
            var entry = new PartitionTableEntry(0x80_0000 * i, Character.toString('A' + i), TestDiskParameterBlocks.Z80RB_DPB);
            table.add(entry);
        }

        assertThat(table.diskSize(), is(0x180_0000));
    }
}
