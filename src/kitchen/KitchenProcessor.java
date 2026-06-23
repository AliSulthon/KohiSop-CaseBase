package kitchen;

import order.OrderItem;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Stack;

public class KitchenProcessor {

    /**
     * Proses dan tampilkan output dapur dari satu batch pesanan.
     * Tim Makanan: PriorityQueue (harga tertinggi duluan).
     * Tim Minuman: Stack (last ordered, first served).
     *
     * @param semuaOrderItems list OrderItem dari semua pelanggan dalam batch ini
     */
    public static void processKitchenOrders(List<List<OrderItem>> semuaOrderItems) {
        // --- Tim Makanan: PriorityQueue harga descending ---
        PriorityQueue<OrderItem> queueMakanan = new PriorityQueue<>(
                Comparator.comparingDouble((OrderItem oi) -> oi.getItem().getHarga()).reversed()
                        .thenComparing(oi -> oi.getItem().getKode(), String.CASE_INSENSITIVE_ORDER)
        );

        // --- Tim Minuman: Stack (LIFO) ---
        Stack<OrderItem> stackMinuman = new Stack<>();

        // Masukkan semua item dari setiap pelanggan dalam batch
        for (List<OrderItem> items : semuaOrderItems) {
            for (OrderItem oi : items) {
                if (oi.isMinuman()) {
                    stackMinuman.push(oi);
                } else {
                    queueMakanan.offer(oi);
                }
            }
        }

        // ---- Cetak output dapur ----
        String sep = "=".repeat(50);
        System.out.println("\n" + sep);
        System.out.println("          KITCHEN PROCESSING SYSTEM");
        System.out.println(sep);

        System.out.println("\n=== TIM MAKANAN ===");
        if (queueMakanan.isEmpty()) {
            System.out.println("  (tidak ada pesanan makanan)");
        } else {
            int no = 1;
            while (!queueMakanan.isEmpty()) {
                OrderItem oi = queueMakanan.poll();
                System.out.printf("  %d. %-35s (Rp %.0f) x%d%n",
                        no++, oi.getItem().getNama(), oi.getItem().getHarga(), oi.getKuantitas());
            }
        }

        System.out.println("\n=== TIM MINUMAN ===");
        if (stackMinuman.isEmpty()) {
            System.out.println("  (tidak ada pesanan minuman)");
        } else {
            int no = 1;
            // Pop dari stack = LIFO
            Stack<OrderItem> temp = new Stack<>();
            temp.addAll(stackMinuman); // copy to preserve original
            while (!temp.isEmpty()) {
                OrderItem oi = temp.pop();
                System.out.printf("  %d. %-35s (Rp %.0f) x%d%n",
                        no++, oi.getItem().getNama(), oi.getItem().getHarga(), oi.getKuantitas());
            }
        }

        System.out.println(sep);
    }
}
