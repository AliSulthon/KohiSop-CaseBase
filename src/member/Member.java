package member;

public class Member {
    private String memberCode;
    private String memberName;
    private int points;

    public Member(String memberCode, String memberName, int points) {
        this.memberCode = memberCode;
        this.memberName = memberName;
        this.points = points;
    }

    public String getMemberCode() { return memberCode; }
    public String getMemberName() { return memberName; }
    public int getPoints() { return points; }

    public void addPoints(int amount) {
        if (amount > 0) this.points += amount;
    }

    /**
     * Gunakan sejumlah poin. Jika nilai poin (poin * 2) melebihi tagihan,
     * hanya poin yang diperlukan yang dipakai. Kembalikan nilai IDR yang dipotong.
     *
     * @param tagihanIDR total tagihan dalam IDR
     * @return potongan IDR dari poin yang digunakan
     */
    public double usePoints(double tagihanIDR) {
        if (points <= 0) return 0;
        double nilaiPoin = points * 2.0;
        if (nilaiPoin <= tagihanIDR) {
            // Pakai semua poin
            points = 0;
            return nilaiPoin;
        } else {
            // Hitung berapa poin yang diperlukan (ceil)
            int poinDipakai = (int) Math.ceil(tagihanIDR / 2.0);
            points -= poinDipakai;
            return poinDipakai * 2.0;
        }
    }

    @Override
    public String toString() {
        return String.format("Member[%s | %s | %d poin]", memberCode, memberName, points);
    }
}
