package ru.veselov.plannerBot.model.content;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.veselov.plannerBot.model.PostEntity;

import javax.persistence.*;
@Entity
@Table(name = "photo")
@Getter
@Setter
@NoArgsConstructor

public class PhotoEntity {
    @Id
    private String photo_id;
    @Column(name="size")
    private int size;
    @Column(name = "caption", columnDefinition = "varchar(1024)")
    private String caption;

    @ManyToOne
    @JoinColumn(name="post_id",referencedColumnName = "post_id")
    private PostEntity post;

    public PhotoEntity(String fileId, Integer fileSize) {
        this.photo_id=fileId;
        this.size=fileSize;
    }
}
