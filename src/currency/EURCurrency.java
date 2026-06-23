package currency;

public class EURCurrency extends Currency {
    @Override public String getNamaMataUang() { return "EUR"; }
    @Override public String getSimbol() { return "EUR"; }
    @Override public double getKursPerIDR() { return 14.0; }
}
