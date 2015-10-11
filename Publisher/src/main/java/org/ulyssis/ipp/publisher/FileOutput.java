/*
 * Copyright (C) 2014-2015 ULYSSIS VZW
 *
 * This file is part of i++.
 * 
 * i++ is free software: you can redistribute it and/or modify
 * it under the terms of version 3 of the GNU Affero General Public License
 * as published by the Free Software Foundation. No other versions apply.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>
 */
package org.ulyssis.ipp.publisher;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ulyssis.ipp.utils.Serialization;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

public final class FileOutput implements ScoreOutput {
    private static final Logger LOG = LogManager.getLogger(FileOutput.class);
    // TODO: Do these permissions work on Windows?
    private static final Set<PosixFilePermission> defaultPerms = PosixFilePermissions.fromString("rw-r--r--");
    private final Path filePath;

    public FileOutput(Path filePath) {
        this.filePath = filePath;
    }

    @Override
    public void outputScore(Score score) {
        Path tmpFile = null;
        try {
            tmpFile = Files.createTempFile("score-", ".json", PosixFilePermissions.asFileAttribute(defaultPerms));
            BufferedWriter writer = Files.newBufferedWriter(tmpFile, StandardCharsets.UTF_8);
            Serialization.getJsonMapper().writer(new DefaultPrettyPrinter()).writeValue(writer, score);
            Files.move(tmpFile, filePath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOG.error("Error writing score to file!", e);
        } finally {
            try {
                if (tmpFile != null) Files.deleteIfExists(tmpFile);
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    public void cleanup() {
        // No cleanup necessary
    }
}
