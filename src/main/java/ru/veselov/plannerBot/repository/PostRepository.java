package ru.veselov.plannerBot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.veselov.plannerBot.model.PostState;
import ru.veselov.plannerBot.model.UserEntity;
import ru.veselov.plannerBot.model.PostEntity;

import java.util.Date;
import java.util.List;

public interface PostRepository extends JpaRepository<PostEntity,Integer> {

    List<PostEntity> findByDateBeforeAndPostState(Date date, PostState postState);
    List<PostEntity> findAllByUser(UserEntity user);
    List<PostEntity> findByUserAndPostStateIn(UserEntity user, List<PostState> postStates);
    List<PostEntity> findByPostState(PostState postState);
    void deleteByPostId(Integer id);
    boolean existsByPostId(Integer id);

    void deleteByUser(UserEntity userEntity);
}
