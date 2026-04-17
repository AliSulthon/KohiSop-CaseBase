package currency;

// 10 JPY = 1 IDR → 1 JPY = 0.1 IDR → kursPerIDR = 0.1
public class JPYCurrency extends Currency {
    @Override public String getNamaMataUang() { return "JPY"; }
    @Override public String getSimbol() { return "¥"; }
    @Override public double getKursPerIDR() { return 0.1; }
}
