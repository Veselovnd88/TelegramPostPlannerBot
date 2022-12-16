package ru.veselov.plannerBot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.veselov.plannerBot.model.UserEntity;
import ru.veselov.plannerBot.model.content.ChatEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<ChatEntity,String> {

    List<ChatEntity> findAllByUsers(UserEntity user);

    Optional<ChatEntity> findByChatId(String chatId);
}
