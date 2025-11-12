# Dependencias Maven para Swagger/OpenAPI y Testing

## Para TODOS los microservicios, agrega estas dependencias en cada `pom.xml`:

```xml
<!-- Springdoc OpenAPI (Swagger) -->
<dependency>
  <groupId>org.springdoc</groupId>
  <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
  <version>2.2.0</version>
</dependency>

<!-- JUnit 5 (ya incluido en spring-boot-starter-test) -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-test</artifactId>
  <scope>test</scope>
</dependency>

<!-- Mockito (ya incluido en spring-boot-starter-test) -->
<!-- Security Test (solo para auth-service) -->
<dependency>
  <groupId>org.springframework.security</groupId>
  <artifactId>spring-security-test</artifactId>
  <scope>test</scope>
</dependency>
```

## Plugin JaCoCo para cobertura de código:

```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.jacoco</groupId>
      <artifactId>jacoco-maven-plugin</artifactId>
      <version>0.8.11</version>
      <executions>
        <execution>
          <goals>
            <goal>prepare-agent</goal>
          </goals>
        </execution>
        <execution>
          <id>report</id>
          <phase>test</phase>
          <goals>
            <goal>report</goal>
          </goals>
        </execution>
        <execution>
          <id>jacoco-check</id>
          <goals>
            <goal>check</goal>
          </goals>
          <configuration>
            <rules>
              <rule>
                <element>PACKAGE</element>
                <limits>
                  <limit>
                    <counter>LINE</counter>
                    <value>COVEREDRATIO</value>
                    <minimum>0.70</minimum>
                  </limit>
                </limits>
              </rule>
            </rules>
          </configuration>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

## Comandos para ejecutar:

```bash
# Ejecutar tests
mvn clean test

# Generar reporte de cobertura
mvn clean test jacoco:report

# Ver reporte HTML en: target/site/jacoco/index.html

# Ejecutar tests de un solo servicio
cd auth-service && mvn test

# Ejecutar tests con perfil específico
mvn test -Dspring.profiles.active=test
```

## Acceso a Swagger UI:

- **Servicios individuales**: `http://localhost:{PORT}/swagger-ui.html`
- **Gateway agregador**: `http://localhost:8080/swagger-ui.html`

Ejemplos:
- Auth Service: http://localhost:8091/swagger-ui.html
- Cart Service: http://localhost:8092/swagger-ui.html
- Gateway (todos): http://localhost:8080/swagger-ui.html

## Notas importantes:

1. Swagger solo estará activo con perfiles `dev` o `test` (no en `prod`)
2. Los reportes JaCoCo se generan en `target/site/jacoco/`
3. La configuración ya incluye autenticación JWT en Swagger
4. Para CI/CD, los tests se ejecutan automáticamente antes de cada deployment
