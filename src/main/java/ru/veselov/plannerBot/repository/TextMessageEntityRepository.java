package ru.veselov.plannerBot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import ru.veselov.plannerBot.model.content.TextMessageEntity;

@Repository
public interface TextMessageEntityRepository extends JpaRepository<TextMessageEntity,Integer> {

}
