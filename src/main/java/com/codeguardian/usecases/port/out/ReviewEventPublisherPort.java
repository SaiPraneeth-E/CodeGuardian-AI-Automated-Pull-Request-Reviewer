package com.codeguardian.usecases.port.out;

import com.codeguardian.domain.model.ReviewJob;

/**
 * Output port for publishing Pull Request review events.
 */
public interface ReviewEventPublisherPort {
    /**
     * Publishes a review job task to the event stream.
     *
     * @param job the review job details
     */
    void publish(ReviewJob job);
}
