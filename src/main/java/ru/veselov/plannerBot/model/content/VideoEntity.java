package ru.veselov.plannerBot.model.content;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.veselov.plannerBot.model.PostEntity;

import javax.persistence.*;

@Entity
@Table(name = "video")
@Getter
@Setter
@NoArgsConstructor
public class VideoEntity {

    @Id
    private String videoId;

    @Column(name = "video_name")
    private String name;


    @Column(name = "caption", columnDefinition = "varchar(1024)")
    private String caption;

    @ManyToOne
    @JoinColumn(name="post_id",referencedColumnName = "post_id")
    private PostEntity post;

    public VideoEntity(String fileId, String fileName) {
        this.name=fileName;
        this.videoId = fileId;
    }
}
