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
@Table("issue_cache")
public class IssueCache {
    @Id
    private Integer id;
    private String name;
    @Column("workshop_id")
    private Integer workshopId;
    private String sesion;
    @Column("scheduled_time")
    private LocalDateTime scheduledTime;
    private String state;
}
