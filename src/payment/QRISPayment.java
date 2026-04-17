package payment;

public class QRISPayment extends PaymentChannel {
    private double saldo;

    public QRISPayment(double saldo) { this.saldo = saldo; }

    public boolean saldoCukup(double total) { return saldo >= total; }

    @Override public String getNamaChannel() { return "QRIS"; }
    @Override public double getDiskon() { return 0.05; }
    @Override public double getBiayaAdmin() { return 0.0; }
}
