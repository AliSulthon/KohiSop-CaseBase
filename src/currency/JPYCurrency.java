package currency;

public class JPYCurrency extends Currency {
    @Override public String getNamaMataUang() { return "JPY"; }
    @Override public String getSimbol() { return "JPY"; }
    @Override public double getKursPerIDR() { return 0.1; }
}
