import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Demo del patron Bulkhead (Grupo 5).
 *
 * Simulamos un servidor con un pool COMPARTIDO de 6 hilos (como Tomcat, pero pequeno).
 * - "Pagos" llama a una pasarela externa que esta lenta (tarda 5 s).
 * - "Catalogo" es rapido y no depende de la pasarela.
 *
 * Ejecutar:
 *   java BulkheadDemo.java sin   -> sin bulkhead: pagos se come todos los hilos y el catalogo espera
 *   java BulkheadDemo.java con   -> con bulkhead: pagos solo puede usar 2 hilos y el catalogo sigue vivo
 *
 * Con bulkhead, los pagos rechazados no se pierden: van a una cola de pendientes
 * y se procesan cuando se libera un cupo.
 */
public class BulkheadDemo {

    // Pool compartido del servidor: 6 hilos para TODAS las peticiones
    static final ExecutorService servidor = Executors.newFixedThreadPool(6);

    // EL BULKHEAD: un semaforo que deja entrar maximo 2 llamadas a pagos al mismo tiempo
    static final Semaphore bulkheadPagos = new Semaphore(2);

    // Cola de pagos pendientes: aqui esperan los pagos rechazados hasta que haya cupo
    static final Queue<Integer> pendientes = new ConcurrentLinkedQueue<>();

    static boolean usarBulkhead;
    static long inicio;

    public static void main(String[] args) throws Exception {
        usarBulkhead = args.length > 0 && args[0].equalsIgnoreCase("con");
        inicio = System.currentTimeMillis();

        System.out.println("=== Demo Bulkhead: " + (usarBulkhead ? "CON bulkhead" : "SIN bulkhead") + " ===");

        // 1. Llegan 6 peticiones de pago (la pasarela esta lenta)
        for (int i = 1; i <= 6; i++) {
            int id = i;
            servidor.submit(() -> pagar(id));
        }

        Thread.sleep(200); // un momento despues...

        // 2. Llegan 3 peticiones al catalogo (deberian ser instantaneas)
        for (int i = 1; i <= 3; i++) {
            int id = i;
            servidor.submit(() -> verCatalogo(id));
        }

        servidor.shutdown();
        servidor.awaitTermination(30, TimeUnit.SECONDS);
        System.out.println("=== Fin ===");
    }

    static void pagar(int id) {
        if (!usarBulkhead) {
            llamarPasarela(id);
            return;
        }
        // Con bulkhead: si no hay cupo, no bloqueamos un hilo; el pago va a la cola de pendientes
        if (!bulkheadPagos.tryAcquire()) {
            pendientes.add(id);
            log("Pago " + id + " RECHAZADO por el bulkhead -> fallback: queda en cola de pendientes");
            return;
        }
        procesarConCupo(id);
    }

    // Procesa un pago que ya tiene cupo y, al terminar, atiende los pendientes de la cola
    static void procesarConCupo(int id) {
        int actual = id;
        while (true) {
            try {
                llamarPasarela(actual);
            } finally {
                bulkheadPagos.release(); // liberar el cupo siempre
            }

            // Se libero un cupo: revisamos si hay pagos pendientes en la cola
            Integer siguiente = pendientes.poll();
            if (siguiente == null) {
                return; // no hay pendientes
            }
            if (!bulkheadPagos.tryAcquire()) {
                pendientes.add(siguiente); // otro lo tomara cuando termine
                return;
            }
            log("Pago " + siguiente + " sale de la cola de pendientes y se procesa");
            actual = siguiente;
        }
    }

    static void llamarPasarela(int id) {
        log("Pago " + id + " esperando a la pasarela (lenta)...");
        dormir(5000);
        log("Pago " + id + " terminado");
    }

    static void verCatalogo(int id) {
        log("Catalogo " + id + " respondio OK");
    }

    static void log(String mensaje) {
        double segundos = (System.currentTimeMillis() - inicio) / 1000.0;
        System.out.printf("[%4.1f s] %s%n", segundos, mensaje);
    }

    static void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}