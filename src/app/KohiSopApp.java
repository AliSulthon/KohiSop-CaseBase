package app;

import currency.*;
import menu.MenuItem;
import order.Order;
import order.OrderItem;
import payment.*;

import java.util.List;
import java.util.Scanner;

public class KohiSopApp {

    private static final Scanner sc = new Scanner(System.in);
    private static final List<MenuItem> menuMinuman = MenuData.getMenuMinuman();
    private static final List<MenuItem> menuMakanan = MenuData.getMenuMakanan();

    // =====================================================================
    // MAIN
    // =====================================================================
    public static void main(String[] args) {
        cetakHeader("Selamat Datang di KohiSop");

        tampilkanMenu();

        Order order = inputPesanan();
        if (order == null) {
            System.out.println("\nPesanan dibatalkan. Terima kasih!");
            return;
        }

        inputKuantitas(order);
        if (order.isEmpty()) {
            System.out.println("\nTidak ada pesanan. Program selesai.");
            return;
        }

        Currency currency = pilihMataUang();
        PaymentChannel payment = pilihPembayaran(order.getTotalSetelahPajak(), currency);
        if (payment == null) {
            System.out.println("\nPembayaran gagal. Program selesai.");
            return;
        }

        cetakKuitansi(order, payment, currency);
    }

    // =====================================================================
    // 1. TAMPILKAN MENU
    // =====================================================================
    private static void tampilkanMenu() {
        System.out.println("\n" + "=".repeat(60));
        System.out.printf("%-6s %-34s %s%n", "Kode", "Menu Minuman", "Harga (Rp)");
        System.out.println("-".repeat(60));
        for (MenuItem m : menuMinuman)
            System.out.printf("%-6s %-34s %d%n", m.getKode(), m.getNama(), (int) m.getHarga());

        System.out.println("\n" + "-".repeat(60));
        System.out.printf("%-6s %-34s %s%n", "Kode", "Menu Makanan", "Harga (Rp)");
        System.out.println("-".repeat(60));
        for (MenuItem m : menuMakanan)
            System.out.printf("%-6s %-34s %d%n", m.getKode(), m.getNama(), (int) m.getHarga());
        System.out.println("=".repeat(60));
    }

    // =====================================================================
    // 2. INPUT PESANAN (kode menu)
    // =====================================================================
    private static Order inputPesanan() {
        Order order = new Order();
        System.out.println("\nMasukkan kode menu (ketik 'FF' jika selesai memesan, 'CC' untuk batal):");

        while (true) {
            if (order.minumanPenuh() && order.makananPenuh()) {
                System.out.println("Pesanan sudah penuh (max 5 jenis minuman & 5 jenis makanan).");
                break;
            }

            System.out.print("Kode > ");
            String input = sc.nextLine().trim();

            if (input.equalsIgnoreCase("CC")) return null;
            if (input.equalsIgnoreCase("FF")) {
                if (order.isEmpty()) {
                    System.out.println("Belum ada pesanan! Masukkan minimal satu menu.");
                    continue;
                }
                break;
            }

            MenuItem item = MenuData.cariByKode(input, menuMinuman, menuMakanan);
            if (item == null) {
                System.out.println("Kode tidak valid. Coba lagi.");
                continue;
            }
            if (order.sudahDipesan(item.getKode())) {
                System.out.println("Item sudah ada dalam pesanan.");
                continue;
            }

            boolean berhasil = order.tambahItem(item, 1);
            if (!berhasil) {
                System.out.println("Batas jenis " +
                        (isMinumanKode(item.getKode()) ? "minuman" : "makanan") + " sudah penuh (max 5).");
            } else {
                System.out.println("\"" + item.getNama() + "\" ditambahkan.");
            }
        }
        return order;
    }

    private static boolean isMinumanKode(String kode) {
        for (MenuItem m : menuMinuman)
            if (m.getKode().equalsIgnoreCase(kode)) return true;
        return false;
    }

    // =====================================================================
    // 3. INPUT KUANTITAS
    // =====================================================================
    private static void inputKuantitas(Order order) {
        System.out.println("\nMasukkan jumlah untuk setiap pesanan.");
        System.out.println("(Tekan Enter = 1 porsi, '0' atau 'S' = batalkan item, 'CC' = batalkan semua)");

        // --- Minuman ---
        List<OrderItem> toRemoveMin = new java.util.ArrayList<>();
        for (OrderItem oi : order.getDaftarMinuman()) {
            tampilkanDaftarPesanan(order);
            System.out.printf("Jumlah [%s] %s (max 3 porsi): ", oi.getItem().getKode(), oi.getItem().getNama());

            Integer qty = bacaKuantitas(3);
            if (qty == null) { order.getDaftarMinuman().clear(); order.getDaftarMakanan().clear(); return; }
            if (qty == 0) { toRemoveMin.add(oi); System.out.println("Item dibatalkan."); }
            else oi.setKuantitas(qty);
        }
        order.getDaftarMinuman().removeAll(toRemoveMin);

        // --- Makanan ---
        List<OrderItem> toRemoveMak = new java.util.ArrayList<>();
        for (OrderItem oi : order.getDaftarMakanan()) {
            tampilkanDaftarPesanan(order);
            System.out.printf("Jumlah [%s] %s (max 2 porsi): ", oi.getItem().getKode(), oi.getItem().getNama());

            Integer qty = bacaKuantitas(2);
            if (qty == null) { order.getDaftarMinuman().clear(); order.getDaftarMakanan().clear(); return; }
            if (qty == 0) { toRemoveMak.add(oi); System.out.println("Item dibatalkan."); }
            else oi.setKuantitas(qty);
        }
        order.getDaftarMakanan().removeAll(toRemoveMak);
    }

    /**
     * Baca kuantitas dari input. Kembalikan null jika CC, 0 jika skip/0.
     */
    private static Integer bacaKuantitas(int max) {
        while (true) {
            String raw = sc.nextLine().trim();
            if (raw.equalsIgnoreCase("CC")) return null;
            if (raw.equalsIgnoreCase("S") || raw.equals("0")) return 0;
            if (raw.isEmpty()) return 1;
            try {
                int n = Integer.parseInt(raw);
                if (n < 1 || n > max) {
                    System.out.printf("Input tidak valid. Masukkan 1 - %d (atau S untuk batalkan): ", max);
                } else {
                    return n;
                }
            } catch (NumberFormatException e) {
                System.out.printf("Input tidak valid. Masukkan angka 1 - %d: ", max);
            }
        }
    }

    private static void tampilkanDaftarPesanan(Order order) {
        System.out.println("\n--- Daftar Pesanan Saat Ini ---");
        if (!order.getDaftarMinuman().isEmpty()) {
            System.out.printf("%-6s %-34s %s%n", "Kode", "Minuman", "Qty");
            System.out.println("-".repeat(48));
            for (OrderItem oi : order.getDaftarMinuman())
                System.out.printf("%-6s %-34s %d%n", oi.getItem().getKode(), oi.getItem().getNama(), oi.getKuantitas());
        }
        if (!order.getDaftarMakanan().isEmpty()) {
            System.out.printf("%-6s %-34s %s%n", "Kode", "Makanan", "Qty");
            System.out.println("-".repeat(48));
            for (OrderItem oi : order.getDaftarMakanan())
                System.out.printf("%-6s %-34s %d%n", oi.getItem().getKode(), oi.getItem().getNama(), oi.getKuantitas());
        }
        System.out.println();
    }

    // =====================================================================
    // 4. PILIH MATA UANG
    // =====================================================================
    private static Currency pilihMataUang() {
        System.out.println("\nPilih mata uang pembayaran:");
        System.out.println("1. IDR (Rupiah)");
        System.out.println("2. USD (1 USD = 15 IDR)");
        System.out.println("3. JPY (10 JPY = 1 IDR)");
        System.out.println("4. MYR (1 MYR = 4 IDR)");
        System.out.println("5. EUR (1 EUR = 14 IDR)");

        while (true) {
            System.out.print("Pilihan (1-5) > ");
            String inp = sc.nextLine().trim();
            switch (inp) {
                case "1": return new IDRCurrency();
                case "2": return new USDCurrency();
                case "3": return new JPYCurrency();
                case "4": return new MYRCurrency();
                case "5": return new EURCurrency();
                default: System.out.println("Pilihan tidak valid. Coba lagi.");
            }
        }
    }

    // =====================================================================
    // 5. PILIH CHANNEL PEMBAYARAN
    // =====================================================================
    private static PaymentChannel pilihPembayaran(double totalIDR, Currency currency) {
        System.out.println("\nPilih channel pembayaran:");
        System.out.println("1. Tunai (tidak ada diskon)");
        System.out.println("2. QRIS  (diskon 5%)");
        System.out.println("3. eMoney (diskon 7%, admin Rp 20)");

        while (true) {
            System.out.print("Pilihan (1-3) > ");
            String inp = sc.nextLine().trim();
            switch (inp) {
                case "1":
                    return new CashPayment();
                case "2": {
                    double saldo = bacaSaldo("QRIS");
                    QRISPayment qris = new QRISPayment(saldo);
                    double totalFinal = qris.hitungTotal(totalIDR);
                    if (!qris.saldoCukup(totalFinal)) {
                        System.out.printf("Saldo QRIS tidak cukup. Total tagihan: Rp %.2f%n", totalFinal);
                        System.out.println("Pilih channel lain.");
                        continue;
                    }
                    return qris;
                }
                case "3": {
                    double saldo = bacaSaldo("eMoney");
                    EMoneyPayment emoney = new EMoneyPayment(saldo);
                    double totalFinal = emoney.hitungTotal(totalIDR);
                    if (!emoney.saldoCukup(totalFinal)) {
                        System.out.printf("Saldo eMoney tidak cukup. Total tagihan: Rp %.2f%n", totalFinal);
                        System.out.println("Pilih channel lain.");
                        continue;
                    }
                    return emoney;
                }
                default:
                    System.out.println("Pilihan tidak valid. Coba lagi.");
            }
        }
    }

    private static double bacaSaldo(String namaChannel) {
        while (true) {
            System.out.printf("Masukkan saldo %s (IDR) > ", namaChannel);
            try {
                double s = Double.parseDouble(sc.nextLine().trim());
                if (s < 0) { System.out.println("Saldo tidak boleh negatif."); continue; }
                return s;
            } catch (NumberFormatException e) {
                System.out.println("Input tidak valid. Masukkan angka.");
            }
        }
    }

    // =====================================================================
    // 6. CETAK KUITANSI
    // =====================================================================
    private static void cetakKuitansi(Order order, PaymentChannel payment, Currency currency) {
        String sep  = "=".repeat(64);
        String line = "-".repeat(64);

        System.out.println("\n" + sep);
        cetakTengah("KUITANSI KohiSop", 64);
        System.out.println(sep);

        // --- Minuman ---
        if (!order.getDaftarMinuman().isEmpty()) {
            System.out.printf("%-6s %-26s %5s %8s %8s%n", "Kode", "Minuman", "Qty", "Harga", "Pajak");
            System.out.println(line);
            for (OrderItem oi : order.getDaftarMinuman()) {
                System.out.printf("%-6s %-26s %5d %8.2f %7.0f%%%n",
                        oi.getItem().getKode(),
                        oi.getItem().getNama(),
                        oi.getKuantitas(),
                        oi.getItem().getHarga(),
                        oi.getPajakRate() * 100);
                System.out.printf("%-6s %-26s %5s %8.2f + pajak %7.2f = %8.2f%n",
                        "", "", "",
                        oi.getSubtotalSebelumPajak(),
                        oi.getTotalPajak(),
                        oi.getSubtotalSetelahPajak());
            }
            System.out.println(line);
            System.out.printf("%-38s %8.2f%n", "Total Minuman (sebelum pajak):", order.getTotalMinumanSebelumPajak());
            System.out.printf("%-38s %8.2f%n", "Total Minuman (setelah pajak):",  order.getTotalMinumanSetelahPajak());
        }

        // --- Makanan ---
        if (!order.getDaftarMakanan().isEmpty()) {
            System.out.println();
            System.out.printf("%-6s %-26s %5s %8s %8s%n", "Kode", "Makanan", "Qty", "Harga", "Pajak");
            System.out.println(line);
            for (OrderItem oi : order.getDaftarMakanan()) {
                System.out.printf("%-6s %-26s %5d %8.2f %7.0f%%%n",
                        oi.getItem().getKode(),
                        oi.getItem().getNama(),
                        oi.getKuantitas(),
                        oi.getItem().getHarga(),
                        oi.getPajakRate() * 100);
                System.out.printf("%-6s %-26s %5s %8.2f + pajak %7.2f = %8.2f%n",
                        "", "", "",
                        oi.getSubtotalSebelumPajak(),
                        oi.getTotalPajak(),
                        oi.getSubtotalSetelahPajak());
            }
            System.out.println(line);
            System.out.printf("%-38s %8.2f%n", "Total Makanan (sebelum pajak):", order.getTotalMakananSebelumPajak());
            System.out.printf("%-38s %8.2f%n", "Total Makanan (setelah pajak):",  order.getTotalMakananSetelahPajak());
        }

        // --- Ringkasan Pembayaran ---
        double totalSebelumPajak = order.getTotalSebelumPajak();
        double totalSetelahPajak = order.getTotalSetelahPajak();
        double diskon            = payment.hitungDiskon(totalSetelahPajak);
        double totalFinal        = payment.hitungTotal(totalSetelahPajak);

        // Konversi ke mata uang terpilih
        double totalSebelumPajakMU = currency.konversiDariIDR(totalSebelumPajak);
        double totalFinalMU        = currency.konversiDariIDR(totalFinal);

        System.out.println(sep);
        System.out.printf("%-38s %8.2f IDR%n", "Grand Total (sebelum pajak):", totalSebelumPajak);
        System.out.printf("%-38s %8.2f IDR%n", "Grand Total (setelah pajak):", totalSetelahPajak);
        System.out.printf("%-38s %8.2f IDR%n", "Diskon " + payment.getNamaChannel() + " (" + (int)(payment.getDiskon()*100) + "%):", diskon);
        if (payment.getBiayaAdmin() > 0)
            System.out.printf("%-38s %8.2f IDR%n", "Biaya Admin:", payment.getBiayaAdmin());
        System.out.println(line);
        System.out.printf("%-38s %8.2f IDR%n", "Total Tagihan (IDR):", totalFinal);
        System.out.println();
        System.out.printf("Mata Uang Pembayaran  : %s%n", currency.getNamaMataUang());
        System.out.printf("Total Sebelum Pajak   : %s %.4f%n", currency.getSimbol(), totalSebelumPajakMU);
        System.out.printf("Total Tagihan Akhir   : %s %.4f%n", currency.getSimbol(), totalFinalMU);
        System.out.println(sep);
        cetakTengah("Terima kasih dan silakan datang kembali!", 64);
        System.out.println(sep);
    }

    // =====================================================================
    // HELPER
    // =====================================================================
    private static void cetakHeader(String judul) {
        System.out.println("=".repeat(64));
        cetakTengah(judul, 64);
        System.out.println("=".repeat(64));
    }

    private static void cetakTengah(String teks, int lebar) {
        int pad = (lebar - teks.length()) / 2;
        System.out.println(" ".repeat(Math.max(0, pad)) + teks);
    }
}
