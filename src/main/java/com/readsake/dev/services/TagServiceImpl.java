package com.readsake.dev.services;

import com.readsake.dev.dto.tag.TagCreateRequest;
import com.readsake.dev.dto.tag.TagUpdateRequest;
import com.readsake.dev.entities.Tag;
import com.readsake.dev.exceptions.ConflictException;
import com.readsake.dev.exceptions.ForbiddenException;
import com.readsake.dev.exceptions.NotFoundException;
import com.readsake.dev.repo.dao.HighlightDao;
import com.readsake.dev.repo.dao.SubscriptionDao;
import com.readsake.dev.repo.dao.TagDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TagServiceImpl implements TagService {

    @Autowired
    private TagDao tagDao;

    @Autowired
    private HighlightDao highlightDao;

    @Autowired
    private SubscriptionDao subscriptionDao;

    @Override
    public List<Tag> listForUser(UUID userId) {
        return tagDao.findVisibleForUser(userId);
    }

    @Override
    @Transactional
    public Tag createCustom(TagCreateRequest req, UUID userId) {
        if (!subscriptionDao.isPro(userId)) {
            throw new ForbiddenException(
                    "Custom tags are a Pro feature. The 7 system tags are available on Free.");
        }
        try {
            return tagDao.insertCustom(userId, req.getName().trim(), req.getColorHex());
        } catch (DuplicateKeyException dup) {
            // Unique index on (lower(name), user_id) for user tags, or on system name.
            throw new ConflictException(
                    "A tag named '" + req.getName() + "' already exists.");
        }
    }

    @Override
    @Transactional
    public Tag updateCustom(UUID id, TagUpdateRequest req, UUID userId) {
        try {
            return tagDao.updateCustom(id, userId,
                            req.getName() != null ? req.getName().trim() : null,
                            req.getColorHex())
                    .orElseThrow(() -> new NotFoundException("Tag not found"));
        } catch (DuplicateKeyException dup) {
            throw new ConflictException(
                    "A tag named '" + req.getName() + "' already exists.");
        }
    }

    @Override
    @Transactional
    public void deleteCustom(UUID id, UUID userId) {
        if (!tagDao.deleteCustom(id, userId)) {
            throw new NotFoundException("Tag not found");
        }
        // highlight_tags rows cascade via FK ON DELETE CASCADE.
    }

    @Override
    @Transactional
    public void attachTag(UUID highlightId, UUID tagId, UUID userId) {
        // 1. Highlight must belong to caller.
        highlightDao.findByIdAndUser(highlightId, userId)
                .orElseThrow(() -> new NotFoundException("Highlight not found"));

        // 2. Tag must be system or owned by caller.
        tagDao.findVisibleByIdForUser(tagId, userId)
                .orElseThrow(() -> new NotFoundException("Tag not found"));

        tagDao.attachToHighlight(highlightId, tagId);
    }

    @Override
    @Transactional
    public void detachTag(UUID highlightId, UUID tagId, UUID userId) {
        highlightDao.findByIdAndUser(highlightId, userId)
                .orElseThrow(() -> new NotFoundException("Highlight not found"));
        tagDao.detachFromHighlight(highlightId, tagId);
    }

    @Override
    public List<Tag> listForHighlight(UUID highlightId, UUID userId) {
        highlightDao.findByIdAndUser(highlightId, userId)
                .orElseThrow(() -> new NotFoundException("Highlight not found"));
        return tagDao.findByHighlight(highlightId);
    }
}
