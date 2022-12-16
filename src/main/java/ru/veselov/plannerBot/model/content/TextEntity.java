package ru.veselov.plannerBot.model.content;

import lombok.*;
import ru.veselov.plannerBot.model.PostEntity;

import javax.persistence.*;


@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "text")
public class TextEntity {

    public TextEntity(String text){
        this.text = text;
    }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int textId;

    @Column(name = "text", columnDefinition = "varchar(4096)")
    private String text;

    @ManyToOne
    @JoinColumn(name="post_id",referencedColumnName = "post_id")
    private PostEntity post;

}
