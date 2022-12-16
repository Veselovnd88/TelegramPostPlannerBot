package ru.veselov.plannerBot.model.content;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.veselov.plannerBot.model.PostEntity;

import javax.persistence.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "doc")
public class DocEntity {

    @Id
    private String docId;

    @Column(name = "doc_name")
    private String docName;

    @Column(name = "caption", columnDefinition = "varchar(1024)")
    private String caption;


    @ManyToOne
    @JoinColumn(name="post_id",referencedColumnName = "post_id")
    private PostEntity post;

    public DocEntity(String id, String name){
        this.docId = id;
        this.docName = name;
    }
}
