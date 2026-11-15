package com.readsake.dev.dto.tag;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.readsake.dev.entities.Tag;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TagResponse {

    private UUID id;
    private String name;
    private String colorHex;
    private Boolean isSystem;

    public static TagResponse from(Tag t) {
        TagResponse r = new TagResponse();
        r.id       = t.getId();
        r.name     = t.getName();
        r.colorHex = t.getColorHex();
        r.isSystem = t.getIsSystem();
        return r;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }
    public Boolean getIsSystem() { return isSystem; }
    public void setIsSystem(Boolean isSystem) { this.isSystem = isSystem; }
}
