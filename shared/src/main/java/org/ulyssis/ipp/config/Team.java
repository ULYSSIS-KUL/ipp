package org.ulyssis.ipp.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;



import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ulyssis.ipp.TagId;



import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

@JsonSerialize(using=Team.Serializer.class)
@JsonDeserialize(using=Team.Deserializer.class)
public final class Team {
	private static final Logger LOG = LogManager.getLogger(Team.class);
	
    static class Deserializer extends JsonDeserializer<Team> {
        @Override
        public Team deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            ObjectCodec oc = jp.getCodec();
            JsonNode node = oc.readTree(jp);
            Iterator<Map.Entry<String,JsonNode>> it = node.fields();
            Team team = new Team();
            while (it.hasNext()) {
                Map.Entry<String,JsonNode> entry = it.next();
                if (Objects.equals(entry.getKey(), "teamNb")) {
                    team.teamNb = entry.getValue().asInt();
                } else if (Objects.equals(entry.getKey(), "name")) {
                    team.name = entry.getValue().asText();
                } else if (Objects.equals(entry.getKey(), "tags")) {
                    ImmutableList.Builder<TagId> builder = ImmutableList.builder();
                    entry.getValue().elements().forEachRemaining(
                            tagNode -> {
                            	try {
                            		builder.add(new TagId(tagNode.asText()));	
                            	} catch (IllegalArgumentException e) {
                            		// TODO: HANDLE THIS EXCEPTION!!!!!
                            		LOG.error("Error decoding tag: {}", tagNode.asText(), e);
                            	}
                            });
                    team.tags = builder.build();
                }
            }
            return team;
        }
    }

    static class Serializer extends JsonSerializer<Team> {
        @Override
        public void serialize(Team value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeStartObject();
            jgen.writeFieldName("teamNb");
            jgen.writeNumber(value.teamNb);
            jgen.writeFieldName("name");
            jgen.writeString(value.name);
            if (!value.tags.isEmpty()) {
                jgen.writeFieldName("tags");
                jgen.writeStartArray();
                for (TagId tag : value.tags) {
                    jgen.writeString(tag.toString());
                }
                jgen.writeEndArray();
            }
            jgen.writeEndObject();
        }
    }

    private int teamNb;
    private String name;
    private ImmutableList<TagId> tags;

    private Team() {
        this.teamNb = 0;
        this.name = "";
        tags = ImmutableList.of();
    }

    public Team(int teamNb, String name) {
        this.teamNb = teamNb;
        this.name = name;
        this.tags = ImmutableList.of();
    }

    public Team(int teamNb, String name, ImmutableList<TagId> tags) {
        this.teamNb = teamNb;
        this.name = name;
        this.tags = tags;
    }

    public int getTeamNb() {
        return teamNb;
    }

    public String getName() {
        return name;
    }

    public ImmutableList<TagId> getTags() {
        return tags;
    }
}
