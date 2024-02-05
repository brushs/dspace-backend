package org.dspace.content;

import org.hibernate.annotations.Immutable;

import javax.persistence.*;
import javax.persistence.Entity;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "metadata_language_summary_v")
@Immutable
public class MetadataLanguageSummary implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "item_id", nullable = false)
    private UUID id;

    @Column(name = "last_modified", columnDefinition = "timestamp with time zone")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastModified;

    @Column(name = "type_count", nullable = false)
    private long typeCount;

    @Column(name = "type_en_count", nullable = false)
    private long typeEnCount;

    @Column(name = "subject_count", nullable = false)
    private long subjectCount;

    @Column(name = "subject_en_count", nullable = false)
    private long subjectEnCount;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public long getTypeCount() {
        return typeCount;
    }

    public void setTypeCount(long typeCount) {
        this.typeCount = typeCount;
    }

    public long getTypeEnCount() {
        return typeEnCount;
    }

    public void setTypeEnCount(long typeEnCount) {
        this.typeEnCount = typeEnCount;
    }

    public long getSubjectCount() {
        return subjectCount;
    }

    public void setSubjectCount(long subjectCount) {
        this.subjectCount = subjectCount;
    }

    public long getSubjectEnCount() {
        return subjectEnCount;
    }

    public void setSubjectEnCount(long subjectEnCount) {
        this.subjectEnCount = subjectEnCount;
    }
}
