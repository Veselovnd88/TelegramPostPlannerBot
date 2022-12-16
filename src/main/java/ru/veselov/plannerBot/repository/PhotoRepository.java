package ru.veselov.plannerBot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.veselov.plannerBot.model.content.PhotoEntity;

@Repository
public interface PhotoRepository extends JpaRepository<PhotoEntity,String> {
}
