package ru.veselov.plannerBot.model.content;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.veselov.plannerBot.model.PostEntity;

import javax.persistence.*;

@Entity
@Getter @Setter
@NoArgsConstructor
@TypeDef(name="jsonb",typeClass = JsonBinaryType.class)
@Table(name = "message_entity")
public class MessageDBEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Type(type = "jsonb")
    @Column(name = "message",columnDefinition = "jsonb")
    private Message message;
    @ManyToOne
    @JoinColumn(name = "post_id",referencedColumnName = "post_id")
    private PostEntity post;
}
