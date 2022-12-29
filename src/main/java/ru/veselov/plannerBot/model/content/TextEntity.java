package ru.veselov.plannerBot.model.content;

import lombok.*;
import ru.veselov.plannerBot.model.PostEntity;

import javax.persistence.*;
import java.util.LinkedList;
import java.util.List;


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
    @Column(name = "id")
    private int textId;

    @Column(name = "text", columnDefinition = "varchar(4096)")
    private String text;
    @OneToMany(mappedBy = "text",cascade = CascadeType.ALL,
            orphanRemoval = true,fetch = FetchType.EAGER)
    private List<TextMessageEntity> entities=new LinkedList<>();
    @ManyToOne
    @JoinColumn(name="post_id",referencedColumnName = "post_id")
    private PostEntity post;

    public void setEntities(List<TextMessageEntity> list){
        this.entities=list;
        for(var e: list){
            e.setText(this);
        }
    }


}
