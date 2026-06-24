package app;

import currency.Currency;
import currency.EURCurrency;
import currency.IDRCurrency;
import currency.JPYCurrency;
import currency.MYRCurrency;
import currency.USDCurrency;
import kitchen.KitchenProcessor;
import member.Member;
import member.MemberDatabase;
import member.MembershipResult;
import menu.Drink;
import menu.MenuItem;
import order.Order;
import order.OrderItem;
import payment.CashPayment;
import payment.EMoneyPayment;
import payment.PaymentChannel;
import payment.QRISPayment;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class KohiSopGui extends JFrame {
    private static final Color RED = new Color(176, 42, 36);
    private static final Color DARK_RED = new Color(111, 28, 24);
    private static final Color YELLOW = new Color(250, 197, 61);
    private static final Color SOFT_YELLOW = new Color(255, 248, 229);
    private static final Color SURFACE = new Color(255, 253, 247);
    private static final Color SURFACE_ALT = new Color(255, 243, 207);
    private static final Color BORDER = new Color(232, 196, 112);
    private static final Color TEXT = new Color(39, 29, 24);
    private static final Color MUTED_TEXT = new Color(103, 72, 59);
    private static final Font FONT = new Font("Segoe UI Variable", Font.PLAIN, 14);
    private static final Font TITLE_FONT = new Font("Segoe UI Variable", Font.BOLD, 24);
    private static final Font SECTION_FONT = new Font("Segoe UI Variable", Font.BOLD, 15);

    private final List<MenuItem> menuMakanan = MenuData.getMenuMakananUrutKode();
    private final List<MenuItem> menuMinuman = MenuData.getMenuMinumanUrutKode();
    private final Comparator<OrderItem> byHarga = Comparator
            .comparingDouble((OrderItem oi) -> oi.getItem().getHarga())
            .thenComparing(oi -> oi.getItem().getKode(), String.CASE_INSENSITIVE_ORDER);

    private Order currentOrder = new Order();
    private int customerCount = 0;
    private final List<List<OrderItem>> kitchenBatch = new ArrayList<>();

    private JTable makananTable;
    private JTable minumanTable;
    private JTable orderTable;
    private DefaultTableModel orderModel;
    private JTabbedPane menuTabs;
    private JSpinner addQtySpinner;
    private JSpinner editQtySpinner;
    private JLabel totalLabel;
    private JLabel memberHintLabel;
    private JLabel kitchenStatusLabel;
    private JTextField nameField;
    private JTextField saldoField;
    private JComboBox<String> currencyCombo;
    private JComboBox<String> paymentCombo;
    private JCheckBox redeemCheck;
    private JTextArea receiptArea;
    private JTextArea kitchenArea;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            installReadableDefaults();
            new KohiSopGui().setVisible(true);
        });
    }

    private static void installReadableDefaults() {
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Button.foreground", TEXT);
        UIManager.put("CheckBox.foreground", TEXT);
        UIManager.put("ComboBox.foreground", TEXT);
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("Table.foreground", TEXT);
        UIManager.put("TableHeader.foreground", DARK_RED);
        UIManager.put("TabbedPane.foreground", DARK_RED);
        UIManager.put("Panel.background", SOFT_YELLOW);
    }

    public KohiSopGui() {
        super("KohiSop II");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1120, 720));
        setLocationByPlatform(true);
        setContentPane(buildContent());
        refreshOrderTable();
        updatePaymentState();
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(SOFT_YELLOW);
        root.add(buildHeader(), BorderLayout.NORTH);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildMenuPanel(), buildWorkPanel());
        mainSplit.setResizeWeight(0.42);
        mainSplit.setBorder(null);
        root.add(mainSplit, BorderLayout.CENTER);
        return root;
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(YELLOW);
        header.setBorder(new EmptyBorder(18, 24, 18, 24));

        JLabel title = new JLabel("KohiSop II");
        title.setForeground(DARK_RED);
        title.setFont(TITLE_FONT);
        header.add(title, BorderLayout.WEST);

        JLabel subtitle = new JLabel("Kasir, membership, pembayaran, dan proses dapur");
        subtitle.setForeground(MUTED_TEXT);
        subtitle.setFont(FONT);
        header.add(subtitle, BorderLayout.EAST);
        return header;
    }

    private JPanel buildMenuPanel() {
        JPanel panel = sectionPanel("Menu");
        panel.setLayout(new BorderLayout(0, 12));

        makananTable = createMenuTable(menuMakanan);
        minumanTable = createMenuTable(menuMinuman);

        menuTabs = new JTabbedPane();
        menuTabs.setFont(SECTION_FONT);
        styleTabbedPane(menuTabs);
        menuTabs.addTab("Makanan", new JScrollPane(makananTable));
        menuTabs.addTab("Minuman", new JScrollPane(minumanTable));
        menuTabs.addChangeListener(e -> updateAddQtyLimit());
        panel.add(menuTabs, BorderLayout.CENTER);

        JPanel controls = new JPanel(new GridBagLayout());
        controls.setOpaque(false);
        GridBagConstraints c = baseConstraints();
        addQtySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 2, 1));
        JButton addButton = primaryButton("Tambah");
        addButton.addActionListener(e -> addSelectedMenuItem());

        c.gridx = 0;
        c.weightx = 0;
        controls.add(new JLabel("Qty"), c);
        c.gridx = 1;
        controls.add(addQtySpinner, c);
        c.gridx = 2;
        c.weightx = 1;
        controls.add(addButton, c);
        panel.add(controls, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildWorkPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(SOFT_YELLOW);
        panel.setBorder(new EmptyBorder(14, 14, 14, 14));

        JSplitPane vertical = new JSplitPane(JSplitPane.VERTICAL_SPLIT, buildOrderAndPaymentPanel(), buildOutputPanel());
        vertical.setResizeWeight(0.46);
        vertical.setBorder(null);
        panel.add(vertical, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildOrderAndPaymentPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setOpaque(false);
        panel.add(buildOrderPanel(), BorderLayout.CENTER);
        panel.add(buildCheckoutPanel(), BorderLayout.EAST);
        return panel;
    }

    private JPanel buildOrderPanel() {
        JPanel panel = sectionPanel("Pesanan");
        panel.setLayout(new BorderLayout(0, 10));

        orderModel = new DefaultTableModel(new Object[]{"Kode", "Nama", "Kategori", "Harga", "Qty", "Subtotal"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        orderTable = styledTable(orderModel);
        panel.add(new JScrollPane(orderTable), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(10, 0));
        bottom.setOpaque(false);
        totalLabel = new JLabel();
        totalLabel.setFont(SECTION_FONT);
        totalLabel.setForeground(DARK_RED);
        bottom.add(totalLabel, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new GridBagLayout());
        buttons.setOpaque(false);
        GridBagConstraints c = baseConstraints();
        editQtySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 3, 1));
        JButton updateButton = secondaryButton("Ubah Qty");
        JButton removeButton = secondaryButton("Hapus");
        JButton clearButton = dangerButton("Kosongkan");
        orderTable.getSelectionModel().addListSelectionListener(e -> updateEditQtyLimit());
        updateButton.addActionListener(e -> updateSelectedOrderQty());
        removeButton.addActionListener(e -> removeSelectedOrderItem());
        clearButton.addActionListener(e -> clearOrder());

        c.gridx = 0;
        buttons.add(editQtySpinner, c);
        c.gridx = 1;
        buttons.add(updateButton, c);
        c.gridx = 2;
        buttons.add(removeButton, c);
        c.gridx = 3;
        buttons.add(clearButton, c);
        bottom.add(buttons, BorderLayout.EAST);

        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildCheckoutPanel() {
        JPanel panel = sectionPanel("Checkout");
        panel.setPreferredSize(new Dimension(310, 10));
        panel.setLayout(new GridBagLayout());
        GridBagConstraints c = baseConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        nameField = new JTextField();
        currencyCombo = new JComboBox<>(new String[]{"IDR", "USD", "JPY", "MYR", "EUR"});
        paymentCombo = new JComboBox<>(new String[]{"Tunai", "QRIS", "eMoney"});
        saldoField = new JTextField();
        styleTextField(nameField);
        styleTextField(saldoField);
        styleComboBox(currencyCombo);
        styleComboBox(paymentCombo);
        redeemCheck = new JCheckBox("Gunakan poin member");
        redeemCheck.setOpaque(false);
        redeemCheck.setFont(FONT);
        redeemCheck.setForeground(TEXT);
        memberHintLabel = new JLabel("Member akan dicek saat checkout.");
        memberHintLabel.setForeground(DARK_RED);
        kitchenStatusLabel = new JLabel("Dapur: 0/3 pelanggan");
        kitchenStatusLabel.setForeground(TEXT);

        paymentCombo.addActionListener(e -> updatePaymentState());
        nameField.addActionListener(e -> updateMemberHint());

        addLabeled(panel, c, 0, "Nama pelanggan", nameField);
        addLabeled(panel, c, 1, "Mata uang", currencyCombo);
        addLabeled(panel, c, 2, "Pembayaran", paymentCombo);
        addLabeled(panel, c, 3, "Saldo channel", saldoField);

        c.gridx = 0;
        c.gridy = 8;
        c.gridwidth = 2;
        panel.add(redeemCheck, c);

        c.gridy = 9;
        panel.add(memberHintLabel, c);

        JButton checkoutButton = primaryButton("Cetak Kuitansi");
        checkoutButton.addActionListener(e -> checkout());
        c.gridy = 10;
        c.insets = new Insets(16, 4, 4, 4);
        panel.add(checkoutButton, c);

        JButton kitchenButton = secondaryButton("Proses Dapur");
        kitchenButton.addActionListener(e -> processPendingKitchen());
        c.gridy = 11;
        c.insets = new Insets(4, 4, 4, 4);
        panel.add(kitchenButton, c);

        c.gridy = 12;
        panel.add(kitchenStatusLabel, c);
        return panel;
    }

    private JPanel buildOutputPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setOpaque(false);

        receiptArea = outputArea();
        kitchenArea = outputArea();
        kitchenArea.setText("Output dapur akan muncul setelah 3 pelanggan atau saat tombol Proses Dapur ditekan.");

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(SECTION_FONT);
        styleTabbedPane(tabs);
        tabs.addTab("Kuitansi", new JScrollPane(receiptArea));
        tabs.addTab("Dapur", new JScrollPane(kitchenArea));
        panel.add(tabs, BorderLayout.CENTER);
        return panel;
    }

    private JTable createMenuTable(List<MenuItem> items) {
        DefaultTableModel model = new DefaultTableModel(new Object[]{"Kode", "Nama", "Harga"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (MenuItem item : items) {
            model.addRow(new Object[]{item.getKode(), item.getNama(), formatIDR(item.getHarga())});
        }
        JTable table = styledTable(model);
        table.setAutoCreateRowSorter(true);
        table.getSelectionModel().addListSelectionListener(e -> updateAddQtyLimit());
        return table;
    }

    private JTable styledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setFont(FONT);
        table.setForeground(TEXT);
        table.setBackground(SURFACE);
        table.setRowHeight(32);
        table.setGridColor(new Color(244, 223, 165));
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);
        table.setSelectionBackground(new Color(255, 224, 120));
        table.setSelectionForeground(TEXT);
        table.getTableHeader().setFont(SECTION_FONT);
        table.getTableHeader().setBackground(SURFACE_ALT);
        table.getTableHeader().setForeground(DARK_RED);
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component cell = super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                cell.setForeground(TEXT);
                if (!isSelected) {
                    cell.setBackground(row % 2 == 0 ? SURFACE : new Color(255, 249, 235));
                }
                if (this instanceof JLabel) {
                    ((JLabel) this).setBorder(new EmptyBorder(0, 10, 0, 10));
                }
                return cell;
            }
        };
        renderer.setOpaque(true);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
        return table;
    }

    private JPanel sectionPanel(String title) {
        JPanel panel = new JPanel();
        panel.setBackground(SURFACE);
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                title
        );
        titledBorder.setTitleFont(SECTION_FONT);
        titledBorder.setTitleColor(DARK_RED);
        panel.setBorder(BorderFactory.createCompoundBorder(
                titledBorder,
                new EmptyBorder(12, 12, 12, 12)
        ));
        return panel;
    }

    private JTextArea outputArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font("Consolas", Font.PLAIN, 12));
        area.setBackground(SURFACE);
        area.setForeground(TEXT);
        area.setBorder(new EmptyBorder(10, 10, 10, 10));
        return area;
    }

    private JButton primaryButton(String text) {
        JButton button = new JButton(text);
        styleButton(button, YELLOW, DARK_RED);
        return button;
    }

    private JButton secondaryButton(String text) {
        JButton button = new JButton(text);
        styleButton(button, SURFACE_ALT, DARK_RED);
        return button;
    }

    private JButton dangerButton(String text) {
        JButton button = new JButton(text);
        styleButton(button, new Color(255, 225, 221), DARK_RED);
        return button;
    }

    private void styleButton(JButton button, Color background, Color foreground) {
        button.setFont(SECTION_FONT);
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(205, 142, 55), 1),
                new EmptyBorder(10, 16, 10, 16)
        ));
        button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }

    private void styleTextField(JTextField field) {
        field.setFont(FONT);
        field.setForeground(TEXT);
        field.setBackground(SURFACE);
        field.setCaretColor(DARK_RED);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                new EmptyBorder(8, 10, 8, 10)
        ));
    }

    private void styleComboBox(JComboBox<String> comboBox) {
        comboBox.setFont(FONT);
        comboBox.setForeground(TEXT);
        comboBox.setBackground(SURFACE);
        comboBox.setBorder(BorderFactory.createLineBorder(BORDER, 1));
    }

    private void styleTabbedPane(JTabbedPane tabs) {
        tabs.setFont(SECTION_FONT);
        tabs.setForeground(DARK_RED);
        tabs.setBackground(SOFT_YELLOW);
        tabs.setBorder(BorderFactory.createEmptyBorder());
    }

    private GridBagConstraints baseConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        return c;
    }

    private void addLabeled(JPanel panel, GridBagConstraints c, int row, String label, Component component) {
        JLabel jLabel = new JLabel(label);
        jLabel.setFont(FONT);
        jLabel.setForeground(TEXT);
        c.gridx = 0;
        c.gridy = row * 2;
        c.gridwidth = 2;
        panel.add(jLabel, c);
        c.gridy = row * 2 + 1;
        panel.add(component, c);
    }

    private void addSelectedMenuItem() {
        MenuItem item = getSelectedMenuItem();
        if (item == null) {
            showError("Pilih menu terlebih dahulu.");
            return;
        }
        if (currentOrder.sudahDipesan(item.getKode())) {
            showError("Item sudah ada di pesanan. Gunakan Ubah Qty untuk mengganti jumlah.");
            return;
        }
        if (item instanceof Drink && currentOrder.minumanPenuh()) {
            showError("Jenis minuman sudah mencapai batas maksimal 5.");
            return;
        }
        if (!(item instanceof Drink) && currentOrder.makananPenuh()) {
            showError("Jenis makanan sudah mencapai batas maksimal 5.");
            return;
        }
        int qty = (Integer) addQtySpinner.getValue();
        int max = item instanceof Drink ? 3 : 2;
        if (qty < 1 || qty > max) {
            showError("Qty " + item.getNama() + " harus 1-" + max + ".");
            return;
        }
        currentOrder.tambahItem(item, qty);
        refreshOrderTable();
    }

    private MenuItem getSelectedMenuItem() {
        JTable table = menuTabs.getSelectedIndex() == 0 ? makananTable : minumanTable;
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return null;
        int modelRow = table.convertRowIndexToModel(viewRow);
        String code = table.getModel().getValueAt(modelRow, 0).toString();
        List<MenuItem> source = menuTabs.getSelectedIndex() == 0 ? menuMakanan : menuMinuman;
        for (MenuItem item : source) {
            if (item.getKode().equalsIgnoreCase(code)) return item;
        }
        return null;
    }

    private void updateAddQtyLimit() {
        if (addQtySpinner == null) return;
        MenuItem item = getSelectedMenuItem();
        int max = item instanceof Drink ? 3 : 2;
        int current = Math.min((Integer) addQtySpinner.getValue(), max);
        addQtySpinner.setModel(new SpinnerNumberModel(current, 1, max, 1));
    }

    private void updateSelectedOrderQty() {
        OrderItem item = getSelectedOrderItem();
        if (item == null) {
            showError("Pilih item pesanan terlebih dahulu.");
            return;
        }
        int max = item.isMinuman() ? 3 : 2;
        int qty = (Integer) editQtySpinner.getValue();
        if (qty < 1 || qty > max) {
            showError("Qty maksimal " + max + " untuk item ini.");
            return;
        }
        item.setKuantitas(qty);
        refreshOrderTable();
    }

    private void updateEditQtyLimit() {
        if (editQtySpinner == null) return;
        OrderItem item = getSelectedOrderItem();
        int max = item != null && item.isMinuman() ? 3 : 2;
        int current = item == null ? 1 : Math.min(item.getKuantitas(), max);
        editQtySpinner.setModel(new SpinnerNumberModel(current, 1, max, 1));
    }

    private void removeSelectedOrderItem() {
        OrderItem item = getSelectedOrderItem();
        if (item == null) {
            showError("Pilih item pesanan terlebih dahulu.");
            return;
        }
        currentOrder.getDaftarMakanan().remove(item);
        currentOrder.getDaftarMinuman().remove(item);
        refreshOrderTable();
    }

    private OrderItem getSelectedOrderItem() {
        int viewRow = orderTable.getSelectedRow();
        if (viewRow < 0) return null;
        int modelRow = orderTable.convertRowIndexToModel(viewRow);
        String code = orderModel.getValueAt(modelRow, 0).toString();
        for (OrderItem item : allOrderItems()) {
            if (item.getItem().getKode().equalsIgnoreCase(code)) return item;
        }
        return null;
    }

    private void clearOrder() {
        currentOrder = new Order();
        refreshOrderTable();
    }

    private void refreshOrderTable() {
        if (orderModel == null) return;
        orderModel.setRowCount(0);
        for (OrderItem item : allOrderItems()) {
            orderModel.addRow(new Object[]{
                    item.getItem().getKode(),
                    item.getItem().getNama(),
                    item.isMinuman() ? "Minuman" : "Makanan",
                    formatIDR(item.getItem().getHarga()),
                    item.getKuantitas(),
                    formatIDR(item.getSubtotalSebelumPajak())
            });
        }
        totalLabel.setText(String.format("Subtotal Rp %.2f | Dengan pajak Rp %.2f",
                currentOrder.getTotalSebelumPajak(), currentOrder.getTotalSetelahPajak()));
    }

    private List<OrderItem> allOrderItems() {
        List<OrderItem> items = new ArrayList<>();
        List<OrderItem> foods = new ArrayList<>(currentOrder.getDaftarMakanan());
        List<OrderItem> drinks = new ArrayList<>(currentOrder.getDaftarMinuman());
        foods.sort(byHarga);
        drinks.sort(byHarga);
        items.addAll(foods);
        items.addAll(drinks);
        return items;
    }

    private void checkout() {
        if (currentOrder.isEmpty()) {
            showError("Pesanan masih kosong.");
            return;
        }
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showError("Nama pelanggan wajib diisi untuk membership.");
            return;
        }

        Member existing = MemberDatabase.findMemberByName(name);
        boolean existingMember = existing != null;
        Member member = existingMember
                ? existing
                : new Member(MemberDatabase.generateMemberCode(), name, 0);
        MembershipResult membership = new MembershipResult(member, existingMember);
        boolean taxExempt = membership.getsTaxExemption();
        currentOrder.setPajakDibebaskan(taxExempt);

        Currency currency = selectedCurrency();
        String paymentName = paymentCombo.getSelectedItem().toString();
        double pointDiscount = calculateRequestedPointDiscount(membership, currency);
        double totalAfterRedeem = currentOrder.getTotalSetelahPajak() - pointDiscount;

        double saldo = 0;
        if (!paymentName.equals("Tunai")) {
            saldo = parseSaldo();
            if (Double.isNaN(saldo)) return;
        }
        PaymentChannel payment = createPayment(paymentName, saldo);
        double totalFinalIDR = payment.hitungTotal(totalAfterRedeem);
        double totalFinalCurrency = currency.konversiDariIDR(totalFinalIDR);

        if (payment instanceof QRISPayment && !((QRISPayment) payment).saldoCukup(totalFinalCurrency)) {
            showError(String.format("Saldo QRIS tidak cukup. Dibutuhkan %s %.4f.",
                    currency.getSimbol(), totalFinalCurrency));
            return;
        }
        if (payment instanceof EMoneyPayment && !((EMoneyPayment) payment).saldoCukup(totalFinalCurrency)) {
            showError(String.format("Saldo eMoney tidak cukup. Dibutuhkan %s %.4f.",
                    currency.getSimbol(), totalFinalCurrency));
            return;
        }

        if (!existingMember) {
            MemberDatabase.addMember(member);
        }
        int poinSebelum = member.getPoints();
        if (pointDiscount > 0) {
            pointDiscount = member.usePoints(currentOrder.getTotalSetelahPajak());
            totalAfterRedeem = currentOrder.getTotalSetelahPajak() - pointDiscount;
            totalFinalIDR = payment.hitungTotal(totalAfterRedeem);
        }
        int poinDapat = calculatePoints(totalFinalIDR, member.getMemberCode());
        member.addPoints(poinDapat);
        int poinSesudah = member.getPoints();

        receiptArea.setText(buildReceipt(currentOrder, payment, currency, membership,
                poinSebelum, poinDapat, poinSesudah, pointDiscount, taxExempt));
        enqueueKitchenBatch();
        clearOrder();
        saldoField.setText("");
        nameField.setText("");
        updateMemberHint();
    }

    private double calculateRequestedPointDiscount(MembershipResult membership, Currency currency) {
        if (!redeemCheck.isSelected()) return 0;
        if (!membership.isExistingMember()) {
            showInfo("Member baru belum memiliki poin transaksi sebelumnya.");
            return 0;
        }
        if (!currency.getNamaMataUang().equals("IDR")) {
            showInfo("Poin hanya dapat digunakan untuk pembayaran IDR.");
            return 0;
        }
        return membership.getMember().calculatePointDiscount(currentOrder.getTotalSetelahPajak());
    }

    private PaymentChannel createPayment(String paymentName, double saldo) {
        if (paymentName.equals("QRIS")) return new QRISPayment(saldo);
        if (paymentName.equals("eMoney")) return new EMoneyPayment(saldo);
        return new CashPayment();
    }

    private Currency selectedCurrency() {
        String selected = currencyCombo.getSelectedItem().toString();
        switch (selected) {
            case "USD":
                return new USDCurrency();
            case "JPY":
                return new JPYCurrency();
            case "MYR":
                return new MYRCurrency();
            case "EUR":
                return new EURCurrency();
            default:
                return new IDRCurrency();
        }
    }

    private double parseSaldo() {
        try {
            double saldo = Double.parseDouble(saldoField.getText().trim());
            if (saldo < 0) {
                showError("Saldo tidak boleh negatif.");
                return Double.NaN;
            }
            return saldo;
        } catch (NumberFormatException e) {
            showError("Saldo harus berupa angka.");
            return Double.NaN;
        }
    }

    private void updatePaymentState() {
        boolean needsSaldo = paymentCombo != null && !paymentCombo.getSelectedItem().toString().equals("Tunai");
        if (saldoField != null) {
            saldoField.setEnabled(needsSaldo);
            saldoField.setBackground(needsSaldo ? SURFACE : new Color(238, 230, 204));
            saldoField.setForeground(TEXT);
            if (!needsSaldo) saldoField.setText("");
        }
    }

    private void updateMemberHint() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            memberHintLabel.setText("Member akan dicek saat checkout.");
            return;
        }
        Member member = MemberDatabase.findMemberByName(name);
        if (member == null) {
            memberHintLabel.setText("Member baru akan dibuat otomatis.");
        } else {
            memberHintLabel.setText(member.getMemberCode() + " | " + member.getPoints() + " poin");
        }
    }

    private int calculatePoints(double totalBelanja, String memberCode) {
        int points = (int) (totalBelanja / 10);
        if (memberCode.toUpperCase().contains("A")) points *= 2;
        return points;
    }

    private String buildReceipt(Order order, PaymentChannel payment, Currency currency,
                                MembershipResult membership, int poinSebelum, int poinDapat,
                                int poinSesudah, double pointDiscount, boolean taxExempt) {
        StringBuilder sb = new StringBuilder();
        String sep = "=".repeat(98);
        String line = "-".repeat(98);
        double totalSetelahPajak = order.getTotalSetelahPajak();
        double totalAfterRedeem = totalSetelahPajak - pointDiscount;
        double discount = payment.hitungDiskon(totalAfterRedeem);
        double totalFinal = payment.hitungTotal(totalAfterRedeem);

        sb.append(sep).append("\n");
        sb.append(center("KUITANSI KohiSop", 98)).append("\n");
        sb.append(sep).append("\n");
        appendReceiptSection(sb, "MAKANAN", order.getDaftarMakanan(), line);
        appendReceiptSection(sb, "MINUMAN", order.getDaftarMinuman(), line);
        sb.append(sep).append("\n");
        sb.append(String.format("%-56s %s %.4f%n", "Total sebelum pajak (" + currency.getNamaMataUang() + "):",
                currency.getSimbol(), currency.konversiDariIDR(order.getTotalSebelumPajak())));
        sb.append(String.format("%-48s %12.2f IDR%n", "Grand total setelah pajak:", totalSetelahPajak));
        if (taxExempt) {
            sb.append(String.format("%-48s %s%n", "Status pajak:", "BEBAS PAJAK member lama kode A"));
        }
        if (pointDiscount > 0) {
            sb.append(String.format("%-48s %12.2f IDR%n", "Potongan poin:", -pointDiscount));
            sb.append(String.format("%-48s %12.2f IDR%n", "Total setelah poin:", totalAfterRedeem));
        }
        sb.append(String.format("%-48s %12.2f IDR%n",
                "Diskon " + payment.getNamaChannel() + " (" + (int) (payment.getDiskon() * 100) + "%):", discount));
        if (payment.getBiayaAdmin() > 0) {
            sb.append(String.format("%-48s %12.2f IDR%n", "Biaya admin:", payment.getBiayaAdmin()));
        }
        sb.append(line).append("\n");
        sb.append(String.format("%-48s %s%n", "Member:", membership.getMember().getMemberName()));
        sb.append(String.format("%-48s %s%n", "Kode member:", membership.getMember().getMemberCode()));
        sb.append(String.format("%-48s %s%n", "Status member:", membership.isExistingMember() ? "Lama" : "Baru"));
        sb.append(String.format("%-48s %d poin%n", "Poin sebelum transaksi:", poinSebelum));
        String bonus = membership.getMember().getMemberCode().toUpperCase().contains("A") ? " (x2 kode A)" : "";
        sb.append(String.format("%-48s %d poin%s%n", "Poin didapat:", poinDapat, bonus));
        sb.append(String.format("%-48s %d poin%n", "Poin setelah transaksi:", poinSesudah));
        if (!currency.getNamaMataUang().equals("IDR") && poinSebelum > 0) {
            sb.append("[INFO] Poin tidak digunakan karena pembayaran bukan IDR.\n");
        }
        sb.append(line).append("\n");
        sb.append(String.format("%-48s %12.2f IDR%n", "Total tagihan IDR:", totalFinal));
        sb.append(String.format("%-56s %s %.4f%n", "Total akhir (" + currency.getNamaMataUang() + "):",
                currency.getSimbol(), currency.konversiDariIDR(totalFinal)));
        sb.append(sep).append("\n");
        sb.append(center("Terima kasih dan silakan datang kembali!", 98)).append("\n");
        sb.append(sep).append("\n");
        return sb.toString();
    }

    private void appendReceiptSection(StringBuilder sb, String title, List<OrderItem> source, String line) {
        if (source.isEmpty()) return;
        List<OrderItem> items = new ArrayList<>(source);
        items.sort(byHarga);
        sb.append("\n").append(title).append("\n");
        sb.append(String.format("%-6s %-36s %5s %9s %10s %13s %10s%n",
                "Kode", "Nama", "Qty", "Harga", "Subtotal", "Pajak", "Total"));
        sb.append(line).append("\n");
        for (OrderItem item : items) {
            sb.append(String.format("%-6s %-36s %5d %9.2f %10.2f %13.2f %10.2f%n",
                    item.getItem().getKode(),
                    item.getItem().getNama(),
                    item.getKuantitas(),
                    item.getItem().getHarga(),
                    item.getSubtotalSebelumPajak(),
                    item.getTotalPajak(),
                    item.getSubtotalSetelahPajak()));
        }
        sb.append(line).append("\n");
    }

    private String center(String text, int width) {
        int padding = Math.max(0, (width - text.length()) / 2);
        return " ".repeat(padding) + text;
    }

    private void enqueueKitchenBatch() {
        List<OrderItem> items = new ArrayList<>();
        items.addAll(currentOrder.getDaftarMakanan());
        items.addAll(currentOrder.getDaftarMinuman());
        kitchenBatch.add(items);
        customerCount++;
        if (customerCount == 3) {
            kitchenArea.setText(KitchenProcessor.formatKitchenOrders(kitchenBatch));
            kitchenBatch.clear();
            customerCount = 0;
        } else {
            kitchenArea.setText("Batch dapur menunggu " + customerCount + "/3 pelanggan.");
        }
        kitchenStatusLabel.setText("Dapur: " + customerCount + "/3 pelanggan");
    }

    private void processPendingKitchen() {
        if (kitchenBatch.isEmpty()) {
            kitchenArea.setText("Belum ada pesanan yang menunggu proses dapur.");
            return;
        }
        kitchenArea.setText(KitchenProcessor.formatKitchenOrders(kitchenBatch));
        kitchenBatch.clear();
        customerCount = 0;
        kitchenStatusLabel.setText("Dapur: 0/3 pelanggan");
    }

    private String formatIDR(double value) {
        return String.format("Rp %.0f", value);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "KohiSop", JOptionPane.ERROR_MESSAGE);
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "KohiSop", JOptionPane.INFORMATION_MESSAGE);
    }
}
