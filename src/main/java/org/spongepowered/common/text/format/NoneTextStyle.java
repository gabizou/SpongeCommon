package org.spongepowered.common.text.format;

import org.spongepowered.api.text.format.TextStyles;

/**
 * A private class that represents the type of the {@link TextStyles#NONE} text style.
 */
final class NoneTextStyle extends SpongeTextStyleBase {

    /**
     * Constructs a new {@link NoneTextStyle}.
     */
    NoneTextStyle() {
        super(
            null,
            null,
            null,
            null,
            null
        );
    }

    @Override
    public String getId() {
        return "NONE";
    }

    @Override
    public String getName() {
        return "NONE";
    }
}
