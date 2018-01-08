package org.spongepowered.common.text.format;

import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextStyle;

import java.util.Optional;

import javax.annotation.Nullable;

final class DummyTextStyle extends SpongeTextStyleBase {

    private final String name;

    DummyTextStyle(String fieldName) {
        super(false, false, false, false, false);
        this.name = fieldName;
    }

    @Override
    public boolean isComposite() {
        throw new UnsupportedOperationException("TextStyles." + this.name + " is not properly assigned!");
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException("TextStyles." + this.name + " is not properly assigned!");
    }

    @Override
    public TextStyle bold(@Nullable Boolean bold) {
        throw new UnsupportedOperationException("TextStyles." + this.name + " is not properly assigned!");
    }

    @Override
    public TextStyle italic(@Nullable Boolean italic) {
        throw new UnsupportedOperationException("TextStyles." + this.name + " is not properly assigned!");
    }

    @Override
    public TextStyle underline(@Nullable Boolean underline) {
        throw new UnsupportedOperationException("TextStyles." + this.name + " is not properly assigned!");
    }

    @Override
    public TextStyle strikethrough(@Nullable Boolean strikethrough) {
        throw new UnsupportedOperationException("TextStyles." + this.name + " is not properly assigned!");
    }

    @Override
    public TextStyle obfuscated(@Nullable Boolean obfuscated) {
        throw new UnsupportedOperationException("TextStyles." + this.name + " is not properly assigned!");
    }

    @Override
    public Optional<Boolean> isBold() {
        throw new UnsupportedOperationException("TextStyles." + this.name + " is not properly assigned!");
    }

    @Override
    public Optional<Boolean> isItalic() {
        throw new UnsupportedOperationException("TextStyles." + this.name + " is not properly assigned!");
    }

    @Override
    public Optional<Boolean> hasUnderline() {
        throw new UnsupportedOperationException("TextStyles." + this.name + " is not properly assigned!");
    }

    @Override
    public Optional<Boolean> hasStrikethrough() {
        throw new UnsupportedOperationException("TextStyles." + this.name + " is not properly assigned!");
    }

    @Override
    public Optional<Boolean> isObfuscated() {
        throw new UnsupportedOperationException("TextStyles." + this.name + " is not properly assigned!");
    }

    @Override
    public boolean contains(TextStyle... styles) {
        throw new UnsupportedOperationException("TextStyles." + this.name + " is not properly assigned!");
    }

    @Override
    public TextStyle negate() {
        throw new UnsupportedOperationException("TextStyles." + this.name + " is not properly assigned!");
    }

    @Override
    public TextStyle and(TextStyle... styles) {
        throw new UnsupportedOperationException("TextStyles." + this.name + " is not properly assigned!");
    }

    @Override
    public TextStyle andNot(TextStyle... styles) {
        throw new UnsupportedOperationException("TextStyles." + this.name + " is not properly assigned!");
    }

    @Override
    public void applyTo(Text.Builder builder) {
        throw new UnsupportedOperationException("TextStyles." + this.name + " is not properly assigned!");
    }

    @Override
    public String getId() {
        throw new UnsupportedOperationException("TextStyles." + this.name + " is not properly assigned!");
    }

    @Override
    public String getName() {
        throw new UnsupportedOperationException("TextStyles." + this.name + " is not properly assigned!");
    }
}
