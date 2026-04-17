package menu;

public abstract class MenuItem {
    protected String kode;
    protected String nama;
    protected double harga;

    public MenuItem(String kode, String nama, double harga) {
        this.kode = kode;
        this.nama = nama;
        this.harga = harga;
    }

    public abstract double hitungPajak();

    public String getKode() { return kode; }
    public String getNama() { return nama; }
    public double getHarga() { return harga; }
}
