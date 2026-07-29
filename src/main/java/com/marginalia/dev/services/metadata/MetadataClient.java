package com.marginalia.dev.services.metadata;
import java.util.Optional;
public interface MetadataClient { Optional<MetadataResult> find(String isbn13, String title, String author); }
