package ru.veselov.plannerBot.bots;

public enum BotState {
    READY_TO_WORK,
    AWAITING_POST,
    AWAITING_DATE,
    BOT_WAITING_FOR_ADDING_TO_CHANNEL,
    MANAGE,
    VIEW,
    PROMOTE_USER,
    READY_TO_SAVE
}
