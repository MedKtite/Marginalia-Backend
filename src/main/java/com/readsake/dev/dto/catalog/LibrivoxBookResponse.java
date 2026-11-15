package com.readsake.dev.dto.catalog;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * A free public-domain audiobook from LibriVox (librivox.org) — the audio
 * sibling of Project Gutenberg. Sections are individual chapter MP3s hosted
 * on the Internet Archive; the client plays them as a playlist.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LibrivoxBookResponse {

    private Long librivoxId;
    private String title;
    private String author;
    private Integer totalTimeSecs;
    private List<Section> sections = new ArrayList<>();

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Section {
        private String title;
        private String listenUrl;
        private Integer playtimeSecs;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getListenUrl() { return listenUrl; }
        public void setListenUrl(String listenUrl) { this.listenUrl = listenUrl; }

        public Integer getPlaytimeSecs() { return playtimeSecs; }
        public void setPlaytimeSecs(Integer playtimeSecs) { this.playtimeSecs = playtimeSecs; }
    }

    public Long getLibrivoxId() { return librivoxId; }
    public void setLibrivoxId(Long librivoxId) { this.librivoxId = librivoxId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public Integer getTotalTimeSecs() { return totalTimeSecs; }
    public void setTotalTimeSecs(Integer totalTimeSecs) { this.totalTimeSecs = totalTimeSecs; }

    public List<Section> getSections() { return sections; }
    public void setSections(List<Section> sections) { this.sections = sections; }
}
