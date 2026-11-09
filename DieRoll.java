import java.util.*;

public class DieRoll {
    private int ndice; // Số lượng xúc xắc cần tung
    private int nsides; // Số mặt của mỗi viên xúc xắc
    private int bonus; // Số điểm cộng hoặc trừ sau khi tung xúc xắc
    private static Random rnd = new Random();


    public DieRoll(int ndice, int nsides, int bonus) {
        this.ndice = ndice; // gán giá trị của mỗi viên xúc xắc
        this.nsides = nsides; // Gán số mặt của mỗi viên
        this.bonus = bonus; // Gán điểm cộng/trừ
    }

    public RollResult makeRoll() {
        RollResult result = new RollResult(bonus);
		// Tạo vòng lặp qua từng viên xúc xắc
        for (int i = 0; i < ndice; i++) {
            int roll = rnd.nextInt(nsides) + 1; // Tạo một số ngẫu nhiên từ 1 đế nside
            result.addResult(roll); // Thêm kết quả lần tung này vào đối tượng result
        }
        return result; // Trả kết quả
    }


    public String toString() {
        String ans = ndice + "d" + nsides;
        if (bonus > 0) {
            ans += "+" + bonus; // Thêm dấu + nếu bonus dương
        } else if (bonus < 0) {
            ans += bonus; // Bonus âm thì hiển thị dấu trừ
        }
        return ans;
    }
}

