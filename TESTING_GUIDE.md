# Guía de Pruebas Unitarias - Proyecto Arka

## 📋 Resumen de Pruebas Creadas

Se han creado **pruebas unitarias completas** para todos los microservicios utilizando:

- **JUnit 5** (Jupiter)
- **Mockito** para mocks
- **Spring Boot Test** (`@WebMvcTest`, `@DataJpaTest`)
- **MockMvc** para pruebas de controladores

---

## 🧪 Estructura de Pruebas por Servicio

### 1. **auth-service** ✅
- `AuthControllerTest.java` - 3 tests (register, login, validación)
- `AuthServiceTest.java` - 4 tests (registro, login, validaciones)
- `UserRepositoryTest.java` - 4 tests (findByUsername, findByEmail, save)

### 2. **cart-service** ✅
- `CartControllerTest.java` - 4 tests (getCart, addItem, removeItem, clearCart)
- `CartServiceTest.java` - 6 tests (crear carrito, agregar items, actualizar cantidades, eliminar)

### 3. **catalog-service** ✅
- `CatalogControllerTest.java` - 4 tests (listar productos, filtrar activos, crear/actualizar)
- `CatalogServiceTest.java` - 5 tests (crear, actualizar, listar, desactivar)

### 4. **category-maintainer** ✅
- `CategoryControllerTest.java` - 5 tests (CRUD completo)
- `CategoryServiceTest.java` - 6 tests (validaciones, actualización, búsqueda)

### 5. **inventory-service** ✅
- `ProductControllerTest.java` - 5 tests (CRUD, actualización de stock, low-stock)
- `ProductServiceTest.java` - 8 tests (crear producto, validaciones, actualizar stock con optimistic locking)

### 6. **review-service** ✅
- `ReviewControllerTest.java` - 4 tests (CRUD, búsqueda por producto/usuario)
- `ReviewServiceTest.java` - 6 tests (validación de rating 1-5, crear, buscar)

### 7. **shipping-service** ✅
- `ShippingControllerTest.java` - 4 tests (listar opciones, filtrar activas)
- `ShippingServiceTest.java` - 4 tests (buscar opciones de envío)

### 8. **provider-service** ✅
- `ProviderControllerTest.java` - 5 tests (CRUD completo)
- `ProviderServiceTest.java` - 6 tests (crear, actualizar, listar proveedores)

### 9. **notification-service** ✅
- `NotificationControllerTest.java` - 2 tests (enviar notificación, payload vacío)

### 10. **order-service** ✅
- `OrderControllerTest.java` - 2 tests (crear orden, modificar orden)
- `OrderServiceTest.java` - 5 tests (crear orden con stock, cancelar, modificar, validaciones)

---

## 🚀 Cómo Ejecutar las Pruebas

### **Opción 1: Ejecutar TODOS los tests del proyecto**

```bash
# Desde la raíz del proyecto
mvn clean test

# Con reporte de cobertura JaCoCo
mvn clean test jacoco:report
```

### **Opción 2: Ejecutar tests de un servicio específico**

```bash
# Ejemplo: solo auth-service
cd auth-service
mvn test

# Ejemplo: solo inventory-service
cd inventory-service
mvn test
```

### **Opción 3: Ejecutar un test específico**

```bash
cd auth-service
mvn test -Dtest=AuthServiceTest
mvn test -Dtest=AuthControllerTest#register_ShouldReturnCreated_WhenValidUser
```

### **Opción 4: Desde IntelliJ IDEA / Eclipse**

1. Click derecho en la clase de test → `Run 'NombreTest'`
2. Para ejecutar todos: Click derecho en carpeta `src/test/java` → `Run All Tests`
3. Ver cobertura: Click derecho → `Run with Coverage`

---

## 📊 Ver Reportes de Cobertura

Después de ejecutar `mvn clean test jacoco:report`:

```bash
# El reporte HTML se genera en:
# <servicio>/target/site/jacoco/index.html

# Por ejemplo:
open auth-service/target/site/jacoco/index.html
open inventory-service/target/site/jacoco/index.html
```

---

## 🔧 Tecnologías Utilizadas

### **JUnit 5** (Jupiter)
```java
@Test
void nombreDelTest() {
  // Assert con mensajes claros
  assertEquals(expected, actual);
  assertTrue(condition);
  assertThrows(Exception.class, () -> method());
}
```

### **Mockito**
```java
@Mock
private MiRepositorio repositorio;

@InjectMocks
private MiServicio servicio;

@Test
void test() {
  when(repositorio.findById(1L)).thenReturn(Optional.of(objeto));
  verify(repositorio, times(1)).save(any());
}
```

### **Spring Boot Test**

#### **Controladores** - `@WebMvcTest`
```java
@WebMvcTest(MiController.class)
class MiControllerTest {
  @Autowired MockMvc mockMvc;
  @MockBean MiServicio servicio;
  
  @Test
  void test() throws Exception {
    mockMvc.perform(get("/api/endpoint"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.campo").value("valor"));
  }
}
```

#### **Servicios** - `@ExtendWith(MockitoExtension.class)`
```java
@ExtendWith(MockitoExtension.class)
class MiServiceTest {
  @Mock private MiRepositorio repo;
  @InjectMocks private MiServicio servicio;
}
```

#### **Repositorios** - `@DataJpaTest`
```java
@DataJpaTest
class MiRepositoryTest {
  @Autowired TestEntityManager entityManager;
  @Autowired MiRepositorio repositorio;
}
```

---

## 📈 Cobertura Objetivo

El proyecto está configurado con **JaCoCo** para garantizar:

- **Cobertura mínima**: 70% de líneas de código
- Si la cobertura cae por debajo, el build falla

```xml
<!-- En cada pom.xml -->
<limit>
  <counter>LINE</counter>
  <value>COVEREDRATIO</value>
  <minimum>0.70</minimum>
</limit>
```

---

## ✅ Gateway - Rutas de Review Service

El servicio de **reviews** ya está configurado en el gateway:

```yaml
# gateway/src/main/resources/application.yml
- id: review
  uri: lb://review-service
  predicates:
    - Path=/api/reviews/**
```

**Endpoints disponibles:**
- `GET /api/reviews/product/{productId}` - Reviews por producto
- `GET /api/reviews/user/{userId}` - Reviews por usuario
- `POST /api/reviews` - Crear review
- `DELETE /api/reviews/{id}` - Eliminar review

---

## 🎯 Resumen de Cobertura por Servicio

| Servicio | Controller Tests | Service Tests | Repository Tests | Total |
|----------|-----------------|---------------|------------------|-------|
| auth-service | 3 | 4 | 4 | **11** |
| cart-service | 4 | 6 | - | **10** |
| catalog-service | 4 | 5 | - | **9** |
| category-maintainer | 5 | 6 | - | **11** |
| inventory-service | 5 | 8 | - | **13** |
| review-service | 4 | 6 | - | **10** |
| shipping-service | 4 | 4 | - | **8** |
| provider-service | 5 | 6 | - | **11** |
| notification-service | 2 | - | - | **2** |
| order-service | 2 | 5 | - | **7** |
| **TOTAL** | **38** | **50** | **4** | **92 tests** ✅

---

## 🔍 Ejemplos de Ejecución

```bash
# Ejecutar todos los tests
mvn clean test

# Ejecutar con perfiles específicos
mvn test -Dspring.profiles.active=test

# Ver solo errores
mvn test --quiet

# Ejecutar en paralelo (más rápido)
mvn test -T 4

# Generar reporte de cobertura
mvn clean verify jacoco:report

# Ver reporte en navegador
open target/site/jacoco/index.html
```

---

## 📌 Notas Importantes

1. **Los tests usan H2 in-memory** para repositorios (no afectan la BD real)
2. **Todos los servicios externos están mockeados** (InventoryClient, NotificationClient, etc.)
3. **JWT no es necesario en tests unitarios** (se mockea la autenticación)
4. **Los tests son independientes** - no dependen del orden de ejecución

---

## 🆘 Solución de Problemas

### Error: "No tests found"
```bash
# Verificar que las clases terminan en *Test.java
# Verificar que tienen @Test en los métodos
```

### Error: "Bean not found"
```bash
# Usar @MockBean en lugar de @Autowired para dependencias en @WebMvcTest
```

### Tests lentos
```bash
# Ejecutar en paralelo
mvn test -T 4

# O configurar en pom.xml:
<parallel>classes</parallel>
<threadCount>4</threadCount>
```

---

## 🎓 Buenas Prácticas Aplicadas

✅ **Given-When-Then** en nombres de tests  
✅ **Arrange-Act-Assert** en estructura  
✅ **Un solo assert lógico por test**  
✅ **Tests independientes y repetibles**  
✅ **Mocks solo para dependencias externas**  
✅ **Cobertura de casos happy path y edge cases**  
✅ **Nomenclatura descriptiva** (`shouldDoSomething_WhenCondition`)

---

¡Las pruebas están listas para ejecutarse! 🚀
