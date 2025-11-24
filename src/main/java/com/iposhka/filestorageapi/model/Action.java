package com.iposhka.filestorageapi.model;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Action {
    REGISTER("Зарегистрировался на сайте"),
    UNKNOWN("Пытались войти под: %s"),
    LOGIN("Вошел на сайт"),
    CREATE_DIRECTORY("Создал директорию: %s"),
    UPLOAD_RESOURCE("Загрузил ресурс: %s"),
    DELETE_RESOURCE("Удалил ресурсы: %s"),
    DOWNLOAD_RESOURCE("Загрузил ресурс: %s"),
    RENAME_RESOURCE("Переименовал ресурс: с %s на %s");

    private final String description;

    public static Action fromDescription(String description) {
        return Arrays.stream(Action.values())
                .filter(action -> action.description.equals(description))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown description: " + description));
    }
}