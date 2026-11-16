import java.util.List;

/**
 * Fungsi fitness untuk OOA dan ChOA.
 *
 * Penjelasan sederhana:
 * Metaheuristik mencoba ribuan "rencana pembagian task". Setiap rencana dinilai
 * dengan pertanyaan: "kalau dibagi begini, semua task selesai dalam berapa detik?"
 * Jawabannya dihitung pakai rumus cepat (tanpa menjalankan CloudSim), sehingga
 * ribuan rencana bisa dinilai dalam hitungan detik. Rencana terbaik baru dijalankan
 * di CloudSim satu kali.
 *
 * Model hitungan cepat ini meniru CloudletSchedulerSpaceShared di CloudSim:
 * - setiap task memakai 1 core,
 * - VM dengan 2 core (LARGE) bisa mengerjakan 2 task bersamaan,
 * - task di satu VM dikerjakan berurutan sesuai urutan dataset.
 *
 * Encoding posisi agen:
 * posisi = array bilangan desimal sepanjang jumlah task, nilainya di [LB, UB].
 * VM untuk task ke-i = floor(posisi[i] - LB), dibatasi ke 0..m-1.
 * Rentang dibuat simetris di sekitar 0 (contoh 10 VM: [-5, 5]) supaya rumus
 * fase 2 OOA, yaitu (lb + r(ub - lb)) / t, bisa bergerak ke dua arah.
 */
public final class Fitness {

    public final double lb;
    public final double ub;

    private final long[] lengths;
    private final int vmCount;
    private final double[] mips;
    private final int[] pes;

    // buffer kerja supaya tidak alokasi memori di setiap evaluasi
    private final double[][] laneFree;
    private final int[] decoded;

    private long nfe = 0;

    public Fitness(long[] lengths, List<VmSpec> vms) {
        this.lengths = lengths;
        this.vmCount = vms.size();
        this.lb = -vmCount / 2.0;
        this.ub = vmCount / 2.0;
        this.mips = new double[vmCount];
        this.pes = new int[vmCount];
        this.laneFree = new double[vmCount][];
        for (int j = 0; j < vmCount; j++) {
            mips[j] = vms.get(j).mipsPerPe();
            pes[j] = vms.get(j).pes();
            laneFree[j] = new double[pes[j]];
        }
        this.decoded = new int[lengths.length];
    }

    public int dim() {
        return lengths.length;
    }

    public long nfe() {
        return nfe;
    }

    public int decodeOne(double x) {
        int v = (int) Math.floor(x - lb);
        if (v < 0) v = 0;
        if (v >= vmCount) v = vmCount - 1;
        return v;
    }

    public int[] decode(double[] position) {
        int[] out = new int[position.length];
        for (int i = 0; i < position.length; i++) out[i] = decodeOne(position[i]);
        return out;
    }

    /** Nilai fitness = estimasi makespan (detik). Makin kecil makin baik. */
    public double evaluate(double[] position) {
        nfe++;
        for (int i = 0; i < position.length; i++) decoded[i] = decodeOne(position[i]);
        return estimateMakespan(decoded);
    }

    /** Estimasi makespan dari assignment yang sudah berbentuk indeks VM. */
    public double estimateMakespan(int[] assignment) {
        for (int j = 0; j < vmCount; j++) {
            java.util.Arrays.fill(laneFree[j], 0.0);
        }
        for (int i = 0; i < assignment.length; i++) {
            int vm = assignment[i];
            double[] lanes = laneFree[vm];
            // ambil core yang paling cepat kosong
            int k = 0;
            for (int p = 1; p < lanes.length; p++) {
                if (lanes[p] < lanes[k]) k = p;
            }
            lanes[k] += lengths[i] / mips[vm];
        }
        double makespan = 0.0;
        for (int j = 0; j < vmCount; j++) {
            for (double t : laneFree[j]) {
                if (t > makespan) makespan = t;
            }
        }
        return makespan;
    }

    /**
     * Ubah assignment (indeks VM) menjadi posisi agen. Posisinya diambil acak di
     * dalam rentang VM tersebut, jadi hasil decode tetap sama persis dengan assignment.
     */
    public double[] encode(int[] assignment, java.util.Random rng) {
        double[] x = new double[assignment.length];
        for (int i = 0; i < x.length; i++) {
            x[i] = lb + assignment[i] + 0.001 + 0.998 * rng.nextDouble();
        }
        return x;
    }

    public double clamp(double x) {
        if (x < lb) return lb;
        if (x > ub) return ub;
        return x;
    }
}
