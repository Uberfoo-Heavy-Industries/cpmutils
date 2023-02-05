package net.uberfoo.cpm.filesystem;

import java.io.Serializable;

public record PartitionTableEntry(int offset, String label, DiskParameterBlock diskParameterBlock) implements Serializable {

}
