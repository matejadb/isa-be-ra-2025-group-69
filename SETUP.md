# Backend Setup Uputstvo

## Preduslovi

- Java 17+

## Koraci

1. Kloniraj repo:

```bash
git clone git@github.com:USERNAME/isa-be-ra-2025-group-69.git
cd isa-be-ra-2025-group-69
git checkout development
```

2. Instaliraj PostgreSQL i kreiraj bazu:

```sql
CREATE DATABASE jutjubic-v1;
```

3. Konfiguriši `application.properties`:

```
spring.application.name=backend

# PostgreSQL Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/jutjubic-v1
spring.datasource.username=postgres
spring.datasource.password=root
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.format_sql=true

# CORS Configuration
spring.web.cors.allowed-origins=http://localhost:3000,http://localhost:5173
spring.web.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
spring.web.cors.allowed-headers=*
spring.web.cors.allow-credentials=true

# Server
server.port=8080
```

# Server

server.port=8080

4. Pokreni aplikaciju:

```bash
./mvnw spring-boot:run
```

Backend će biti dostupan na: http://localhost:8080

## Workflow

1. Uvek radi na feature branch-evima:

```bash
git checkout develop
git pull origin develop
git checkout -b feature/ime-feature-a
# Radi svoj deo...
git add .
git commit -m "Opis izmene"
git push origin feature/ime-feature-a
```

2. Otvori Pull Request na GitHub-u: develop ← feature/ime-feature-a
