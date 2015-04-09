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
package org.ulyssis.ipp.control;

import org.junit.Test;
import org.ulyssis.ipp.control.commands.AddTagCommand;
import org.ulyssis.ipp.control.commands.Command;
import org.ulyssis.ipp.utils.Serialization;
import org.ulyssis.ipp.TagId;

import java.time.Instant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;

public class TestCommands {
    @Test
    public void testAddTagCommand_Serialize() throws Exception {
        AddTagCommand addTagCommand = new AddTagCommand(Instant.EPOCH, new TagId("abcd"), 1);
        assertThat(Serialization.getJsonMapper().writeValueAsString(addTagCommand),
                sameJSONAs("{" +
                             "\"type\": \"AddTag\"," +
                             "\"commandId\": \"" + addTagCommand.getCommandId() + "\"," +
                             "\"time\": 0," +
                             "\"tag\": \"abcd\"," +
                             "\"teamNb\": 1" +
                           "}"));
    }

    @Test
    public void testAddTagCommand_Deserialize() throws Exception {
        String addTagCommandStr = "{" +
                                    "\"type\": \"AddTag\"," +
                                    "\"commandId\": \"1234\"," +
                                    "\"time\": 0," +
                                    "\"tag\": \"ABCD\"," +
                                    "\"teamNb\": 1" +
                                   "}";
        Command command = Serialization.getJsonMapper().readValue(addTagCommandStr, Command.class);
        assertThat(command, instanceOf(AddTagCommand.class));
        AddTagCommand addTagCommand = (AddTagCommand)command;
        assertThat(addTagCommand.getCommandId(), equalTo("1234"));
        assertThat(addTagCommand.getTime(), equalTo(Instant.EPOCH));
        assertThat(addTagCommand.getTag(), equalTo(new TagId("abcd")));
        assertThat(addTagCommand.getTeamNb(), equalTo(1));
    }
}
