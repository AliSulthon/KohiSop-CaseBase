package menu;

public abstract class Food extends MenuItem {
    public Food(String kode, String nama, double harga) {
        super(kode, nama, harga);
    }

    @Override
    public double hitungPajak() {
        if (harga > 50) return 0.08;
        return 0.11;
    }
}
