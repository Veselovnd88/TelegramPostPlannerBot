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
@Table(name = "audio")
public class AudioEntity {

    @Id
    private String audioId;

    @Column (name = "audio_name")
    private String name;
    @Column(name = "caption", columnDefinition = "varchar(1024)")
    private String caption;

    @ManyToOne
    @JoinColumn(name="post_id",referencedColumnName = "post_id")
    private PostEntity post;

    public AudioEntity(String fileId, String fileName) {
        this.name=fileName;
        this.audioId = fileId;
    }
}
