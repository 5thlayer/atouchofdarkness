// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.light;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * Where Tough As Nails is loaded, the Campfire warms and the Soul Campfire cools by its tags (#13),
 * which count a block only while it is lit. Each tag adds to Tough As Nails' own, and holds nothing
 * else of the Pillar's, so the Torch and the Lamp give no heat or cold.
 */
class ToughAsNailsTagsTest {

    private static final Pattern ID = Pattern.compile("\"([a-z0-9_.-]+:[a-z0-9_./-]+)\"");

    @Test
    void theCampfireIsAHeatingBlock() throws IOException {
        assertAddsExactly("heating_blocks", "atouchofdarkness:campfire");
    }

    @Test
    void theSoulCampfireIsACoolingBlock() throws IOException {
        assertAddsExactly("cooling_blocks", "atouchofdarkness:soul_campfire");
    }

    /** The plain JVM has no JSON parser, so this reads the tag file's text. */
    private static void assertAddsExactly(String tag, String... ids) throws IOException {
        // Gradle runs the tests from the project directory.
        var json = Files.readString(Path.of("src/main/resources/data/toughasnails/tags/block", tag + ".json"))
                .replaceAll("\\s", "");
        assertTrue(json.contains("\"replace\":false"), json);
        assertEquals(List.of(ids), ID.matcher(json).results().map(match -> match.group(1)).toList());
    }
}
