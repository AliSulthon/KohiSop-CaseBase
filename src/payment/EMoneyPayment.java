package payment;

public class EMoneyPayment extends PaymentChannel {
    private double saldo;

    public EMoneyPayment(double saldo) { this.saldo = saldo; }

    public boolean saldoCukup(double total) { return saldo >= total; }

    @Override public String getNamaChannel() { return "eMoney"; }
    @Override public double getDiskon() { return 0.07; }
    @Override public double getBiayaAdmin() { return 20.0; }
}
