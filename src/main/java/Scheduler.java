import java.util.List;

/**
 * Semua algoritma penjadwalan (OOA, ChOA, OLB) mengikuti bentuk yang sama:
 * menerima daftar panjang task dan daftar VM, lalu mengembalikan
 * "task ke-i dikirim ke VM nomor berapa".
 */
public interface Scheduler {

    String name();

    Result schedule(long[] taskLengths, List<VmSpec> vms, long seed);

    /**
     * @param assignment        assignment[i] = id VM untuk task ke-i
     * @param estimatedMakespan makespan menurut hitungan cepat (bukan hasil CloudSim)
     * @param convergence       nilai fitness terbaik per iterasi (null untuk OLB)
     * @param nfe               jumlah evaluasi fitness (0 untuk OLB)
     */
    record Result(int[] assignment, double estimatedMakespan, double[] convergence, long nfe) {}
}
