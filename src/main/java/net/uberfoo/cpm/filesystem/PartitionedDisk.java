package net.uberfoo.cpm.filesystem;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

public class PartitionedDisk {

    private List<LabeledDisk> disks;

    public PartitionedDisk() {
        disks = new LinkedList<>();
    }

    public PartitionedDisk(ByteBuffer buffer, PartitionTable table) throws IOException, ClassNotFoundException {
        disks = new ArrayList<>(table.size());
        for (var entry : table.getEntries()) {
            var diskBuffer = buffer.slice(entry.offset(), entry.diskParameterBlock().getFilesystemSize());
            disks.add(new LabeledDisk(entry.label(), new CpmDisk(entry.diskParameterBlock(), diskBuffer)));
        }
    }

    public PartitionedDisk(ByteBuffer buffer) throws IOException, ClassNotFoundException {
        this(buffer, new PartitionTable(buffer));
    }

    public List<LabeledDisk> getDisks() {
        return Collections.unmodifiableList(disks);
    }

    public boolean add(LabeledDisk disk) {
        return disks.add(disk);
    }

    public LabeledDisk get(int index) {
        return disks.get(index);
    }

    public Optional<LabeledDisk> get(String label) {
        return disks.stream().filter(x -> x.label().equals(label)).findFirst();
    }

    public ByteBuffer createDisk() throws IOException {
        var table = createPartitionTable();
        var encodedTable = table.encode();
        var buffer = ByteBuffer.allocate(table.diskSize() + encodedTable.limit());
        for (var disk : disks) {
            buffer.put(disk.disk().getBuffer());
        }
        buffer.put(encodedTable);

        return buffer.rewind();
    }

    private PartitionTable createPartitionTable() {
        int offset = 0;
        var table = new PartitionTable();
        for (var disk : disks) {
            var entry = new PartitionTableEntry(offset, disk.label(), disk.disk().getDpb());
            table.add(entry);
            offset += disk.disk().getDpb().getFilesystemSize();
        }
        return table;
    }
}
