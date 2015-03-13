package org.ulyssis.ipp.snapshot;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.io.BaseEncoding.DecodingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ulyssis.ipp.processor.Processor;
import org.ulyssis.ipp.TagId;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@JsonSerialize(using = TeamTagMap.Serializer.class)
@JsonDeserialize(using = TeamTagMap.Deserializer.class)
public final class TeamTagMap {
    private static final Logger LOG = LogManager.getLogger(Processor.class);

    static class Serializer extends JsonSerializer<TeamTagMap> {
        @Override
        public void serialize(TeamTagMap teamTagMap, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
                throws IOException {
            jsonGenerator.writeStartObject();
            Multimap<Integer,TagId> teamToTags = HashMultimap.create(teamTagMap.getTagToTeam().size(), 1);
            teamTagMap.getTagToTeam().forEach((tag, team) -> teamToTags.put(team, tag));
            for (Map.Entry<Integer,Collection<TagId>> entry : teamToTags.asMap().entrySet()) {
                jsonGenerator.writeFieldName(String.valueOf(entry.getKey()));
                jsonGenerator.writeStartArray();
                for (TagId tag : entry.getValue()) {
                    jsonGenerator.writeString(tag.toString());
                }
                jsonGenerator.writeEndArray();
            }
            jsonGenerator.writeEndObject();
        }
    }

    static class Deserializer extends JsonDeserializer<TeamTagMap> {
        @Override
        public TeamTagMap deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
                throws IOException {
            final Map<TagId,Integer> tagToTeam = new HashMap<>();
            ObjectCodec oc = jsonParser.getCodec();
            JsonNode node = oc.readTree(jsonParser);
            node.fields().forEachRemaining((entry) -> {
                int team = Integer.parseInt(entry.getKey(), 10);
                entry.getValue().elements().forEachRemaining((tagNode) -> {
                    String tagStr = tagNode.asText();
                    try {
                    	tagToTeam.put(new TagId(tagStr), team);
                    } catch (DecodingException e) {
                    	// TODO: Handle this exception!!!!
                    	LOG.error("Error decoding tag id: {}", tagStr, e);
                    }
                });
            });
            return new TeamTagMap(ImmutableMap.<TagId,Integer>builder()
                .putAll(tagToTeam).build());
        }
    }

    // NOTE: List<Byte> is used, because byte[] has no proper equals()
    private final ImmutableMap<TagId,Integer> tagToTeam;

    public TeamTagMap() {
        tagToTeam = ImmutableMap.of();
    }

    private TeamTagMap(ImmutableMap<TagId,Integer> tagToTeam) {
        this.tagToTeam = tagToTeam;
    }

    public TeamTagMap addTagToTeam(String tag, int team) throws DecodingException {
        return addTagToTeam(new TagId(tag), team);
    }

    public TeamTagMap addTagToTeam(TagId tag, int team) {
        if (tagToTeam.containsKey(tag)) {
            if (tagToTeam.get(tag) != team) {
                LOG.error("The tag {} was already assigned to team {}, ignoring!",
                        tag, tagToTeam.get(tag));
            }
            return this;
        }

        return new TeamTagMap(
                ImmutableMap.<TagId, Integer>builder()
                        .putAll(tagToTeam)
                        .put(tag, team)
                        .build()
        );
    }

    public TeamTagMap removeTag(String tag) throws DecodingException {
        return removeTag(new TagId(tag));
    }

    public TeamTagMap removeTag(TagId tag) {
        if (!tagToTeam.containsKey(tag)) {
            return this;
        }
        return new TeamTagMap(ImmutableMap.copyOf(Maps.filterKeys(tagToTeam, t -> !tag.equals(t))));
    }

    public Optional<Integer> tagToTeam(String tag) throws DecodingException {
        return tagToTeam(new TagId(tag));
    }

    public Optional<Integer> tagToTeam(TagId tag) {
        if (tagToTeam.containsKey(tag)) {
            return Optional.of(tagToTeam.get(tag));
        } else {
            return Optional.empty();
        }
    }

    public Map<TagId, Integer> getTagToTeam() {
        return tagToTeam;
    }
}
