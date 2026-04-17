package payment;

public class CashPayment extends PaymentChannel {
    @Override public String getNamaChannel() { return "Tunai"; }
    @Override public double getDiskon() { return 0.0; }
    @Override public double getBiayaAdmin() { return 0.0; }
}
