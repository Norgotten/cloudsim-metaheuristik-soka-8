import java.util.ArrayList;
import java.util.List;

/**
 * Spesifikasi satu VM. Daftar ini dipakai dua kali:
 * 1. untuk membuat VM sungguhan di CloudSim, dan
 * 2. oleh scheduler (OOA/ChOA/OLB) untuk menghitung estimasi waktu.
 *
 * Konfigurasi sama dengan CloudSim OLB kelompok 8:
 * 2 LARGE (2 core x 1.000 MIPS), 4 MEDIUM (1 x 1.000), 4 SMALL (1 x 500).
 */
public record VmSpec(int id, String type, double mipsPerPe, int pes,
                     long ramMb, double costPerSecond) {

    public static List<VmSpec> defaultVms() {
        List<VmSpec> list = new ArrayList<>();
        int id = 0;
        for (int i = 0; i < 2; i++) list.add(new VmSpec(id++, "LARGE", 1000, 2, 4096, 0.12));
        for (int i = 0; i < 4; i++) list.add(new VmSpec(id++, "MEDIUM", 1000, 1, 2048, 0.05));
        for (int i = 0; i < 4; i++) list.add(new VmSpec(id++, "SMALL", 500, 1, 1024, 0.02));
        return list;
    }
}
