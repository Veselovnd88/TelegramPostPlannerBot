package ru.veselov.plannerBot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.veselov.plannerBot.model.content.TextEntity;
@Repository
public interface TextRepository extends JpaRepository<TextEntity,Integer> {
}
