package com.brianna.jobsearch.service;

import com.brianna.jobsearch.model.PrepItem;
import com.brianna.jobsearch.model.PrepItemType;
import com.brianna.jobsearch.repository.PrepItemRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PrepService {

    private final PrepItemRepository repository;

    public PrepService(PrepItemRepository repository) {
        this.repository = repository;
    }

    public List<PrepItem> search(String query, PrepItemType type, Long applicationId) {
        return repository.findAll(query, type, applicationId);
    }

    public PrepItem get(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Prep item not found: " + id));
    }

    public List<PrepItem> forApplication(long applicationId) {
        return repository.findForApplication(applicationId);
    }

    public List<PrepItem> linkableReusableForApplication(long applicationId) {
        return repository.findLinkableReusable(applicationId);
    }

    @Transactional
    public void linkToApplication(long prepItemId, long applicationId) {
        get(prepItemId);
        repository.linkToApplication(prepItemId, applicationId);
    }

    @Transactional
    public void unlinkFromApplication(long prepItemId, long applicationId) {
        PrepItem item = get(prepItemId);
        if (item.getApplicationId() != null && item.getApplicationId() == applicationId) {
            throw new IllegalArgumentException("Application-specific prep items are linked by ownership. Edit the prep item to make it reusable instead.");
        }
        repository.unlinkFromApplication(prepItemId, applicationId);
    }

    @Transactional
    public long create(PrepItem item) {
        normalize(item);
        return repository.save(item);
    }

    @Transactional
    public void update(PrepItem item) {
        get(item.getId());
        normalize(item);
        if (item.getApplicationId() != null) {
            repository.clearLinksForPrepItem(item.getId());
        }
        repository.update(item);
    }

    @Transactional
    public PrepItem markReviewed(long id, int confidence) {
        get(id);
        repository.markReviewed(id, Math.max(1, Math.min(5, confidence)));
        return get(id);
    }

    @Transactional
    public void delete(long id) {
        get(id);
        repository.delete(id);
    }

    public PrepSnapshot snapshot() {
        return new PrepSnapshot(
                repository.countAll(),
                repository.countByType(PrepItemType.TECHNICAL_TOPIC),
                repository.countByType(PrepItemType.STAR_STORY),
                repository.countLinked(),
                repository.countNeedsReview(),
                repository.findNeedsReview(6));
    }

    private void normalize(PrepItem item) {
        if (item.getType() == null) {
            item.setType(PrepItemType.TECHNICAL_TOPIC);
        }
        item.setConfidence(item.getSafeConfidence());
        if (item.getApplicationId() != null && item.getApplicationId() <= 0) {
            item.setApplicationId(null);
        }
    }

    public record PrepSnapshot(
            long total,
            long technical,
            long starStories,
            long linked,
            long needsReviewCount,
            List<PrepItem> needsReview) {
    }
}
