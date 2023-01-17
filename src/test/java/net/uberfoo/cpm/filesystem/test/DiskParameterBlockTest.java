package net.uberfoo.cpm.filesystem.test;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.*;

import net.uberfoo.cpm.filesystem.DiskParameterBlock;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class DiskParameterBlockTest {

    @Test
    public void serializationTest() throws Exception {
        var dpb = TestDiskParameterBlocks.Z80RB_DPB;

        var out = new ByteArrayOutputStream();
        var oos = new ObjectOutputStream(out);

        oos.writeObject(dpb);

        var in = new ByteArrayInputStream(out.toByteArray());
        var ois = new ObjectInputStream(in);

        dpb = (DiskParameterBlock) ois.readObject();

        assertThat(dpb, equalTo(TestDiskParameterBlocks.Z80RB_DPB));
    }

}
