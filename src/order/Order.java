package order;

import menu.MenuItem;
import java.util.LinkedList;
import java.util.List;

public class Order {
    private static final int MAX_JENIS_MINUMAN = 5;
    private static final int MAX_JENIS_MAKANAN = 5;

    private final List<OrderItem> daftarMinuman = new LinkedList<>();
    private final List<OrderItem> daftarMakanan = new LinkedList<>();

    public boolean tambahItem(MenuItem item, int kuantitas) {
        OrderItem oi = new OrderItem(item, kuantitas);
        if (oi.isMinuman()) {
            if (daftarMinuman.size() >= MAX_JENIS_MINUMAN) return false;
            daftarMinuman.add(oi);
        } else {
            if (daftarMakanan.size() >= MAX_JENIS_MAKANAN) return false;
            daftarMakanan.add(oi);
        }
        return true;
    }

    public boolean sudahDipesan(String kode) {
        for (OrderItem oi : daftarMinuman)
            if (oi.getItem().getKode().equalsIgnoreCase(kode)) return true;
        for (OrderItem oi : daftarMakanan)
            if (oi.getItem().getKode().equalsIgnoreCase(kode)) return true;
        return false;
    }

    public boolean minumanPenuh() { return daftarMinuman.size() >= MAX_JENIS_MINUMAN; }
    public boolean makananPenuh() { return daftarMakanan.size() >= MAX_JENIS_MAKANAN; }
    public boolean isEmpty() { return daftarMinuman.isEmpty() && daftarMakanan.isEmpty(); }

    public List<OrderItem> getDaftarMinuman() { return daftarMinuman; }
    public List<OrderItem> getDaftarMakanan() { return daftarMakanan; }

    public List<OrderItem> semuaItem() {
        List<OrderItem> all = new LinkedList<>();
        all.addAll(daftarMinuman);
        all.addAll(daftarMakanan);
        return all;
    }

    public void kosongkan() {
        daftarMinuman.clear();
        daftarMakanan.clear();
    }

    /** Bebaskan pajak semua item (untuk member dengan kode mengandung 'A') */
    public void bebaskanPajak() {
        for (OrderItem oi : daftarMinuman) oi.setPajakDibebaskan(true);
        for (OrderItem oi : daftarMakanan) oi.setPajakDibebaskan(true);
    }

    public double getTotalMinumanSebelumPajak() {
        return daftarMinuman.stream().mapToDouble(OrderItem::getSubtotalSebelumPajak).sum();
    }

    public double getTotalMinumanSetelahPajak() {
        return daftarMinuman.stream().mapToDouble(OrderItem::getSubtotalSetelahPajak).sum();
    }

    public double getTotalMakananSebelumPajak() {
        return daftarMakanan.stream().mapToDouble(OrderItem::getSubtotalSebelumPajak).sum();
    }

    public double getTotalMakananSetelahPajak() {
        return daftarMakanan.stream().mapToDouble(OrderItem::getSubtotalSetelahPajak).sum();
    }

    public double getTotalSebelumPajak() {
        return getTotalMinumanSebelumPajak() + getTotalMakananSebelumPajak();
    }

    public double getTotalSetelahPajak() {
        return getTotalMinumanSetelahPajak() + getTotalMakananSetelahPajak();
    }
}
