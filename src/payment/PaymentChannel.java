package payment;

public abstract class PaymentChannel {
    public abstract String getNamaChannel();
    public abstract double getDiskon();
    public abstract double getBiayaAdmin();

    public double hitungTotal(double subtotalSetelahPajak) {
        double diskon = subtotalSetelahPajak * getDiskon();
        return subtotalSetelahPajak - diskon + getBiayaAdmin();
    }

    public double hitungDiskon(double subtotalSetelahPajak) {
        return subtotalSetelahPajak * getDiskon();
    }
}
