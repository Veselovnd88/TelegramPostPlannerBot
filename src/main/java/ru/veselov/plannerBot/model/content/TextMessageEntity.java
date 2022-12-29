package ru.veselov.plannerBot.model.content;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@TypeDef(name="jsonb", typeClass = JsonBinaryType.class)
public class TextMessageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    @Type(type = "jsonb")
    @Column(name="entity",columnDefinition = "jsonb")
    private TextMessageEntity entity;
}
