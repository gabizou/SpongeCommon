package org.spongepowered.common.data.generator;

import java.util.List;

public class SerializationInfo {

    public final List<ValueGroupInfo> valueGroupInfos;
    public final int contentVersion;

    public SerializationInfo(List<ValueGroupInfo> valueGroupInfos, int contentVersion) {
        this.valueGroupInfos = valueGroupInfos;
        this.contentVersion = contentVersion;
    }
}
