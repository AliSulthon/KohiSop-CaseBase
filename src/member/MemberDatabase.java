package member;

import java.util.ArrayList;
import java.util.Random;

public class MemberDatabase {
    private static final ArrayList<Member> memberDatabase = new ArrayList<>();
    private static final Random RANDOM = new Random();
    private static final char[] CHARS = "ABCDEF0123456789".toCharArray();

    // =========================================================
    // GENERATE MEMBER CODE
    // =========================================================

    /**
     * Generate kode member unik 6 karakter (A-F, 0-9).
     * Dipastikan tidak duplikat dengan yang sudah ada di database.
     */
    public static String generateMemberCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                sb.append(CHARS[RANDOM.nextInt(CHARS.length)]);
            }
            code = sb.toString();
        } while (findMember(code) != null);
        return code;
    }

    // =========================================================
    // CRUD
    // =========================================================

    /**
     * Tambah member baru ke database.
     */
    public static void addMember(Member member) {
        memberDatabase.add(member);
    }

    public static MembershipResult findOrCreateByName(String name) {
        Member existing = findMemberByName(name);
        if (existing != null) {
            return new MembershipResult(existing, true);
        }

        Member member = new Member(generateMemberCode(), name, 0);
        addMember(member);
        return new MembershipResult(member, false);
    }

    /**
     * Cari member berdasarkan nama (case-insensitive).
     * Return null jika tidak ditemukan.
     */
    public static Member findMemberByName(String name) {
        for (Member m : memberDatabase) {
            if (m.getMemberName().equalsIgnoreCase(name)) return m;
        }
        return null;
    }

    /**
     * Cari member berdasarkan kode member.
     * Return null jika tidak ditemukan.
     */
    public static Member findMember(String memberCode) {
        for (Member m : memberDatabase) {
            if (m.getMemberCode().equalsIgnoreCase(memberCode)) return m;
        }
        return null;
    }

    /**
     * Update data member (tidak dipakai langsung karena Member mutable,
     * tapi disediakan untuk keperluan future-proofing).
     */
    public static boolean updateMember(String memberCode, int newPoints) {
        Member m = findMember(memberCode);
        if (m == null) return false;
        // Points langsung diupdate via addPoints / usePoints di object Member
        return true;
    }

    public static ArrayList<Member> getAll() {
        return memberDatabase;
    }
}
