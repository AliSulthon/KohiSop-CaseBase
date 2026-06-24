package member;

public class MembershipResult {
    private final Member member;
    private final boolean existingMember;

    public MembershipResult(Member member, boolean existingMember) {
        this.member = member;
        this.existingMember = existingMember;
    }

    public Member getMember() {
        return member;
    }

    public boolean isExistingMember() {
        return existingMember;
    }

    public boolean hasCodeA() {
        return member != null && member.getMemberCode().toUpperCase().contains("A");
    }

    public boolean getsTaxExemption() {
        return existingMember && hasCodeA();
    }
}
