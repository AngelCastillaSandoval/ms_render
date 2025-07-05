package pe.edu.vallegrande.report_workshop_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("attendance_cache")
public class AttendanceCache {
    @Id
    private Integer id;
    @Column("issue_id")
    private Integer issueId;
    @Column("person_id")
    private Integer personId;
    @Column("entry_time")
    private LocalDateTime entryTime;
    private String record;
    @Column("justification_document")
    private String justificationDocument;
    private String state;
}
