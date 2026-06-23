package order;

import menu.MenuItem;
import menu.Drink;

public class OrderItem {
    private MenuItem item;
    private int kuantitas;
    private boolean pajakDibebaskan = false;

    public OrderItem(MenuItem item, int kuantitas) {
        this.item = item;
        this.kuantitas = kuantitas;
    }

    public MenuItem getItem() { return item; }
    public int getKuantitas() { return kuantitas; }
    public void setKuantitas(int kuantitas) { this.kuantitas = kuantitas; }
    public void setPajakDibebaskan(boolean val) { this.pajakDibebaskan = val; }

    public boolean isMinuman() { return item instanceof Drink; }

    public double getSubtotalSebelumPajak() {
        return item.getHarga() * kuantitas;
    }

    public double getPajakRate() {
        if (pajakDibebaskan) return 0.0;
        return item.hitungPajak();
    }

    public double getTotalPajak() {
        return getSubtotalSebelumPajak() * getPajakRate();
    }

    public double getSubtotalSetelahPajak() {
        return getSubtotalSebelumPajak() + getTotalPajak();
    }
}
