package app;

import currency.*;
import kitchen.KitchenProcessor;
import member.Member;
import member.MemberDatabase;
import menu.MenuItem;
import order.Order;
import order.OrderItem;
import payment.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

public class KohiSopApp {

    private static final Scanner sc = new Scanner(System.in);
    private static final List<MenuItem> menuMinuman = MenuData.getMenuMinumanUrutKode();
    private static final List<MenuItem> menuMakanan = MenuData.getMenuMakananUrutKode();
    private static final Comparator<OrderItem> ORDER_ITEM_BY_HARGA =
            Comparator.comparingDouble((OrderItem oi) -> oi.getItem().getHarga())
                    .thenComparing(oi -> oi.getItem().getKode(), String.CASE_INSENSITIVE_ORDER);

    // Kitchen batch tracker
    private static int customerCount = 0;
    private static final List<List<OrderItem>> kitchenBatch = new ArrayList<>();

    // =====================================================================
    // MAIN
    // =====================================================================
    public static void main(String[] args) {
        boolean lanjut = true;
        while (lanjut) {
            cetakHeader("Selamat Datang di KohiSop");

            tampilkanMenu();

            Order order = inputPesanan();
            if (order == null) {
                System.out.println("\nPesanan dibatalkan. Terima kasih!");
            } else {
                inputKuantitas(order);
                if (order.isEmpty()) {
                    System.out.println("\nTidak ada pesanan. Transaksi dibatalkan.");
                } else {
                    Currency currency = pilihMataUang();

                    // --- Membership flow ---
                    Member member = handleMembership();

                    // --- Payment ---
                    double totalSetelahPajak = order.getTotalSetelahPajak();

                    // Tawarkan redeem poin jika IDR & punya poin
                    double potonganPoin = 0;
                    int poinSebelum = 0;
                    if (member != null) {
                        poinSebelum = member.getPoints();
                        if (currency.getNamaMataUang().equals("IDR") && member.getPoints() > 0) {
                            potonganPoin = tawarRedeemPoin(member, totalSetelahPajak);
                        }
                    }
                    double totalSetelahRedeem = totalSetelahPajak - potonganPoin;

                    PaymentChannel payment = pilihPembayaran(totalSetelahRedeem, currency);
                    if (payment == null) {
                        System.out.println("\nPembayaran gagal. Transaksi dibatalkan.");
                    } else {
                        // --- Hitung & tambah poin ---
                        int poinDapat = 0;
                        int poinSesudah = 0;
                        if (member != null) {
                            poinDapat = hitungPoin(totalSetelahPajak, member.getMemberCode());
                            member.addPoints(poinDapat);
                            poinSesudah = member.getPoints();
                        }

                        cetakKuitansi(order, payment, currency, member,
                                poinSebelum, poinDapat, poinSesudah, potonganPoin);

                        // --- Kitchen tracking ---
                        List<OrderItem> allItems = new ArrayList<>();
                        allItems.addAll(order.getDaftarMakanan());
                        allItems.addAll(order.getDaftarMinuman());
                        kitchenBatch.add(allItems);
                        customerCount++;

                        if (customerCount == 3) {
                            KitchenProcessor.processKitchenOrders(kitchenBatch);
                            kitchenBatch.clear();
                            customerCount = 0;
                        }
                    }
                }
            }

            System.out.print("\nTransaksi lain? (Y/N) > ");
            String jawab = sc.nextLine().trim();
            lanjut = jawab.equalsIgnoreCase("Y");
        }

        System.out.println("\nTerima kasih telah menggunakan KohiSop!");
    }

    // =====================================================================
    // 1. TAMPILKAN MENU
    // =====================================================================
    private static void tampilkanMenu() {
        System.out.println("\n" + "=".repeat(60));
        cetakKelompokMenu("Menu Makanan", menuMakanan);
        System.out.println("\n" + "-".repeat(60));
        cetakKelompokMenu("Menu Minuman", menuMinuman);
        System.out.println("=".repeat(60));
    }

    private static void cetakKelompokMenu(String judul, List<MenuItem> items) {
        System.out.printf("%-6s %-34s %s%n", "Kode", judul, "Harga (Rp)");
        System.out.println("-".repeat(60));
        for (MenuItem m : items)
            System.out.printf("%-6s %-34s %d%n", m.getKode(), m.getNama(), (int) m.getHarga());
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
            String input = MenuData.normalisasiKode(sc.nextLine());

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

        if (!inputKuantitasUntuk(order, order.getDaftarMakanan(), "makanan", 2)) {
            order.kosongkan();
            return;
        }
        if (!inputKuantitasUntuk(order, order.getDaftarMinuman(), "minuman", 3)) {
            order.kosongkan();
        }
    }

    private static boolean inputKuantitasUntuk(Order order, List<OrderItem> daftar, String kategori, int max) {
        List<OrderItem> toRemove = new ArrayList<>();
        for (OrderItem oi : urutkanOrderItemsByHarga(daftar)) {
            tampilkanDaftarPesanan(order);
            System.out.printf("Jumlah [%s] %s (max %d porsi %s): ",
                    oi.getItem().getKode(), oi.getItem().getNama(), max, kategori);

            Integer qty = bacaKuantitas(max);
            if (qty == null) return false;
            if (qty == 0) {
                toRemove.add(oi);
                System.out.println("Item dibatalkan.");
            } else {
                oi.setKuantitas(qty);
            }
        }
        daftar.removeAll(toRemove);
        return true;
    }

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
        if (!order.getDaftarMakanan().isEmpty()) {
            cetakDaftarPesananKategori("Makanan", order.getDaftarMakanan());
        }
        if (!order.getDaftarMinuman().isEmpty()) {
            cetakDaftarPesananKategori("Minuman", order.getDaftarMinuman());
        }
        System.out.println();
    }

    private static void cetakDaftarPesananKategori(String kategori, List<OrderItem> items) {
        System.out.printf("%-6s %-34s %8s %5s%n", "Kode", kategori, "Harga", "Qty");
        System.out.println("-".repeat(58));
        for (OrderItem oi : urutkanOrderItemsByHarga(items))
            System.out.printf("%-6s %-34s %8.2f %5d%n",
                    oi.getItem().getKode(),
                    oi.getItem().getNama(),
                    oi.getItem().getHarga(),
                    oi.getKuantitas());
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
    // 5. MEMBERSHIP
    // =====================================================================

    /**
     * Tanya nama pelanggan. Jika sudah ada di DB -> pakai data lama.
     * Jika baru -> buat member baru dengan kode random.
     */
    private static Member handleMembership() {
        System.out.print("\nMasukkan nama Anda > ");
        String nama = sc.nextLine().trim();
        if (nama.isEmpty()) {
            System.out.println("Nama kosong. Transaksi tanpa membership.");
            return null;
        }

        Member member = MemberDatabase.findMemberByName(nama);
        if (member != null) {
            System.out.println("Selamat datang kembali, " + member.getMemberName() + "!");
            System.out.println("Kode Member : " + member.getMemberCode());
            System.out.println("Poin Saat Ini: " + member.getPoints());
        } else {
            String kode = MemberDatabase.generateMemberCode();
            member = new Member(kode, nama, 0);
            MemberDatabase.addMember(member);
            System.out.println("Member baru terdaftar!");
            System.out.println("Nama        : " + member.getMemberName());
            System.out.println("Kode Member : " + member.getMemberCode());
        }
        return member;
    }

    // =====================================================================
    // 6. POINT SYSTEM
    // =====================================================================

    /**
     * Hitung poin dari total belanja (sebelum redeem).
     * 1 poin per 10 IDR. Jika kode mengandung 'A', poin digandakan.
     */
    private static int hitungPoin(double totalBelanja, String memberCode) {
        int poin = (int)(totalBelanja / 10);
        if (memberCode.contains("A")) {
            poin *= 2;
        }
        return poin;
    }

    /**
     * Tawarkan redeem poin kepada member.
     * Hanya bisa jika bayar IDR.
     * Return potongan IDR yang digunakan (0 jika tidak redeem).
     */
    private static double tawarRedeemPoin(Member member, double tagihan) {
        System.out.println("\n--- Redeem Poin ---");
        System.out.println("Poin Anda     : " + member.getPoints());
        System.out.printf("Nilai Poin    : Rp %.0f (@ 1 poin = Rp 2)%n", member.getPoints() * 2.0);
        System.out.printf("Total Tagihan : Rp %.2f%n", tagihan);
        System.out.print("Gunakan poin untuk potongan? (Y/N) > ");
        String jawab = sc.nextLine().trim();
        if (!jawab.equalsIgnoreCase("Y")) return 0;

        double potongan = member.usePoints(tagihan);
        System.out.printf("Poin digunakan. Potongan: Rp %.0f%n", potongan);
        System.out.println("Sisa Poin    : " + member.getPoints());
        return potongan;
    }

    // =====================================================================
    // 7. PILIH CHANNEL PEMBAYARAN
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
    // 8. CETAK KUITANSI
    // =====================================================================
    private static void cetakKuitansi(Order order, PaymentChannel payment, Currency currency,
                                      Member member, int poinSebelum, int poinDapat,
                                      int poinSesudah, double potonganPoin) {
        String sep  = "=".repeat(100);
        String line = "-".repeat(100);

        System.out.println("\n" + sep);
        cetakTengah("KUITANSI KohiSop", 100);
        System.out.println(sep);

        cetakBagianKuitansi("Makanan", order.getDaftarMakanan(),
                order.getTotalMakananSebelumPajak(), order.getTotalMakananSetelahPajak(), line);
        cetakBagianKuitansi("Minuman", order.getDaftarMinuman(),
                order.getTotalMinumanSebelumPajak(), order.getTotalMinumanSetelahPajak(), line);

        double totalSebelumPajak = order.getTotalSebelumPajak();
        double totalSetelahPajak = order.getTotalSetelahPajak();
        double totalSetelahRedeem = totalSetelahPajak - potonganPoin;
        double diskon             = payment.hitungDiskon(totalSetelahRedeem);
        double totalFinal         = payment.hitungTotal(totalSetelahRedeem);

        double totalSebelumPajakMU = currency.konversiDariIDR(totalSebelumPajak);
        double totalFinalMU        = currency.konversiDariIDR(totalFinal);

        System.out.println(sep);
        System.out.printf("%-48s %12.2f IDR%n", "Grand Total (sebelum pajak):", totalSebelumPajak);
        System.out.printf("%-48s %12.2f IDR%n", "Grand Total (setelah pajak):", totalSetelahPajak);

        if (potonganPoin > 0) {
            System.out.printf("%-48s %12.2f IDR%n", "Potongan Poin Member:", -potonganPoin);
            System.out.printf("%-48s %12.2f IDR%n", "Total setelah Potongan Poin:", totalSetelahRedeem);
        }

        System.out.printf("%-48s %12.2f IDR%n",
                "Diskon " + payment.getNamaChannel() + " (" + (int)(payment.getDiskon()*100) + "%):", diskon);
        if (payment.getBiayaAdmin() > 0)
            System.out.printf("%-48s %12.2f IDR%n", "Biaya Admin:", payment.getBiayaAdmin());
        System.out.println(line);
        System.out.printf("%-48s %12.2f IDR%n", "Total Tagihan (IDR):", totalFinal);

        // --- Informasi Member & Poin ---
        if (member != null) {
            System.out.println(line);
            System.out.printf("%-48s %s%n", "Member:", member.getMemberName());
            System.out.printf("%-48s %s%n", "Kode Member:", member.getMemberCode());
            System.out.printf("%-48s %d poin%n", "Point Sebelum Transaksi:", poinSebelum);
            System.out.printf("%-48s %d poin%n", "Point Didapat:", poinDapat
                    + (member.getMemberCode().contains("A") ? " (bonus x2, kode mengandung 'A')" : ""));
            System.out.printf("%-48s %d poin%n", "Point Sesudah:", poinSesudah);
        }

        System.out.println(line);
        System.out.println();
        System.out.printf("Mata Uang Pembayaran           : %s%n", currency.getNamaMataUang());
        System.out.printf("Total sebelum pajak (%s)       : %s %.4f%n",
                currency.getNamaMataUang(), currency.getSimbol(), totalSebelumPajakMU);
        System.out.printf("Total tagihan akhir (%s)       : %s %.4f%n",
                currency.getNamaMataUang(), currency.getSimbol(), totalFinalMU);

        // Catatan jika mata uang bukan IDR dan punya poin
        if (member != null && !currency.getNamaMataUang().equals("IDR") && member.getPoints() > 0) {
            System.out.println("\n[INFO] Poin tidak dapat ditukar karena pembayaran bukan IDR.");
        }

        System.out.println(sep);
        cetakTengah("Terima kasih dan silakan datang kembali!", 100);
        System.out.println(sep);

        // Info pelanggan ke-N untuk kitchen
        System.out.printf("\n[Kitchen] Pelanggan ke-%d dalam batch. ", customerCount + 1);
        if (customerCount + 1 == 3) {
            System.out.println("Batch berikutnya akan memproses dapur!");
        } else {
            System.out.printf("Dapur diproses setiap 3 pelanggan.%n");
        }
    }

    private static void cetakBagianKuitansi(String kategori, List<OrderItem> items,
                                            double totalSebelumPajak,
                                            double totalSetelahPajak,
                                            String line) {
        if (items.isEmpty()) return;

        System.out.printf("%n%s%n", kategori.toUpperCase());
        System.out.printf("%-6s %-38s %5s %9s %10s %14s %10s%n",
                "Kode", "Nama", "Qty", "Harga", "Subtotal", "Pajak", "Total");
        System.out.println(line);
        for (OrderItem oi : urutkanOrderItemsByHarga(items)) {
            String pajak = String.format("%.2f (%.0f%%)", oi.getTotalPajak(), oi.getPajakRate() * 100);
            System.out.printf("%-6s %-38s %5d %9.2f %10.2f %14s %10.2f%n",
                    oi.getItem().getKode(),
                    oi.getItem().getNama(),
                    oi.getKuantitas(),
                    oi.getItem().getHarga(),
                    oi.getSubtotalSebelumPajak(),
                    pajak,
                    oi.getSubtotalSetelahPajak());
        }
        System.out.println(line);
        System.out.printf("%-63s %10.2f IDR%n", "Total " + kategori + " (sebelum pajak):", totalSebelumPajak);
        System.out.printf("%-63s %10.2f IDR%n", "Total " + kategori + " (setelah pajak):", totalSetelahPajak);
    }

    private static List<OrderItem> urutkanOrderItemsByHarga(List<OrderItem> items) {
        List<OrderItem> sorted = new ArrayList<>(items);
        sorted.sort(ORDER_ITEM_BY_HARGA);
        return sorted;
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
