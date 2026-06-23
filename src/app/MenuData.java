package app;

import menu.MenuItem;
import menu.DrinkItem;
import menu.FoodItem;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MenuData {
    private static final Comparator<MenuItem> SORT_BY_KODE =
            Comparator.comparing(MenuItem::getKode, String.CASE_INSENSITIVE_ORDER);

    public static List<MenuItem> getMenuMinuman() {
        List<MenuItem> list = new ArrayList<>();
        list.add(new DrinkItem("A1", "Caffe Latte", 46));
        list.add(new DrinkItem("A2", "Cappuccino", 46));
        list.add(new DrinkItem("E1", "Caffe Americano", 37));
        list.add(new DrinkItem("E2", "Caffe Mocha", 55));
        list.add(new DrinkItem("E3", "Caramel Macchiato", 59));
        list.add(new DrinkItem("E4", "Asian Dolce Latte", 55));
        list.add(new DrinkItem("E5", "Double Shots Iced Shaken Espresso", 50));
        list.add(new DrinkItem("B1", "Freshly Brewed Coffee", 23));
        list.add(new DrinkItem("B2", "Vanilla Sweet Cream Cold Brew", 50));
        list.add(new DrinkItem("B3", "Cold Brew", 44));
        return list;
    }

    public static List<MenuItem> getMenuMakanan() {
        List<MenuItem> list = new ArrayList<>();
        list.add(new FoodItem("M1", "Petemania Pizza", 112));
        list.add(new FoodItem("M2", "Mie Rebus Super Mario", 35));
        list.add(new FoodItem("M3", "Ayam Bakar Goreng Rebus Spesial", 72));
        list.add(new FoodItem("M4", "Soto Kambing Iga Guling", 124));
        list.add(new FoodItem("S1", "Singkong Bakar A La Carte", 37));
        list.add(new FoodItem("S2", "Ubi Cilembu Bakar Arang", 58));
        list.add(new FoodItem("S3", "Tempe Mendoan", 18));
        list.add(new FoodItem("S4", "Tahu Bakso Extra Telur", 28));
        return list;
    }

    public static List<MenuItem> getMenuMinumanUrutKode() {
        return urutkanByKode(getMenuMinuman());
    }

    public static List<MenuItem> getMenuMakananUrutKode() {
        return urutkanByKode(getMenuMakanan());
    }

    public static String normalisasiKode(String kode) {
        if (kode == null) return "";
        return kode.replaceAll("\\s+", "").toUpperCase();
    }

    public static MenuItem cariByKode(String kode, List<MenuItem> minuman, List<MenuItem> makanan) {
        String target = normalisasiKode(kode);
        for (MenuItem m : minuman)
            if (m.getKode().equalsIgnoreCase(target)) return m;
        for (MenuItem m : makanan)
            if (m.getKode().equalsIgnoreCase(target)) return m;
        return null;
    }

    private static List<MenuItem> urutkanByKode(List<MenuItem> menu) {
        List<MenuItem> sorted = new ArrayList<>(menu);
        sorted.sort(SORT_BY_KODE);
        return sorted;
    }
}
