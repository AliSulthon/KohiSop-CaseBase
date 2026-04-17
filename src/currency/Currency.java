package currency;

public abstract class Currency {
    public abstract String getNamaMataUang();
    public abstract String getSimbol();
    public abstract double getKursPerIDR();

    public double konversiDariIDR(double idr) {
        return idr / getKursPerIDR();
    }
}
