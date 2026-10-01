# Patrón Bulkhead — Grupo 5

Demo del patrón **Bulkhead** para la exposición de patrones de diseño para microservicios.

## Idea

Un servidor tiene un pool **compartido** de 6 hilos. El servicio de **pagos** depende de una pasarela externa lenta (5 s) y el **catálogo** es rápido.

- **Sin bulkhead:** las peticiones de pagos ocupan los 6 hilos y el catálogo tiene que esperar 5 s aunque no tenga nada que ver con el problema (fallo en cascada).
- **Con bulkhead:** un semáforo limita pagos a 2 llamadas simultáneas. Las demás se rechazan al instante con un *fallback* ("pago pendiente") y el catálogo responde de inmediato.

## Cómo ejecutarlo

Requiere Java 17 o superior (no necesita Maven ni librerías).

```bash
java BulkheadDemo.java sin
java BulkheadDemo.java con
```

## En un proyecto real

En Spring Boot se usa la librería **Resilience4j**, que hace lo mismo con configuración (`maxConcurrentCalls`, `maxWaitDuration`) y la anotación `@Bulkhead`. Su bulkhead por semáforo aplica la misma idea de esta demo.

## Integrantes

Nicolás Mosorongo · Juan Pablo Maestre · Santiago Fernández · Luis Yair Valencia · Sebastián Rey Escobar · Kevin Aristizábal Ipia
