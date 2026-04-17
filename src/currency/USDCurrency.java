package currency;

public class USDCurrency extends Currency {
    @Override public String getNamaMataUang() { return "USD"; }
    @Override public String getSimbol() { return "$"; }
    @Override public double getKursPerIDR() { return 15.0; }
}
