package com.student.performance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A learning resource (tutorial video, documentation page or search) a student
 * opened for a topic. The unique resourceKey makes re-opening the same link
 * update the existing row instead of inserting a duplicate, so generation can
 * reason about what the student has actually looked at.
 *
 * <p>The position and content fields are what turn a link into a quiz about
 * something specific: {@code watchedSeconds} says how far into a video the
 * student got, and {@code contentText} caches what the resource said up to that
 * point - the captions of a video, or the readable text of a page. Without them
 * the generator only knows the topic name, and the questions drift.
 */
@Entity
@Table(name = "resource_views",
        uniqueConstraints = @UniqueConstraint(name = "uq_resource_view_key", columnNames = "resource_key"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResourceView {

    /** How the text in {@link #contentText} was obtained. */
    public enum ContentSource { TRANSCRIPT, PAGE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "view_id")
    private Long viewId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 10)
    private ResourceType resourceType = ResourceType.VIDEO;

    @Column(name = "resource_title", nullable = false, length = 255)
    private String resourceTitle;

    @Column(name = "resource_url", nullable = false, length = 500)
    private String resourceUrl;

    @Column(name = "resource_key", nullable = false, length = 64)
    private String resourceKey;

    @Column(name = "progress_label", length = 120)
    private String progressLabel;

    /** How far into the video the student had played, in seconds. */
    @Column(name = "watched_seconds")
    private Integer watchedSeconds;

    /** How long the video is, in seconds, once the player has reported it. */
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    /**
     * The text of the resource up to the watched position, so generation can
     * write the questions from it instead of from the topic name alone.
     */
    @Column(name = "content_text", columnDefinition = "MEDIUMTEXT")
    private String contentText;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_source", length = 20)
    private ContentSource contentSource;

    /**
     * The position {@link #contentText} covers, in seconds. A stored extract is
     * only reused for the position it was read at, so watching further is never
     * answered from a stale, short transcript.
     */
    @Column(name = "content_seconds")
    private Integer contentSeconds;

    @Column(name = "first_viewed_at", nullable = false)
    private LocalDateTime firstViewedAt;

    @Column(name = "last_viewed_at", nullable = false)
    private LocalDateTime lastViewedAt;

    @Column(name = "view_count", nullable = false)
    private int viewCount = 1;

    public enum ResourceType { VIDEO, DOC, SEARCH }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (firstViewedAt == null) {
            firstViewedAt = now;
        }
        if (lastViewedAt == null) {
            lastViewedAt = now;
        }
    }
}
