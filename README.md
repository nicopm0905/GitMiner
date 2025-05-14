# GitMiner
# GitMiner API

GitMiner es una API REST desarrollada en Java con Spring Boot que permite extraer, estructurar y consultar información de proyectos de control de versiones, como GitHub y Bitbucket. Forma parte del proyecto de integración de AISS 2025.

## 📚 Descripción

El microservicio **GitMiner** actúa como intermediario entre APIs públicas de plataformas de repositorios (como GitHub) y una base de datos local, permitiendo almacenar, consultar y filtrar información de:

- Proyectos (repositories)
- Commits
- Issues
- Usuarios
- Comentarios

Toda la información es normalizada para poder ser utilizada por otros servicios del ecosistema.

## 🛠️ Tecnologías

- Java 21
- Spring Boot 3.4.5
- Spring Data JPA
- H2 (base de datos embebida)
- Swagger / OpenAPI (documentación)
- Maven

## 🚀 Ejecución

Para levantar el servicio:

```bash
mvn spring-boot:run
