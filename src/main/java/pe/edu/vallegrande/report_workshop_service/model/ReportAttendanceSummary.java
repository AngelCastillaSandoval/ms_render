package pe.edu.vallegrande.report_workshop_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table("report_attendance_summary")
public class ReportAttendanceSummary {
    @Id
    private Integer id;
    @Column("report_workshop_id")
    private Integer reportWorkshopId;
    @Column("person_id")
    private Integer personId;
    @Column("person_name")
    private String personName;
    @Column("present_count")
    private Integer presentCount;
    @Column("absent_count")
    private Integer absentCount;
    @Column("late_count")
    private Integer lateCount;
    @Column("justified_count")
    private Integer justifiedCount;
}
