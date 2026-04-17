package menu;

public abstract class Drink extends MenuItem {
    public Drink(String kode, String nama, double harga) {
        super(kode, nama, harga);
    }

    @Override
    public double hitungPajak() {
        if (harga < 50) return 0.0;
        if (harga <= 55) return 0.08;
        return 0.11;
    }
}
