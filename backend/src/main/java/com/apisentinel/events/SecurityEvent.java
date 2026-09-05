package com.apisentinel.events;

import com.apisentinel.detection.ThreatDetection;
import com.apisentinel.gateway.ApiRequest;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Security incident record triggered when a threat is identified.
 */
@Entity
@Table(
        name = "security_events",
        indexes = {
                @Index(name = "idx_sec_event_id", columnList = "event_id", unique = true),
                @Index(name = "idx_sec_event_type", columnList = "threat_type"),
                @Index(name = "idx_sec_event_severity", columnList = "severity"),
                @Index(name = "idx_sec_event_score", columnList = "threat_score"),
                @Index(name = "idx_sec_event_source", columnList = "source"),
                @Index(name = "idx_sec_event_time", columnList = "timestamp")
        }
)
public class SecurityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 64)
    @Column(name = "event_id", nullable = false, unique = true, length = 64)
    private String eventId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "threat_type", nullable = false, length = 40)
    private ThreatType threatType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private ThreatSeverity severity;

    @NotNull
    @Column(name = "threat_score", nullable = false)
    private Double threatScore = 0.0;

    @NotBlank
    @Size(max = 255)
    @Column(name = "reason", nullable = false, length = 255)
    private String reason;

    @Column(name = "evidence", columnDefinition = "TEXT")
    private String evidence;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "action_taken", nullable = false, length = 30)
    private MitigationAction actionTaken = MitigationAction.ALLOWED;

    @Size(max = 500)
    @Column(name = "endpoint", length = 500)
    private String endpoint;

    @NotBlank
    @Size(max = 100)
    @Column(name = "source", nullable = false, length = 100)
    private String source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_request_id")
    private ApiRequest apiRequest;

    @JsonIgnore
    @OneToMany(mappedBy = "securityEvent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ThreatDetection> detections = new ArrayList<>();

    @NotNull
    @Column(name = "timestamp", nullable = false, updatable = false)
    private Instant timestamp;

    public SecurityEvent() {
    }

    public SecurityEvent(String eventId, ThreatType threatType, ThreatSeverity severity, Double threatScore,
                         String reason, MitigationAction actionTaken, String endpoint, String source) {
        this.eventId = eventId;
        this.threatType = threatType;
        this.severity = severity;
        this.threatScore = threatScore;
        this.reason = reason;
        this.actionTaken = actionTaken != null ? actionTaken : MitigationAction.ALLOWED;
        this.endpoint = endpoint;
        this.source = source;
        this.timestamp = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.timestamp == null) {
            this.timestamp = Instant.now();
        }
    }

    public void addDetection(ThreatDetection detection) {
        detections.add(detection);
        detection.setSecurityEvent(this);
    }

    public void removeDetection(ThreatDetection detection) {
        detections.remove(detection);
        detection.setSecurityEvent(null);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public ThreatType getThreatType() {
        return threatType;
    }

    public void setThreatType(ThreatType threatType) {
        this.threatType = threatType;
    }

    public ThreatSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(ThreatSeverity severity) {
        this.severity = severity;
    }

    public Double getThreatScore() {
        return threatScore;
    }

    public void setThreatScore(Double threatScore) {
        this.threatScore = threatScore;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    public MitigationAction getActionTaken() {
        return actionTaken;
    }

    public void setActionTaken(MitigationAction actionTaken) {
        this.actionTaken = actionTaken;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public ApiRequest getApiRequest() {
        return apiRequest;
    }

    public void setApiRequest(ApiRequest apiRequest) {
        this.apiRequest = apiRequest;
    }

    public List<ThreatDetection> getDetections() {
        return detections;
    }

    public void setDetections(List<ThreatDetection> detections) {
        this.detections = detections;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
