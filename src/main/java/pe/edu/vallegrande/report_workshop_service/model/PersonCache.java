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
@NoArgsConstructor
@AllArgsConstructor
@Table("person_cache")
public class PersonCache {
    @Id
    @Column("id_person")
    private Integer idPerson;
    private String name;
    private String surname;
    private Integer age;
    @Column("type_kinship")
    private String typeKinship;
    private String sponsored;
    private String state;
    @Column("family_id_family")
    private Integer familyIdFamily;
}
