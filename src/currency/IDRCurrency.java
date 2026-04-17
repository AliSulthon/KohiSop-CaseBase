package currency;

public class IDRCurrency extends Currency {
    @Override public String getNamaMataUang() { return "IDR"; }
    @Override public String getSimbol() { return "Rp"; }
    @Override public double getKursPerIDR() { return 1.0; }
}
