package org.dspace.content;

import org.hibernate.annotations.Immutable;

import javax.persistence.*;
import javax.persistence.Entity;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "metadata_language_summary_mv")
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

    @Column(name = "metadata_process_date", columnDefinition = "timestamp")
    @Temporal(TemporalType.TIMESTAMP)
    private Date metadataProcessDate;

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

    public Date getMetadataProcessDate() {
        return metadataProcessDate;
    }

    public void setSmetadataProcessDate(Date metadataProcessDate) {
        this.metadataProcessDate = metadataProcessDate;
    }

}
