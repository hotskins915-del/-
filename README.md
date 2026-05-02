# 📝 Заметки — Android App

Приложение для заметок. Kotlin + Material 3 + Room DB.

## Возможности

- ✏️ Создание/редактирование заметок (заголовок + текст)
- 📌 Закрепление важных заметок вверху списка
- 🔍 Живой поиск по заголовку и тексту
- 📤 Экспорт всех заметок в JSON-файл
- 📥 Импорт заметок из JSON (с заменой или добавлением)
- 🌙 Тёмная тема Material 3 (автоматически)

---

## 🚀 Получить APK через GitHub Actions (бесплатно, без установки ПО)

### 1. Создать репозиторий на GitHub
Зайди на github.com → «+» → New repository → Public → Create

### 2. Загрузить код
```bash
git init
git add .
git commit -m "Notes app"
git branch -M main
git remote add origin https://github.com/ТВОЙлогин/notes-app.git
git push -u origin main
```

### 3. Подождать сборку (~3–5 минут)
Вкладка Actions → «Build APK» → ждёшь зелёной галочки ✅

### 4. Скачать APK
Кликни на workflow → раздел Artifacts → скачай notes-app-debug → распакуй → внутри app-debug.apk

### 5. Установить на Android
Перекинь APK на телефон → открой → разреши установку из неизвестных источников → готово 🎉

---

## 📦 Перенос заметок между устройствами

**Экспорт:** ⋮ меню → Экспорт → выбери папку (Google Drive, Downloads)
**Импорт:** ⋮ меню → Импорт → выбери файл → «Заменить все» или «Добавить»

---

## Стек
Kotlin · Coroutines · Flow · Room · Material 3 · MVVM · Gson · GitHub Actions
