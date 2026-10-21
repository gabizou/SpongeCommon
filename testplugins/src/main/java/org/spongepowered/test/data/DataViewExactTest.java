/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.test.data;

import com.google.inject.Inject;
import net.kyori.adventure.text.Component;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.Server;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.BiFunction;

/**
 * Shows the difference between the lenient DataView getters (getInt, ...) and
 * the strict exact* getters, in memory and after a JSON or NBT round trip.
 *
 * <p>Runs once when the server has started (see the log) and on
 * {@code /dataviewexacttest [memory|json|nbt]}.</p>
 */
@Plugin("dataviewexacttest")
public final class DataViewExactTest {

    private final PluginContainer plugin;
    private final Logger logger;

    @Inject
    public DataViewExactTest(final PluginContainer plugin, final Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    @Listener
    private void onServerStarted(final StartedEngineEvent<Server> event) {
        for (final String format : List.of("memory", "json", "nbt")) {
            try {
                DataViewExactTest.demonstrate(format).forEach(line -> this.logger.info("[{}] {}", format, line));
            } catch (final IOException e) {
                this.logger.error("Round trip through {} failed", format, e);
            }
        }
    }

    @Listener
    private void onRegisterCommand(final RegisterCommandEvent<Command.Parameterized> event) {
        final Parameter.Value<String> format = Parameter.choices("memory", "json", "nbt").key("format").optional().build();
        event.register(this.plugin, Command.builder()
            .addParameter(format)
            .executor(context -> {
                final String chosen = context.one(format).orElse("memory");
                try {
                    DataViewExactTest.demonstrate(chosen).forEach(line -> context.sendMessage(Component.text(line)));
                } catch (final IOException e) {
                    throw new CommandException(Component.text("Round trip through " + chosen + " failed"), e);
                }
                return CommandResult.success();
            })
            .build(), "dataviewexacttest");
    }

    private static DataContainer sample() {
        return DataContainer.createNew()
            .set(DataQuery.of("level"), (byte) 7)
            .set(DataQuery.of("timestamp"), 1L)
            .set(DataQuery.of("ratio"), 0.1f)
            .set(DataQuery.of("bigCount"), 1L << 40)
            .set(DataQuery.of("count"), "12")
            .set(DataQuery.of("empty"), "")
            .set(DataQuery.of("name"), "Steve")
            .set(DataQuery.of("owner", "name"), "Alex")
            .set(DataQuery.of("enabled"), true);
    }

    private static DataView roundTrip(final String format, final DataContainer container) throws IOException {
        return switch (format) {
            case "json" -> DataFormats.JSON.get().read(DataFormats.JSON.get().write(container));
            case "nbt" -> {
                final ByteArrayOutputStream out = new ByteArrayOutputStream();
                DataFormats.NBT.get().writeTo(out, container);
                yield DataFormats.NBT.get().readFrom(new ByteArrayInputStream(out.toByteArray()));
            }
            default -> container;
        };
    }

    static List<String> demonstrate(final String format) throws IOException {
        final DataView view = DataViewExactTest.roundTrip(format, DataViewExactTest.sample());
        final List<String> lines = new ArrayList<>();
        lines.add("DataView getters after a " + format + " round trip (lenient result | exact result):");
        DataViewExactTest.compare(lines, view, DataQuery.of("level"), "byte", DataView::getByte, DataView::exactByte);
        DataViewExactTest.compare(lines, view, DataQuery.of("timestamp"), "long", DataView::getLong, (v, p) -> DataViewExactTest.box(v.exactLong(p)));
        DataViewExactTest.compare(lines, view, DataQuery.of("ratio"), "float", DataView::getFloat, DataView::exactFloat);
        DataViewExactTest.compare(lines, view, DataQuery.of("bigCount"), "int", DataView::getInt, (v, p) -> DataViewExactTest.box(v.exactInt(p)));
        DataViewExactTest.compare(lines, view, DataQuery.of("count"), "int", DataView::getInt, (v, p) -> DataViewExactTest.box(v.exactInt(p)));
        DataViewExactTest.compare(lines, view, DataQuery.of("empty"), "long", DataView::getLong, (v, p) -> DataViewExactTest.box(v.exactLong(p)));
        DataViewExactTest.compare(lines, view, DataQuery.of("missing"), "int", DataView::getInt, (v, p) -> DataViewExactTest.box(v.exactInt(p)));
        DataViewExactTest.compare(lines, view, DataQuery.of("name", "first"), "string", DataView::getString, DataView::exactString);
        DataViewExactTest.compare(lines, view, DataQuery.of("owner", "name"), "string", DataView::getString, DataView::exactString);
        DataViewExactTest.compare(lines, view, DataQuery.of("enabled"), "boolean", DataView::getBoolean, DataView::exactBoolean);
        return lines;
    }

    private static void compare(final List<String> lines, final DataView view, final DataQuery path, final String type,
        final BiFunction<DataView, DataQuery, Optional<?>> lenient, final BiFunction<DataView, DataQuery, Optional<?>> exact) {
        final String stored = view.get(path).map(o -> o.getClass().getSimpleName() + " " + o).orElse("nothing");
        String exactResult;
        try {
            exactResult = String.valueOf(exact.apply(view, path));
        } catch (final InvalidDataException e) {
            exactResult = "InvalidDataException: " + e.getMessage();
        }
        lines.add("  " + path + " as " + type + " [stored: " + stored + "]: " + lenient.apply(view, path) + " | " + exactResult);
    }

    private static Optional<Integer> box(final OptionalInt value) {
        return value.isPresent() ? Optional.of(value.getAsInt()) : Optional.empty();
    }

    private static Optional<Long> box(final OptionalLong value) {
        return value.isPresent() ? Optional.of(value.getAsLong()) : Optional.empty();
    }
}
