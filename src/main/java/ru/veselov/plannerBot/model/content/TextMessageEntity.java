package ru.veselov.plannerBot.model.content;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import ru.veselov.plannerBot.model.PostEntity;

import javax.persistence.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "text_entity")
@TypeDef(name="jsonb", typeClass = JsonBinaryType.class)
public class TextMessageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    @Type(type = "jsonb")
    @Column(name="entity",columnDefinition = "jsonb")
    private MessageEntity entity;

    @ManyToOne
    @JoinColumn(name = "textId",referencedColumnName = "id")
    private TextEntity text;

}
