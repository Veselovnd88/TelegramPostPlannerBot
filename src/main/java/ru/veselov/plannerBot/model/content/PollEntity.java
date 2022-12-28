package ru.veselov.plannerBot.model.content;

import lombok.*;
import ru.veselov.plannerBot.model.PostEntity;

import javax.persistence.*;
import java.util.LinkedList;
import java.util.List;

@Entity(name="poll")
@Getter
@Setter
@NoArgsConstructor
public class PollEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "poll_id")
    private int pollId;

    @Column(name = "question")
    private String question;

    @Column(name = "multiple_answer")
    private boolean multipleAnswer;

    @ElementCollection
    @CollectionTable(name = "poll_options",joinColumns = @JoinColumn (name="pollId"))
    @Column(name = "pollOptions")
    private List<String> pollOptions = new LinkedList<>();

    @ElementCollection
    @CollectionTable(name = "expl_texts", joinColumns = @JoinColumn(name = "pollId"))
    @Column(name = "explTexts")
    private List<String> explTexts = new LinkedList<>();

    @Column(name = "type")
    private String type;

    @Column(name="anon")
    private boolean isAnonymous;
    @Column(name = "explanation")
    private String explanation;
    @Column(name = "correct_id")
    private Integer correctOptionId;
    @ManyToOne
    @JoinColumn(name="post_id",referencedColumnName = "post_id")
    private PostEntity post;


    public PollEntity(String question) {
        this.question = question;
    }
}
