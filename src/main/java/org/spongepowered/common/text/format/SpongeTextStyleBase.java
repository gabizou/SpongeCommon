package org.spongepowered.common.text.format;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextElement;
import org.spongepowered.api.text.format.TextStyle;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.util.OptBool;

import java.util.Optional;

import javax.annotation.Nullable;

/**
 * Represents a {@link TextStyle} that is not a composite, for example
 * {@link TextStyles#BOLD}. It is a base text style in Minecraft with a
 * name.
 *
 * @see TextStyle
 * @see SpongeTextStyleBase
 */
public abstract class SpongeTextStyleBase extends TextStyle implements CatalogType, TextElement {

    /**
     * Whether text where this style is applied is bolded.
     */
    protected final @Nullable Boolean bold;
    /**
     * Whether text where this style is applied is italicized.
     */
    protected final @Nullable Boolean italic;
    /**
     * Whether text where this style is applied is underlined.
     */
    protected final @Nullable Boolean underline;
    /**
     * Whether text where this style is applied has a strikethrough.
     */
    protected final @Nullable Boolean strikethrough;
    /**
     * Whether text where this style is applied is obfuscated.
     */
    protected final @Nullable Boolean obfuscated;

    /**
     * Constructs a new {@link SpongeTextStyleBase}.
     *
     * @param bold Whether text where this style is applied is bolded
     * @param italic Whether text where this style is applied is italicized
     * @param underline Whether text where this style is applied is
     *        underlined
     * @param obfuscated Whether text where this style is applied is
     *        obfuscated
     * @param strikethrough Whether text where this style is applied has a
     *        strikethrough
     */
    SpongeTextStyleBase(@Nullable Boolean bold,
        @Nullable Boolean italic,
        @Nullable Boolean underline,
        @Nullable Boolean strikethrough,
        @Nullable Boolean obfuscated) {
        this.bold = bold;
        this.underline = underline;
        this.strikethrough = strikethrough;
        this.obfuscated = obfuscated;
        this.italic = italic;
    }

    /**
     * Utility method to check if the given "super-property" contains the given
     * "sub-property".
     *
     * @param superprop The super property
     * @param subprop The sub property
     * @return True if the property is contained, otherwise false
     */
    private static boolean propContains(Optional<Boolean> superprop, Optional<Boolean> subprop) {
        return !subprop.isPresent() || superprop.equals(subprop);
    }

    /**
     * Utility method to negate a property if it is not null.
     *
     * @param prop The property to negate
     * @return The negated property, or {@link Optional#empty()}
     */
    public static Optional<Boolean> propNegate(Optional<Boolean> prop) {
        if (prop.isPresent()) {
            return OptBool.of(!prop.get());
        }
        return OptBool.ABSENT;
    }

    /**
     * Utility method to perform a compose operation between two properties.
     *
     * @param prop1 The first property
     * @param prop2 The second property
     * @return The composition of the two properties
     */
    public static Optional<Boolean> propCompose(Optional<Boolean> prop1, Optional<Boolean> prop2) {
        if (!prop1.isPresent()) {
            return prop2;
        } else if (!prop2.isPresent()) {
            return prop1;
        } else if (!prop1.equals(prop2)) {
            return OptBool.ABSENT;
        } else {
            return prop1;
        }
    }

    @Override
    public boolean isComposite() {
        // By definition, base TextStyles are not composites
        return false;
    }

    @Override
    public boolean isEmpty() {
        return !(this.bold != null
                || this.italic != null
                || this.underline != null
                || this.strikethrough != null
                || this.obfuscated != null);
    }

    @Override
    public TextStyle bold(@Nullable Boolean bold) {
        return new SpongeTextStyle(
                bold,
                this.italic,
                this.underline,
                this.strikethrough,
                this.obfuscated
        );
    }

    @Override
    public TextStyle italic(@Nullable Boolean italic) {
        return new SpongeTextStyle(
                this.bold,
            italic,
                this.underline,
                this.strikethrough,
                this.obfuscated
        );
    }

    @Override
    public TextStyle underline(@Nullable Boolean underline) {
        return new SpongeTextStyle(
                this.bold,
                this.italic,
                underline,
                this.strikethrough,
                this.obfuscated
        );
    }

    @Override
    public TextStyle strikethrough(@Nullable Boolean strikethrough) {
        return new SpongeTextStyle(
                this.bold,
                this.italic,
                this.underline,
                strikethrough,
                this.obfuscated
        );
    }

    @Override
    public TextStyle obfuscated(@Nullable Boolean obfuscated) {
        return new SpongeTextStyle(
                this.bold,
                this.italic,
                this.underline,
                this.strikethrough,
                OptBool.of(obfuscated)
        );
    }

    @Override
    public Optional<Boolean> isBold() {
        return this.bold;
    }

    @Override
    public Optional<Boolean> isItalic() {
        return this.italic;
    }

    @Override
    public Optional<Boolean> hasUnderline() {
        return this.underline;
    }

    @Override
    public Optional<Boolean> hasStrikethrough() {
        return this.strikethrough;
    }

    @Override
    public Optional<Boolean> isObfuscated() {
        return this.obfuscated;
    }

    @Override
    public boolean contains(TextStyle... styles) {
        for (TextStyle style : checkNotNull(styles, "styles")) {
            checkNotNull(style, "style");
            if (!propContains(this.bold, style.bold)
                    || !propContains(this.italic, style.italic)
                    || !propContains(this.underline, style.underline)
                    || !propContains(this.strikethrough, style.strikethrough)
                    || !propContains(this.obfuscated, style.obfuscated)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public TextStyle negate() {
        // Do a negation of each property
        return new TextStyle(
                propNegate(this.bold),
                propNegate(this.italic),
                propNegate(this.underline),
                propNegate(this.strikethrough),
                propNegate(this.obfuscated)
        );
    }

    @Override
    public TextStyle and(TextStyle... styles) {
        return compose(styles, false);
    }

    @Override
    public TextStyle andNot(TextStyle... styles) {
        return compose(styles, true);
    }

    @Override
    protected TextStyle compose(TextStyle[] styles, boolean negate) {
        checkNotNull(styles, "styles");
        if (styles.length == 0) {
            return this;
        } else if (this.isEmpty() && styles.length == 1) {
            TextStyle style = checkNotNull(styles[0], "style");
            return negate ? style.negate() : style;
        }

        Optional<Boolean> boldAcc = this.bold;
        Optional<Boolean> italicAcc = this.italic;
        Optional<Boolean> underlineAcc = this.underline;
        Optional<Boolean> strikethroughAcc = this.strikethrough;
        Optional<Boolean> obfuscatedAcc = this.obfuscated;

        if (negate) {
            for (TextStyle style : styles) {
                checkNotNull(style, "style");
                boldAcc = propCompose(boldAcc, propNegate(style.bold));
                italicAcc = propCompose(italicAcc, propNegate(style.italic));
                underlineAcc = propCompose(underlineAcc, propNegate(style.underline));
                strikethroughAcc = propCompose(strikethroughAcc, propNegate(style.strikethrough));
                obfuscatedAcc = propCompose(obfuscatedAcc, propNegate(style.obfuscated));
            }
        } else {
            for (TextStyle style : styles) {
                checkNotNull(style, "style");
                boldAcc = propCompose(boldAcc, style.bold);
                italicAcc = propCompose(italicAcc, style.italic);
                underlineAcc = propCompose(underlineAcc, style.underline);
                strikethroughAcc = propCompose(strikethroughAcc, style.strikethrough);
                obfuscatedAcc = propCompose(obfuscatedAcc, style.obfuscated);
            }
        }

        return new SpongeTextStyle(
                boldAcc,
                italicAcc,
                underlineAcc,
                strikethroughAcc,
                obfuscatedAcc
        );
    }

    @Override
    public void applyTo(Text.Builder builder) {
        builder.style(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TextStyle)) {
            return false;
        }

        TextStyle that = (TextStyle) o;
        return this.bold.equals(that.bold)
                && this.italic.equals(that.italic)
                && this.underline.equals(that.underline)
                && this.obfuscated.equals(that.obfuscated)
                && this.strikethrough.equals(that.strikethrough);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.bold, this.italic, this.underline, this.obfuscated, this.strikethrough);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(TextStyle.class)
                .omitNullValues()
                .add("bold", this.bold.orElse(null))
                .add("italic", this.italic.orElse(null))
                .add("underline", this.underline.orElse(null))
                .add("strikethrough", this.strikethrough.orElse(null))
                .add("obfuscated", this.obfuscated.orElse(null))
                .toString();
    }
}
