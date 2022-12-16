package ru.veselov.plannerBot.model.content;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;
import ru.veselov.plannerBot.model.PostEntity;
import ru.veselov.plannerBot.model.UserEntity;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "chat")
public class ChatEntity {

    @Id
    @Column(name = "chat_id")
    private String chatId;

    @Column(name="title")
    private String title;

    @ManyToMany(mappedBy = "chats",
            cascade = {CascadeType.PERSIST,CascadeType.MERGE,CascadeType.REFRESH})
    Set<UserEntity> users=new HashSet<>();
    public void addUser(UserEntity user){
        this.users.add(user);
        user.getChats().add(this);
    }
    public void removeUser(UserEntity user){
        this.users.remove(user);
        user.getChats().remove(this);
    }


    //Двусторонние отношения для ManyToMany
    @ManyToMany(mappedBy = "chats",
    cascade = {CascadeType.MERGE,CascadeType.REFRESH})
    private Set<PostEntity> posts = new HashSet<>();
    public void addPost(PostEntity post){
        this.posts.add(post);
        post.getChats().add(this);
    }
    public void removePost(PostEntity post){
        this.posts.remove(post);
        post.getChats().remove(this);
    }




    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        ChatEntity that = (ChatEntity) o;
        return chatId != null && Objects.equals(chatId, that.chatId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
