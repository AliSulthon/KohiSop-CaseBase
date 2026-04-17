package currency;

public class MYRCurrency extends Currency {
    @Override public String getNamaMataUang() { return "MYR"; }
    @Override public String getSimbol() { return "RM"; }
    @Override public double getKursPerIDR() { return 4.0; }
}
