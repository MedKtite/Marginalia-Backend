package com.marginalia.dev.controllers;

import com.marginalia.dev.dto.catalog.CatalogVolumeResponse;
import com.marginalia.dev.services.CatalogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Catalog search (Google Books proxy). Session-gated like the rest of /me/**;
 * the user adds a result to their library via POST /me/books.
 */
@RestController
@RequestMapping("/me/catalog")
public class CatalogController {

    @Autowired
    private CatalogService catalogService;

    @GetMapping("/search")
    public List<CatalogVolumeResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "20") int limit) {
        return catalogService.search(q, limit);
    }
}
