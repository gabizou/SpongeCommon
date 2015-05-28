package org.spongepowered.common.data.component.block;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spongepowered.common.data.DataTransactionBuilder.fail;
import static org.spongepowered.common.data.DataTransactionBuilder.successNoData;
import static org.spongepowered.common.data.DataTransactionBuilder.successReplaceData;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import org.spongepowered.api.data.Component;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.component.block.WireAttachmentComponent;
import org.spongepowered.api.data.type.WireAttachmentType;
import org.spongepowered.api.data.type.WireAttachmentTypes;
import org.spongepowered.api.util.Direction;
import org.spongepowered.common.data.component.AbstractMappedComponent;

import java.util.Collection;
import java.util.Map;

public class SpongeWiredTypeComponent extends AbstractMappedComponent<Direction, WireAttachmentType, WireAttachmentComponent> implements WireAttachmentComponent {

    public SpongeWiredTypeComponent() {
        super(WireAttachmentComponent.class);
    }

    @Override
    public DataTransactionResult set(Direction key, WireAttachmentType value) {
        if (!checkNotNull(key).isCardinal()) {
            return fail(new SpongeWiredTypeComponent().setUnsafe(key, value));
        }
        if (this.keyValueMap.containsKey(key)) {
            DataTransactionResult result = successReplaceData(new SpongeWiredTypeComponent().setUnsafe(key, value));
            this.keyValueMap.put(key, checkNotNull(value));
            return result;
        }
        this.keyValueMap.put(key, checkNotNull(value));
        return successNoData();
    }

    @Override
    public DataTransactionResult set(Map<Direction, WireAttachmentType> mapped) {
        ImmutableSet.Builder<Component<?>> builder = ImmutableSet.builder();
        for (Map.Entry<Direction, WireAttachmentType> entry : mapped.entrySet()) {
            DataTransactionResult result = set(entry.getKey(), entry.getValue());
            if (result.getType() == DataTransactionResult.Type.SUCCESS) {
                if (result.getReplacedData().isPresent()) {
                    builder.addAll(result.getReplacedData().get());
                }
            } else {
                rejected(result, builder.build());
            }
        }
        return succesReplaced(builder.build());
    }

    @Override
    public DataTransactionResult set(Direction... mapped) {
        checkNotNull(mapped);
        ImmutableSet.Builder<Component<?>> builder = ImmutableSet.builder();
        for (Direction direction : mapped) {
            DataTransactionResult result = set(direction, WireAttachmentTypes.NONE);
            if (result.getType() == DataTransactionResult.Type.SUCCESS) {
                if (result.getReplacedData().isPresent()) {
                    builder.addAll(result.getReplacedData().get());
                }
            } else {
                rejected(result, builder.build());
            }
        }
        return succesReplaced(builder.build());
    }

    @Override
    public WireAttachmentComponent copy() {
        return new SpongeWiredTypeComponent().setUnsafe(this.keyValueMap);
    }

    @Override
    public int compareTo(WireAttachmentComponent o) {
        return 0;
    }

    @Override
    public DataContainer toContainer() {
        return null;
    }

    private DataTransactionResult succesReplaced(final ImmutableSet<Component<?>> set) {
        return new DataTransactionResult() {
            @Override
            public Type getType() {
                return Type.SUCCESS;
            }

            @Override
            public Optional<Collection<Component<?>>> getRejectedData() {
                return Optional.absent();
            }

            @Override
            public Optional<Collection<Component<?>>> getReplacedData() {
                return Optional.<Collection<Component<?>>>of(set);
            }
        };
    }

    private DataTransactionResult rejected(final DataTransactionResult result, final ImmutableSet<Component<?>> build) {
        return new DataTransactionResult() {
            @Override
            public Type getType() {
                return result.getType();
            }

            @Override
            public Optional<? extends Collection<? extends Component<?>>> getRejectedData() {
                return result.getRejectedData();
            }

            @Override
            public Optional<Collection<Component<?>>> getReplacedData() {
                return Optional.<Collection<Component<?>>>of(build);
            }
        };
    }
}
