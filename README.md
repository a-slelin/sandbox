# 🧪 Sandbox

**Песочница для разнородных проектов и экспериментов**

> Место, где живут небольшие проекты, пробы новых технологий, заготовки и эксперименты, которым пока (или вообще) не
> нужен собственный репозиторий.

![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk)
![Maven](https://img.shields.io/badge/Maven-multi--module-red?logo=apachemaven)
![Status](https://img.shields.io/badge/status-in%20development-yellow)

---

## 📖 Содержание

- [О проекте](#-о-проекте)
- [Структура проекта](#-структура-проекта)
- [Корневой pom.xml](#-корневой-pomxml)
- [Как добавить новый модуль](#-как-добавить-новый-модуль)
- [Требования](#-требования)
- [Быстрый старт](#-быстрый-старт)
- [Статус](#-статус)
- [Разработчики](#-разработчики)

---

## 🎯 О проекте

**Sandbox** — модульный **Maven**-проект: каждый эксперимент оформляется как отдельный модуль (или группа модулей), а
все модули наследуются от корневого `sandbox`. Благодаря этому версии Java, плагинов и общих библиотек задаются в одном
месте, а каждый модуль описывает только то, что нужно именно ему.

## 📁 Структура проекта

```text
sandbox/                        a.slelin.work:sandbox              (pom, корень)
├── pom.xml
├── <Group>/                    a.slelin.work.sandbox:<group>      (pom, группа модулей)
│   ├── pom.xml
│   └── <Project>/              a.slelin.work.sandbox.<group>:<project>
│       ├── pom.xml
│       └── src/
└── <Project>/                  a.slelin.work.sandbox:<project>    (самостоятельный модуль)
    ├── pom.xml
    └── src/
```

Список модулей:

* **[MailClient](MailClient/README.md)** 📬: настольный почтовый клиент на **Java Swing** + **Jakarta Mail** —
  отправка писем по **SMTP** и просмотр последних входящих по **IMAP**.

* **[Voting](Voting/README.md)** 🗳️: клиент-серверное голосование — REST-сервер на **Spring Boot** и настольный
  клиент на **Spring Boot + Swing (FlatLaf)** с анимированными результатами в реальном времени. Группа из трёх
  подмодулей: `Common` (общие DTO), `Server`, `Client`.

## 🧾 Корневой pom.xml

Корневой `pom.xml` имеет `packaging = pom` и ничего не собирает сам — он задаёт общие правила для всех модулей:

| Блок                      | Что в нём                                                                                                |
|---------------------------|----------------------------------------------------------------------------------------------------------|
| `groupId` / `artifactId`  | `a.slelin.work:sandbox` — корень всей иерархии.                                                          |
| `version`                 | `${revision}` — единая версия для всех модулей, задаётся свойством `revision` в одном месте.             |
| `name` / `description`    | Человекочитаемое имя и описание проекта.                                                                 |
| `url` / `developers`      | Ссылка на репозиторий и информация о разработчике.                                                       |
| `repositories`            | Явно указанный Maven Central.                                                                            |
| `modules`                 | Список дочерних модулей.                                                                                 |
| `properties` → General    | Кодировка `UTF-8`, `java.version = 25`, флаг `skipTests`.                                                |
| `properties` → Plugins    | Версии плагинов `clean`, `compiler`, `surefire`, `jar`, `install`, `flatten`.                            |
| `properties` → Libraries  | Версии общих библиотек: **Lombok**, **Logback**, **JUnit**, **Mockito**.                                 |
| `lastCheckVersion`        | Дата последней проверки актуальности версий.                                                             |
| `dependencyManagement`    | Общие библиотеки с версиями и `scope`. Сами по себе **не подключаются** — модуль объявляет нужные без версии. Импорт `junit-bom` выравнивает версии всех артефактов JUnit (в том числе пришедших транзитивно, например из Spring Boot). |
| `pluginManagement`        | Настройки плагинов: компиляция под Java 25 (`release`), Lombok как annotation processor, флаг `-parameters`. |
| `plugins` → flatten       | `flatten-maven-plugin` подставляет значение `${revision}` в устанавливаемые pom. Без него модуль, зависящий от другого модуля песочницы, не соберётся отдельно (`-pl`) — Maven не найдёт родителя с версией `${revision}`. |
| `finalName`               | Имя собираемого артефакта.                                                                               |

Специфичные для эксперимента библиотеки (Spring, БД, MapStruct и т.д.) объявляются в `pom.xml` самого модуля —
корень остаётся лёгким.

## ➕ Как добавить новый модуль

**1)** Создать папку модуля, например `Experiment`, и в ней `pom.xml`:

```xml
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>a.slelin.work</groupId>
        <artifactId>sandbox</artifactId>
        <version>${revision}</version>
    </parent>

    <groupId>a.slelin.work.sandbox</groupId>
    <artifactId>experiment</artifactId>

    <packaging>jar</packaging>

    <name>Sandbox::Experiment</name>

    <description>Short description of the experiment</description>

    <dependencies>

        <!--Lombok-->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>

    </dependencies>

    <build>
        <finalName>Experiment</finalName>
    </build>

</project>
```

**2)** Зарегистрировать модуль в корневом `pom.xml`:

```xml
<modules>
    <module>Experiment</module>
</modules>
```

**3)** Добавить модуль в список в разделе [Структура проекта](#-структура-проекта).

Соглашения:

* папка модуля — `PascalCase`, `artifactId` — `lowercase`;
* `groupId` модуля — `groupId` родителя + `.` + `artifactId` родителя;
* `name` — путь от корня через `::` (например, `Sandbox::Group::Project`);
* `finalName` — имя папки модуля.

## 📌 Требования

**1)** Java JDK **25**.

**2)** Apache Maven **3.9.9**.

## 🚀 Быстрый старт

```shell
# Клонируем репозиторий;
git clone https://github.com/a-slelin/sandbox.git;

# Переходим в корневую папку;
cd sandbox;

# Собираем проект с помощью maven;
mvn clean install;
```

## 📍 Статус

В репозиторий постоянно добавляются новые проекты и эксперименты!

## 👨‍💻 Разработчики

Слелин А. В. 🤗 (**a-slelin**)
