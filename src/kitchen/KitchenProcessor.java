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
        System.out.print(formatKitchenOrders(semuaOrderItems));
    }

    public static String formatKitchenOrders(List<List<OrderItem>> semuaOrderItems) {
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
        StringBuilder output = new StringBuilder();
        String sep = "=".repeat(50);
        output.append("\n").append(sep).append("\n");
        output.append("          KITCHEN PROCESSING SYSTEM\n");
        output.append(sep).append("\n");

        output.append("\n=== TIM MAKANAN ===\n");
        if (queueMakanan.isEmpty()) {
            output.append("  (tidak ada pesanan makanan)\n");
        } else {
            int no = 1;
            while (!queueMakanan.isEmpty()) {
                OrderItem oi = queueMakanan.poll();
                output.append(String.format("  %d. %-35s (Rp %.0f) x%d%n",
                        no++, oi.getItem().getNama(), oi.getItem().getHarga(), oi.getKuantitas()));
            }
        }

        output.append("\n=== TIM MINUMAN ===\n");
        if (stackMinuman.isEmpty()) {
            output.append("  (tidak ada pesanan minuman)\n");
        } else {
            int no = 1;
            // Pop dari stack = LIFO
            Stack<OrderItem> temp = new Stack<>();
            temp.addAll(stackMinuman); // copy to preserve original
            while (!temp.isEmpty()) {
                OrderItem oi = temp.pop();
                output.append(String.format("  %d. %-35s (Rp %.0f) x%d%n",
                        no++, oi.getItem().getNama(), oi.getItem().getHarga(), oi.getKuantitas()));
            }
        }

        output.append(sep).append("\n");
        return output.toString();
    }
}
