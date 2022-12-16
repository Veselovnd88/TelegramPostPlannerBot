# Бот планировщик постов для Телеграма.
________
Бот предназначен для размещения постов в каналы. Пользователь может написать пост и прикрепить к нему
изображения, аудио, видео, документ, опрос, затем ввести назначенное время для публикации
поста в канал, куда бот поставлен в качестве администратора.
_______________
Технологии:
- Java 17.0.5
- Maven 3.8.6
- SpringBoot 2.7.5
- Spring Data JPA
- Postgresql 14.6
- TelegramApi для Java: https://github.com/rubenlagus/TelegramApi
__________
Бот реализован на вебхуках, для работы требуется назначить переменные среды:
- имя бота
- токен
- логин Postgresql
- пароль Postgresql
Сам контент в базу данных не сохраняется, помещаются только file_id телеграма.
Бот задеплоен на VPS сервере с Linux, SSL сертификат подгружается сервером apache2,
с переадресацией запроса в наше приложение. 
Рабочая версия бота: @WordsNBooksBot
